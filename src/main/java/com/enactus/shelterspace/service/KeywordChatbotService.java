package com.enactus.shelterspace.service;

import com.enactus.shelterspace.dto.BookingDecisionRequest;
import com.enactus.shelterspace.dto.BookingResponse;
import com.enactus.shelterspace.dto.ChatbotMessageRequest;
import com.enactus.shelterspace.dto.ChatbotMessageResponse;
import com.enactus.shelterspace.exception.BookingConflictException;
import com.enactus.shelterspace.model.GuestProfile;
import com.enactus.shelterspace.model.Shelter;
import com.enactus.shelterspace.model.enums.BookingStatus;
import com.enactus.shelterspace.model.enums.ShelterStatus;
import com.enactus.shelterspace.repository.GuestProfileRepository;
import com.enactus.shelterspace.repository.ShelterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class KeywordChatbotService {

    private static final Duration SESSION_TTL = Duration.ofMinutes(5);
    private static final int PAGE_SIZE = 2;
    private static final int MAX_SEGMENT_LENGTH = 160;

    private final ShelterRepository shelterRepository;
    private final GuestProfileRepository guestProfileRepository;
    private final BookingService bookingService;

    private final Map<String, ChatSession> sessions = new ConcurrentHashMap<>();

    public ChatbotMessageResponse handleMessage(ChatbotMessageRequest request) {
        String clientSessionId = safeTrim(request.getClientSessionId());
        String alias = safeTrim(request.getAlias());
        String rawMessage = safeTrim(request.getMessage());
        String normalized = normalizeInput(rawMessage);

        ChatSession session = sessions.computeIfAbsent(clientSessionId, ignored -> new ChatSession());
        if (isExpired(session)) {
            session.resetFlow();
            session.guestId = null;
            session.alias = null;
        }
        session.lastActivity = Instant.now();

        if (alias != null) {
            session.alias = alias;
        }

        if ("HELP".equals(normalized)) {
            return respond(session, helpText(), List.of("BED", "STATUS", "CANCEL", "DIR"));
        }
        if ("STATUS".equals(normalized)) {
            return handleStatus(session);
        }
        if ("CANCEL".equals(normalized)) {
            return handleCancel(session);
        }
        if ("DIR".equals(normalized)) {
            return handleDirections(session);
        }

        if (session.alias == null || session.alias.isBlank()) {
            return respond(session,
                    "Enter the name you want staff to call you.",
                    List.of("Type your alias")
            );
        }

        if ("BED".equals(normalized)) {
            return beginShelterChoice(session);
        }

        return switch (session.state) {
            case CHOOSING -> handleChoice(session, normalized);
            case DURATION -> handleDuration(session, normalized);
            case CONFIRM -> handleConfirm(session, normalized);
            case WAITING, IDLE -> respond(session,
                    "I only understand keywords. Start with BED.",
                    List.of("BED", "STATUS", "CANCEL", "HELP"));
        };
    }

    private ChatbotMessageResponse beginShelterChoice(ChatSession session) {
        List<Shelter> available = getAvailableShelters();
        if (available.isEmpty()) {
            session.resetFlow();
            return respond(session,
                    "No shelters with open beds right now.",
                    List.of("STATUS", "HELP"));
        }

        session.offeredShelterIds = available.stream().map(Shelter::getId).toList();
        session.pageStart = 0;
        session.state = ChatState.CHOOSING;
        return respond(session, renderShelterPage(session), List.of("1-2", "MORE", "STATUS", "CANCEL"));
    }

    private ChatbotMessageResponse handleChoice(ChatSession session, String normalized) {
        if ("MORE".equals(normalized)) {
            int nextStart = session.pageStart + PAGE_SIZE;
            if (nextStart >= session.offeredShelterIds.size()) {
                return respond(session,
                        "No more shelters in the list.",
                        List.of("1-2", "BED", "STATUS", "HELP"));
            }
            session.pageStart = nextStart;
            return respond(session, renderShelterPage(session), List.of("1-2", "MORE", "STATUS", "CANCEL"));
        }

        Integer selection = parsePositiveInt(normalized);
        if (selection == null) {
            return respond(session,
                    "Pick a number from the list or send MORE.",
                    List.of("1-2", "MORE", "STATUS", "CANCEL"));
        }

        List<Shelter> page = getCurrentPageShelters(session);
        if (selection < 1 || selection > page.size()) {
            return respond(session,
                    "That number is out of range for this page.",
                    List.of("1-2", "MORE", "STATUS", "CANCEL"));
        }

        Shelter shelter = page.get(selection - 1);
        session.selectedShelterId = shelter.getId();
        session.state = ChatState.DURATION;
        return respond(session,
                shelter.getName() + ". How many nights? (1-14)",
                List.of("Number of nights"));
    }

    private ChatbotMessageResponse handleDuration(ChatSession session, String normalized) {
        Integer nights = parsePositiveInt(normalized);
        if (nights == null || nights < 1 || nights > 14) {
            return respond(session,
                    "Enter nights as a number from 1 to 14.",
                    List.of("1-14", "STATUS", "CANCEL"));
        }

        session.nights = nights;
        session.state = ChatState.CONFIRM;
        Shelter shelter = getSelectedShelter(session);
        return respond(session,
                "Request bed at " + shelter.getName() + ", " + nights + " night(s)? YES or NO.",
                List.of("YES", "NO", "STATUS", "CANCEL"));
    }

    private ChatbotMessageResponse handleConfirm(ChatSession session, String normalized) {
        if ("NO".equals(normalized)) {
            session.resetFlow();
            return respond(session,
                    "Okay, no request was sent.",
                    List.of("BED", "STATUS", "HELP"));
        }
        if (!"YES".equals(normalized)) {
            return respond(session,
                    "Please reply YES or NO.",
                    List.of("YES", "NO", "STATUS", "CANCEL"));
        }

        if (session.guestId == null) {
            GuestProfile guest = new GuestProfile();
            guest.setDisplayName(session.alias);
            guest = guestProfileRepository.save(guest);
            session.guestId = guest.getId();
        }

        Shelter shelter = getSelectedShelter(session);
        String intakeNotes = "Chatbot request for " + session.nights + " night(s).";
        BookingResponse booking = bookingService.createChatbotRequest(
                shelter.getId(),
                session.guestId,
                LocalDate.now(),
                "Chatbot",
                intakeNotes
        );

        session.lastBookingId = booking.id();
        session.state = ChatState.WAITING;
        session.selectedShelterId = null;
        session.nights = null;
        String code = "A" + booking.id();
        return respond(session,
                "Requested. Code " + code + ". We'll update you after staff review.",
                List.of("STATUS", "CANCEL", "DIR", "HELP"));
    }

    private ChatbotMessageResponse handleStatus(ChatSession session) {
        BookingResponse booking = getLatestBooking(session);
        if (booking == null) {
            return respond(session,
                    "No booking found yet for this chat session.",
                    List.of("BED", "HELP"));
        }

        session.lastBookingId = booking.id();
        String base = "Status: " + booking.status() + " at " + booking.shelter().name() + ". Code A" + booking.id() + ".";
        if (booking.status() == BookingStatus.ADMITTED || booking.status() == BookingStatus.CHECKED_IN) {
            return respond(session, base + " Reply DIR for directions.", List.of("DIR", "CANCEL", "HELP"));
        }
        return respond(session, base, List.of("BED", "CANCEL", "HELP"));
    }

    private ChatbotMessageResponse handleCancel(ChatSession session) {
        BookingResponse booking = getLatestBooking(session);
        if (booking == null) {
            return respond(session,
                    "No active booking to cancel.",
                    List.of("BED", "STATUS", "HELP"));
        }

        if (!(booking.status() == BookingStatus.REQUESTED || booking.status() == BookingStatus.WAITLISTED)) {
            return respond(session,
                    "Only REQUESTED or WAITLISTED bookings can be cancelled.",
                    List.of("STATUS", "DIR", "HELP"));
        }

        BookingDecisionRequest decision = new BookingDecisionRequest();
        decision.setStaffName("Chatbot");
        decision.setNotes("Cancelled by guest keyword.");
        try {
            BookingResponse cancelled = bookingService.cancel(booking.id(), decision);
            session.lastBookingId = cancelled.id();
            session.resetFlow();
            return respond(session,
                    "Cancelled booking A" + cancelled.id() + ".",
                    List.of("BED", "STATUS", "HELP"));
        } catch (BookingConflictException conflict) {
            return respond(session,
                    conflict.getMessage(),
                    List.of("STATUS", "HELP"));
        }
    }

    private ChatbotMessageResponse handleDirections(ChatSession session) {
        BookingResponse booking = getLatestBooking(session);
        if (booking == null) {
            return respond(session,
                    "No active booking found for directions.",
                    List.of("STATUS", "BED", "HELP"));
        }

        Shelter shelter = shelterRepository.findById(booking.shelter().id()).orElse(null);
        if (shelter == null) {
            return respond(session,
                    "Shelter details are unavailable right now.",
                    List.of("STATUS", "HELP"));
        }

        String text = shelter.getName() + ": " + shelter.getAddress() + ". "
                + (shelter.getPhoneNumber() == null ? "" : "Call " + shelter.getPhoneNumber() + ". ");
        return respond(session, text.trim(), List.of("STATUS", "CANCEL", "HELP"));
    }

    private BookingResponse getLatestBooking(ChatSession session) {
        if (session.lastBookingId != null) {
            try {
                return bookingService.getById(session.lastBookingId);
            } catch (Exception ignored) {
                // Fall through to guest lookup.
            }
        }
        if (session.guestId == null) {
            return null;
        }
        return bookingService.findLatestBookingForGuest(session.guestId).orElse(null);
    }

    private Shelter getSelectedShelter(ChatSession session) {
        if (session.selectedShelterId == null) {
            throw new IllegalStateException("No shelter selected");
        }
        return shelterRepository.findById(session.selectedShelterId)
                .orElseThrow(() -> new IllegalStateException("Shelter is no longer available"));
    }

    private List<Shelter> getCurrentPageShelters(ChatSession session) {
        List<Shelter> all = session.offeredShelterIds.stream()
                .map(id -> shelterRepository.findById(id).orElse(null))
                .filter(shelter -> shelter != null && shelter.getAvailableBeds() > 0)
                .toList();
        int start = Math.min(session.pageStart, all.size());
        int end = Math.min(start + PAGE_SIZE, all.size());
        return all.subList(start, end);
    }

    private List<Shelter> getAvailableShelters() {
        return shelterRepository.findAll(Sort.by(Sort.Order.asc("city"), Sort.Order.asc("name")))
                .stream()
                .filter(shelter -> shelter.getAvailableBeds() > 0)
                .filter(shelter -> shelter.getOperationalStatus() != ShelterStatus.TEMPORARILY_CLOSED)
                .sorted(Comparator.comparing(Shelter::getAvailableBeds).reversed().thenComparing(Shelter::getName))
                .toList();
    }

    private String renderShelterPage(ChatSession session) {
        List<Shelter> page = getCurrentPageShelters(session);
        StringBuilder builder = new StringBuilder("Shelters with space:");
        for (int i = 0; i < page.size(); i++) {
            Shelter shelter = page.get(i);
            builder.append('\n')
                    .append(i + 1)
                    .append(") ")
                    .append(shelter.getName())
                    .append(" (")
                    .append(shelter.getAvailableBeds())
                    .append(") - ")
                    .append(barrierLabel(shelter))
                    .append(", ")
                    .append(populationLabel(shelter));
        }
        if (session.pageStart + PAGE_SIZE < session.offeredShelterIds.size()) {
            builder.append("\nReply 1-").append(page.size()).append(", or MORE.");
        } else {
            builder.append("\nReply 1-").append(page.size()).append(".");
        }
        return builder.toString();
    }

    private String barrierLabel(Shelter shelter) {
        return shelter.getBarrierLevel().name() == null
                ? "unknown barrier"
                : shelter.getBarrierLevel().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private String populationLabel(Shelter shelter) {
        return switch (shelter.getPopulationType()) {
            case MEN_ONLY -> "men";
            case WOMEN_ONLY -> "women";
            case FAMILY_ONLY -> "family";
            case WOMEN_WITH_CHILDREN -> "women+kids";
            case YOUTH_ONLY -> "youth";
            default -> "coed";
        };
    }

    private String helpText() {
        return "Commands: BED, STATUS, CANCEL, MORE, DIR, HELP. Use numbers to pick options and YES/NO to confirm.";
    }

    private ChatbotMessageResponse respond(ChatSession session, String message, List<String> nextInputs) {
        String withNext = message + " Next: " + String.join(", ", nextInputs) + ".";
        return new ChatbotMessageResponse(
                splitIntoSegments(withNext),
                nextInputs,
                session.state.name(),
                session.lastBookingId
        );
    }

    private List<String> splitIntoSegments(String text) {
        List<String> segments = new ArrayList<>();
        String remaining = text.trim();
        while (remaining.length() > MAX_SEGMENT_LENGTH) {
            int splitAt = remaining.lastIndexOf(' ', MAX_SEGMENT_LENGTH);
            if (splitAt <= 0) {
                splitAt = MAX_SEGMENT_LENGTH;
            }
            segments.add(remaining.substring(0, splitAt).trim());
            remaining = remaining.substring(splitAt).trim();
        }
        if (!remaining.isEmpty()) {
            segments.add(remaining);
        }
        return segments;
    }

    private String safeTrim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeInput(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private Integer parsePositiveInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean isExpired(ChatSession session) {
        if (session.lastActivity == null) {
            return false;
        }
        return session.lastActivity.plus(SESSION_TTL).isBefore(Instant.now());
    }

    private enum ChatState {
        IDLE,
        CHOOSING,
        DURATION,
        CONFIRM,
        WAITING
    }

    private static class ChatSession {
        private ChatState state = ChatState.IDLE;
        private String alias;
        private Long guestId;
        private List<Long> offeredShelterIds = List.of();
        private int pageStart = 0;
        private Long selectedShelterId;
        private Integer nights;
        private Long lastBookingId;
        private Instant lastActivity;

        private void resetFlow() {
            this.state = ChatState.IDLE;
            this.offeredShelterIds = List.of();
            this.pageStart = 0;
            this.selectedShelterId = null;
            this.nights = null;
        }
    }
}

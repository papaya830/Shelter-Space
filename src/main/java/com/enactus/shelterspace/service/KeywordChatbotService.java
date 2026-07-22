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

    private final ShelterRepository shelterRepository;
    private final GuestProfileRepository guestProfileRepository;
    private final BookingService bookingService;
    private final OllamaChatService ollamaChatService;

    private final Map<String, ChatSession> sessions = new ConcurrentHashMap<>();

    public ChatbotMessageResponse handleMessage(ChatbotMessageRequest request) {
        String clientSessionId = safeTrim(request.getClientSessionId());
        String alias = safeTrim(request.getAlias());
        String rawMessage = safeTrim(request.getMessage());
        String normalized = resolveCommand(rawMessage);

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
                    "Before we start, what name should staff call you?",
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
            case WAITING, IDLE -> handleOpenQuestion(session, rawMessage);
        };
    }

    private ChatbotMessageResponse handleOpenQuestion(ChatSession session, String rawMessage) {
        List<Shelter> shelters = shelterRepository.findAll(Sort.by(Sort.Order.asc("city"), Sort.Order.asc("name")));
        String normalized = normalizeInput(rawMessage);
        if (normalized.contains("WHEELCHAIR") || normalized.contains("ACCESSIBLE")) {
            return respondWithMatchingShelters(session, shelters.stream()
                    .filter(Shelter::isWheelchairAccessible)
                    .filter(this::hasUsableBed)
                    .toList(), "wheelchair-accessible shelters with space");
        }
        if (normalized.contains("PET") || normalized.contains("DOG") || normalized.contains("CAT")) {
            return respondWithMatchingShelters(session, shelters.stream()
                    .filter(Shelter::isPetsAllowed)
                    .filter(this::hasUsableBed)
                    .toList(), "pet-friendly shelters with space");
        }
        if (normalized.contains("AVAILABLE") || normalized.contains("AVAILABILITY")
                || (normalized.contains("SHELTER") && normalized.contains("SPACE"))) {
            return respondWithMatchingShelters(session, shelters.stream()
                    .filter(this::hasUsableBed)
                    .sorted(Comparator.comparing(Shelter::getAvailableBeds).reversed())
                    .toList(), "shelters with space");
        }
        return ollamaChatService.answer(rawMessage, session.state.name(), shelters)
                .map(answer -> respond(session, answer, List.of("BED", "STATUS", "CANCEL", "HELP")))
                .orElseGet(() -> respond(session,
                        "I can help with shelter questions when the local assistant is running. Send BED to request a bed, or HELP for commands.",
                        List.of("BED", "STATUS", "CANCEL", "HELP")));
    }

    private ChatbotMessageResponse respondWithMatchingShelters(
            ChatSession session,
            List<Shelter> matches,
            String description
    ) {
        if (matches.isEmpty()) {
            return respond(session, "I cannot find any " + description + " right now.", List.of("BED", "HELP"));
        }
        String result = matches.stream()
                .map(shelter -> shelter.getName() + " (" + shelter.getAvailableBeds() + " bed(s))")
                .limit(5)
                .reduce((left, right) -> left + "; " + right)
                .orElse("");
        return respond(session, "Current " + description + ": " + result + ".", List.of("BED", "HELP"));
    }

    private boolean hasUsableBed(Shelter shelter) {
        return shelter.getAvailableBeds() > 0
                && shelter.getOperationalStatus() != ShelterStatus.TEMPORARILY_CLOSED;
    }

    private ChatbotMessageResponse beginShelterChoice(ChatSession session) {
        List<Shelter> available = getAvailableShelters();
        if (available.isEmpty()) {
            session.resetFlow();
            return respond(session,
                    "I cannot find an open bed right now. You can check back soon or send STATUS for your latest request.",
                    List.of("STATUS", "HELP"));
        }

        session.offeredShelterIds = available.stream().map(Shelter::getId).toList();
        session.pageStart = 0;
        session.state = ChatState.CHOOSING;
        return respond(session, renderShelterPage(session), buildChoiceNextInputs(session));
    }

    private ChatbotMessageResponse handleChoice(ChatSession session, String normalized) {
        if ("MORE".equals(normalized)) {
            int nextStart = session.pageStart + PAGE_SIZE;
            if (nextStart >= session.offeredShelterIds.size()) {
                List<Shelter> page = getCurrentPageShelters(session);
                return respond(session,
                        "You are already on the last page. Reply with " + renderChoiceNumbers(page.size()) + " to choose.",
                        buildChoiceNextInputs(session));
            }
            session.pageStart = nextStart;
            return respond(session, renderShelterPage(session), buildChoiceNextInputs(session));
        }

        List<Shelter> page = getCurrentPageShelters(session);
        Integer selection = parsePositiveInt(normalized);
        if (selection == null) {
            return respond(session,
                    "Please reply with " + renderChoiceNumbers(page.size()) + " from this list, or send MORE to see more shelters.",
                    buildChoiceNextInputs(session));
        }

        if (selection < 1 || selection > page.size()) {
            return respond(session,
                    "That number is not on this page. Please choose " + renderChoiceNumbers(page.size()) + ".",
                    buildChoiceNextInputs(session));
        }

        Shelter shelter = page.get(selection - 1);
        session.selectedShelterId = shelter.getId();
        session.state = ChatState.DURATION;
        return respond(session,
                "You chose " + shelter.getName() + ". How many nights do you need? (1-14)",
                List.of("Number of nights"));
    }

    private ChatbotMessageResponse handleDuration(ChatSession session, String normalized) {
        Integer nights = parsePositiveInt(normalized);
        if (nights == null || nights < 1 || nights > 14) {
            return respond(session,
                    "Please enter the number of nights as 1 to 14.",
                    List.of("1-14", "STATUS", "CANCEL"));
        }

        session.nights = nights;
        session.state = ChatState.CONFIRM;
        Shelter shelter = getSelectedShelter(session);
        return respond(session,
                "Confirm request: " + shelter.getName() + " for " + nights + " night(s). Reply YES to send or NO to cancel.",
                List.of("YES", "NO", "STATUS", "CANCEL"));
    }

    private ChatbotMessageResponse handleConfirm(ChatSession session, String normalized) {
        if ("NO".equals(normalized)) {
            session.resetFlow();
            return respond(session,
                    "No problem. I did not send the request.",
                    List.of("BED", "STATUS", "HELP"));
        }
        if (!"YES".equals(normalized)) {
            return respond(session,
                    "Please reply with YES or NO.",
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
                "Request sent. Your code is " + code + ". Staff will review it and you can check with STATUS anytime.",
                List.of("STATUS", "CANCEL", "DIR", "HELP"));
    }

    private ChatbotMessageResponse handleStatus(ChatSession session) {
        BookingResponse booking = getLatestBooking(session);
        if (booking == null) {
            return respond(session,
                    "I do not see a booking for this chat yet. Send BED to start a new request.",
                    List.of("BED", "HELP"));
        }

        session.lastBookingId = booking.id();
        String base = "Status: " + booking.status() + " at " + booking.shelter().name() + ". Code A" + booking.id() + ".";
        if (booking.status() == BookingStatus.ADMITTED || booking.status() == BookingStatus.CHECKED_IN) {
            return respond(session, base + " Send DIR for address and phone details.", List.of("DIR", "CANCEL", "HELP"));
        }
        return respond(session, base, List.of("BED", "CANCEL", "HELP"));
    }

    private ChatbotMessageResponse handleCancel(ChatSession session) {
        BookingResponse booking = getLatestBooking(session);
        if (booking == null) {
            return respond(session,
                    "There is no active REQUESTED or WAITLISTED booking to cancel.",
                    List.of("BED", "STATUS", "HELP"));
        }

        if (!(booking.status() == BookingStatus.REQUESTED || booking.status() == BookingStatus.WAITLISTED)) {
            return respond(session,
                    "You can only cancel bookings that are still REQUESTED or WAITLISTED.",
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
                    "Cancelled booking A" + cancelled.id() + ". If you still need a bed, send BED to start again.",
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
                    "I cannot share directions yet because there is no booking on this chat. Send STATUS or BED first.",
                    List.of("STATUS", "BED", "HELP"));
        }

        Shelter shelter = shelterRepository.findById(booking.shelter().id()).orElse(null);
        if (shelter == null) {
            return respond(session,
                    "I cannot load shelter details right now. Please try STATUS again in a moment.",
                    List.of("STATUS", "HELP"));
        }

        String text = "Directions for " + shelter.getName() + ": " + shelter.getAddress() + ". "
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
        StringBuilder builder = new StringBuilder("Shelters with space right now:");
        for (int i = 0; i < page.size(); i++) {
            Shelter shelter = page.get(i);
            builder.append('\n')
                    .append(i + 1)
                    .append(") ")
                    .append(shelter.getName());
            builder.append('\n')
                    .append("   Beds: ")
                    .append(shelter.getAvailableBeds())
                    .append(" open");
            builder.append('\n')
                    .append("   Access: ")
                    .append(barrierLabel(shelter));
            builder.append('\n')
                    .append("   Serves: ")
                    .append(populationLabel(shelter));
        }
        if (session.pageStart + PAGE_SIZE < session.offeredShelterIds.size()) {
            builder.append("\nReply with ").append(renderChoiceNumbers(page.size())).append(" to choose, or MORE for more shelters.");
        } else {
            builder.append("\nReply with ").append(renderChoiceNumbers(page.size())).append(" to choose.");
        }
        return builder.toString();
    }

    private List<String> buildChoiceNextInputs(ChatSession session) {
        List<Shelter> page = getCurrentPageShelters(session);
        List<String> inputs = new ArrayList<>();
        for (int i = 1; i <= page.size(); i++) {
            inputs.add(String.valueOf(i));
        }
        if (session.pageStart + PAGE_SIZE < session.offeredShelterIds.size()) {
            inputs.add("MORE");
        }
        inputs.add("STATUS");
        inputs.add("CANCEL");
        return inputs;
    }

    private String renderChoiceNumbers(int count) {
        if (count <= 1) {
            return "1";
        }
        if (count == 2) {
            return "1 or 2";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= count; i++) {
            if (i > 1) {
                builder.append(i == count ? ", or " : ", ");
            }
            builder.append(i);
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
        return "I can help you request a shelter bed. Commands: BED, STATUS, CANCEL, MORE, DIR, HELP. Use numbers to choose shelters and YES/NO to confirm.";
    }

    private ChatbotMessageResponse respond(ChatSession session, String message, List<String> nextInputs) {
        String withNext = message + " Next: " + String.join(", ", nextInputs) + ".";
        return new ChatbotMessageResponse(
                List.of(withNext),
                nextInputs,
                session.state.name(),
                session.lastBookingId
        );
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

    private String resolveCommand(String value) {
        String normalized = normalizeInput(value);
        if (List.of("BED", "STATUS", "CANCEL", "DIR", "HELP", "MORE", "YES", "NO").contains(normalized)
                || parsePositiveInt(normalized) != null) {
            return normalized;
        }

        String words = " " + normalized.replaceAll("[^A-Z0-9]+", " ").trim() + " ";
        if (words.matches(".*\\b(NEED|FIND|WANT|REQUEST|LOOKING)\\b.*\\b(BED|SHELTER|PLACE TO STAY)\\b.*")) {
            return "BED";
        }
        if (words.matches(".*\\b(BOOKING|REQUEST)\\b.*\\b(STATUS|UPDATE|PROGRESS)\\b.*")
                || words.contains(" CHECK MY STATUS ")) {
            return "STATUS";
        }
        if (words.matches(".*\\b(CANCEL|WITHDRAW)\\b.*\\b(BOOKING|REQUEST|BED)\\b.*")) {
            return "CANCEL";
        }
        if (words.matches(".*\\b(WHERE|DIRECTION|DIRECTIONS|ADDRESS)\\b.*\\b(SHELTER|GO|BOOKING)\\b.*")) {
            return "DIR";
        }
        return normalized;
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

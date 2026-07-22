package com.enactus.shelterspace.service;

import com.enactus.shelterspace.dto.BookingDecisionRequest;
import com.enactus.shelterspace.dto.BookingRequest;
import com.enactus.shelterspace.dto.BookingResponse;
import com.enactus.shelterspace.dto.PublicBookingRequest;
import com.enactus.shelterspace.exception.BookingConflictException;
import com.enactus.shelterspace.exception.ResourceNotFoundException;
import com.enactus.shelterspace.model.GuestProfile;
import com.enactus.shelterspace.model.Shelter;
import com.enactus.shelterspace.model.ShelterBooking;
import com.enactus.shelterspace.model.enums.BookingChannel;
import com.enactus.shelterspace.model.enums.BookingStatus;
import com.enactus.shelterspace.model.enums.ShelterStatus;
import com.enactus.shelterspace.repository.GuestProfileRepository;
import com.enactus.shelterspace.repository.ShelterBookingRepository;
import com.enactus.shelterspace.repository.ShelterRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final Set<BookingStatus> ACTIVE_GUEST_STATUSES = EnumSet.of(
            BookingStatus.REQUESTED,
            BookingStatus.WAITLISTED,
            BookingStatus.ADMITTED,
            BookingStatus.CHECKED_IN
    );

    private final ShelterBookingRepository shelterBookingRepository;
    private final ShelterRepository shelterRepository;
    private final GuestProfileRepository guestProfileRepository;

    @Transactional
    public List<BookingResponse> getAll() {
        return shelterBookingRepository.findAll(Sort.by(
                        Sort.Order.asc("requestedBedDate"),
                        Sort.Order.asc("id")))
                .stream()
                .map(BookingResponse::fromEntity)
                .toList();
    }

    @Transactional
    public BookingResponse getById(Long id) {
        return BookingResponse.fromEntity(getBookingEntity(id));
    }

    @Transactional
    public BookingResponse createRequest(BookingRequest request) {
        Shelter shelter = getShelterEntity(request.getShelterId());
        GuestProfile guest = guestProfileRepository.findById(request.getGuestId())
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found: " + request.getGuestId()));
        validateBookingPreconditions(shelter, guest);
        return createBooking(shelter, guest, request.getRequestedBedDate(), request.getRequestChannel(),
                request.getRequestedBy(), request.getIntakeNotes(), BookingStatus.REQUESTED);
    }

    @Transactional
    public BookingResponse createPublicRequest(PublicBookingRequest request) {
        Shelter shelter = getShelterEntity(request.getShelterId());

        if (shelter.getOperationalStatus() == ShelterStatus.TEMPORARILY_CLOSED) {
            throw new BookingConflictException("Cannot register at a temporarily closed shelter.");
        }
        if (!shelter.hasCapacity()) {
            throw new BookingConflictException(shelter.isSupportsWaitlist()
                    ? "Shelter is full. Join its waitlist instead."
                    : "Shelter is full and is not accepting a waitlist.");
        }

        GuestProfile guest = createPublicGuest(request);
        return createBooking(
                shelter,
                guest,
                request.getRequestedBedDate(),
                BookingChannel.APP,
                publicRequestedBy(request),
                request.getIntakeNotes(),
                BookingStatus.REQUESTED
        );
    }

    @Transactional
    public BookingResponse createPublicWaitlistRequest(PublicBookingRequest request) {
        Shelter shelter = getShelterEntity(request.getShelterId());
        if (shelter.getOperationalStatus() == ShelterStatus.TEMPORARILY_CLOSED) {
            throw new BookingConflictException("Cannot join the waitlist for a temporarily closed shelter.");
        }
        if (shelter.hasCapacity()) {
            throw new BookingConflictException("Shelter has beds available. Send a booking request instead.");
        }
        if (!shelter.isSupportsWaitlist()) {
            throw new BookingConflictException("Shelter is full and is not accepting a waitlist.");
        }

        GuestProfile guest = createPublicGuest(request);
        return createBooking(
                shelter,
                guest,
                request.getRequestedBedDate(),
                BookingChannel.APP,
                publicRequestedBy(request),
                request.getIntakeNotes(),
                BookingStatus.WAITLISTED
        );
    }

    private GuestProfile createPublicGuest(PublicBookingRequest request) {

        GuestProfile guest = new GuestProfile();
        guest.setDisplayName(trimToNull(request.getDisplayName()));
        guest.setLegalName(trimToNull(request.getLegalName()));
        guest.setBirthDate(request.getBirthDate());
        guest.setPhoneNumber(trimToNull(request.getPhoneNumber()));
        return guestProfileRepository.save(guest);
    }

    private String publicRequestedBy(PublicBookingRequest request) {
        return request.getRequestedBy() != null && !request.getRequestedBy().isBlank()
                ? request.getRequestedBy()
                : "Public Web";
    }

    @Transactional
    public BookingResponse createChatbotRequest(
            Long shelterId,
            Long guestId,
            LocalDate requestedBedDate,
            String requestedBy,
            String intakeNotes
    ) {
        Shelter shelter = getShelterEntity(shelterId);
        GuestProfile guest = guestProfileRepository.findById(guestId)
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found: " + guestId));
        validateBookingPreconditions(shelter, guest);
        return createBooking(
                shelter,
                guest,
                requestedBedDate,
                BookingChannel.SMS,
                requestedBy == null || requestedBy.isBlank() ? "Chatbot" : requestedBy,
                intakeNotes,
                BookingStatus.REQUESTED
        );
    }

    @Transactional
    public BookingResponse admit(Long bookingId, BookingDecisionRequest request) {
        ShelterBooking booking = getBookingEntity(bookingId);
        if (!(booking.getStatus() == BookingStatus.REQUESTED || booking.getStatus() == BookingStatus.WAITLISTED)) {
            throw new BookingConflictException("Only requested or waitlisted bookings can be admitted");
        }

        Shelter shelter = booking.getShelter();
        if (!shelter.hasCapacity()) {
            throw new BookingConflictException("Shelter has no beds available");
        }

        shelter.incrementOccupancy();
        shelterRepository.save(shelter);

        booking.setStatus(BookingStatus.ADMITTED);
        booking.setDecidedAt(LocalDateTime.now());
        booking.setDecidedBy(trimToNull(request.getStaffName()));
        booking.setDecisionNotes(trimToNull(request.getNotes()));
        return BookingResponse.fromEntity(shelterBookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse waitlist(Long bookingId, BookingDecisionRequest request) {
        ShelterBooking booking = getBookingEntity(bookingId);
        if (booking.getStatus() != BookingStatus.REQUESTED) {
            throw new BookingConflictException("Only requested bookings can be waitlisted");
        }
        booking.setStatus(BookingStatus.WAITLISTED);
        booking.setDecidedAt(LocalDateTime.now());
        booking.setDecidedBy(trimToNull(request.getStaffName()));
        booking.setDecisionNotes(trimToNull(request.getNotes()));
        return BookingResponse.fromEntity(shelterBookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse reject(Long bookingId, BookingDecisionRequest request) {
        ShelterBooking booking = getBookingEntity(bookingId);
        if (!(booking.getStatus() == BookingStatus.REQUESTED || booking.getStatus() == BookingStatus.WAITLISTED)) {
            throw new BookingConflictException("Only requested or waitlisted bookings can be rejected");
        }
        booking.setStatus(BookingStatus.REJECTED);
        booking.setDecidedAt(LocalDateTime.now());
        booking.setDecidedBy(trimToNull(request.getStaffName()));
        booking.setDecisionNotes(trimToNull(request.getNotes()));
        return BookingResponse.fromEntity(shelterBookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse checkIn(Long bookingId, BookingDecisionRequest request) {
        ShelterBooking booking = getBookingEntity(bookingId);
        if (booking.getStatus() != BookingStatus.ADMITTED) {
            throw new BookingConflictException("Only admitted bookings can be checked in");
        }
        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setCheckedInAt(LocalDateTime.now());
        booking.setCheckedInBy(trimToNull(request.getStaffName()));
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            booking.setIntakeNotes(trimToNull(request.getNotes()));
        }
        return BookingResponse.fromEntity(shelterBookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse checkOut(Long bookingId, BookingDecisionRequest request) {
        ShelterBooking booking = getBookingEntity(bookingId);
        if (!(booking.getStatus() == BookingStatus.ADMITTED || booking.getStatus() == BookingStatus.CHECKED_IN)) {
            throw new BookingConflictException("Only admitted or checked-in bookings can be checked out");
        }

        Shelter shelter = booking.getShelter();
        shelter.decrementOccupancy();
        shelterRepository.save(shelter);

        booking.setStatus(BookingStatus.CHECKED_OUT);
        booking.setCheckedOutAt(LocalDateTime.now());
        booking.setCheckedOutBy(trimToNull(request.getStaffName()));
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            booking.setDecisionNotes(trimToNull(request.getNotes()));
        }
        return BookingResponse.fromEntity(shelterBookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse cancel(Long bookingId, BookingDecisionRequest request) {
        ShelterBooking booking = getBookingEntity(bookingId);
        if (!(booking.getStatus() == BookingStatus.REQUESTED || booking.getStatus() == BookingStatus.WAITLISTED)) {
            throw new BookingConflictException("Only requested or waitlisted bookings can be cancelled");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setDecidedAt(LocalDateTime.now());
        booking.setDecidedBy(trimToNull(request.getStaffName()));
        booking.setDecisionNotes(trimToNull(request.getNotes()));
        return BookingResponse.fromEntity(shelterBookingRepository.save(booking));
    }

    @Transactional
    public Optional<BookingResponse> findLatestBookingForGuest(Long guestId) {
        return shelterBookingRepository.findTopByGuestIdOrderByRequestedAtDescIdDesc(guestId)
                .map(BookingResponse::fromEntity);
    }

    private ShelterBooking getBookingEntity(Long id) {
        return shelterBookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
    }

    private Shelter getShelterEntity(Long id) {
        return shelterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shelter not found: " + id));
    }

    private void validateBookingPreconditions(Shelter shelter, GuestProfile guest) {
        if (shelter.getOperationalStatus() == ShelterStatus.TEMPORARILY_CLOSED) {
            throw new BookingConflictException("Cannot create a booking for a closed shelter");
        }
        if (shelterBookingRepository.existsByGuestIdAndStatusIn(guest.getId(), ACTIVE_GUEST_STATUSES)) {
            throw new BookingConflictException("Guest already has an active booking lifecycle");
        }
    }

    private BookingResponse createBooking(
            Shelter shelter,
            GuestProfile guest,
            java.time.LocalDate requestedBedDate,
            com.enactus.shelterspace.model.enums.BookingChannel requestChannel,
            String requestedBy,
            String intakeNotes,
            BookingStatus initialStatus
    ) {
        validateBookingPreconditions(shelter, guest);

        ShelterBooking booking = new ShelterBooking();
        booking.setShelter(shelter);
        booking.setGuest(guest);
        booking.setRequestedBedDate(requestedBedDate);
        booking.setRequestChannel(requestChannel);
        booking.setRequestedBy(trimToNull(requestedBy));
        booking.setIntakeNotes(trimToNull(intakeNotes));
        booking.setStatus(initialStatus);
        return BookingResponse.fromEntity(shelterBookingRepository.save(booking));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

package com.enactus.shelterspace.service;

import com.enactus.shelterspace.dto.BookingDecisionRequest;
import com.enactus.shelterspace.dto.BookingRequest;
import com.enactus.shelterspace.dto.BookingResponse;
import com.enactus.shelterspace.exception.BookingConflictException;
import com.enactus.shelterspace.exception.ResourceNotFoundException;
import com.enactus.shelterspace.model.GuestProfile;
import com.enactus.shelterspace.model.Shelter;
import com.enactus.shelterspace.model.ShelterBooking;
import com.enactus.shelterspace.model.enums.BookingStatus;
import com.enactus.shelterspace.model.enums.ShelterStatus;
import com.enactus.shelterspace.repository.GuestProfileRepository;
import com.enactus.shelterspace.repository.ShelterBookingRepository;
import com.enactus.shelterspace.repository.ShelterRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
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
        Shelter shelter = shelterRepository.findById(request.getShelterId())
                .orElseThrow(() -> new ResourceNotFoundException("Shelter not found: " + request.getShelterId()));
        GuestProfile guest = guestProfileRepository.findById(request.getGuestId())
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found: " + request.getGuestId()));

        if (shelter.getOperationalStatus() == ShelterStatus.TEMPORARILY_CLOSED) {
            throw new BookingConflictException("Cannot create a booking for a closed shelter");
        }
        if (shelterBookingRepository.existsByGuestIdAndStatusIn(guest.getId(), ACTIVE_GUEST_STATUSES)) {
            throw new BookingConflictException("Guest already has an active booking lifecycle");
        }

        ShelterBooking booking = new ShelterBooking();
        booking.setShelter(shelter);
        booking.setGuest(guest);
        booking.setRequestedBedDate(request.getRequestedBedDate());
        booking.setRequestChannel(request.getRequestChannel());
        booking.setRequestedBy(trimToNull(request.getRequestedBy()));
        booking.setIntakeNotes(trimToNull(request.getIntakeNotes()));
        booking.setStatus(BookingStatus.REQUESTED);
        return BookingResponse.fromEntity(shelterBookingRepository.save(booking));
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

    private ShelterBooking getBookingEntity(Long id) {
        return shelterBookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

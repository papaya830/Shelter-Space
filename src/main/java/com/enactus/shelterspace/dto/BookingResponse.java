package com.enactus.shelterspace.dto;

import com.enactus.shelterspace.model.ShelterBooking;
import com.enactus.shelterspace.model.enums.BookingChannel;
import com.enactus.shelterspace.model.enums.BookingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        BookingStatus status,
        BookingChannel requestChannel,
        LocalDate requestedBedDate,
        LocalDateTime requestedAt,
        LocalDateTime decidedAt,
        LocalDateTime checkedInAt,
        LocalDateTime checkedOutAt,
        String requestedBy,
        String decidedBy,
        String checkedInBy,
        String checkedOutBy,
        String decisionNotes,
        String intakeNotes,
        BookingGuestSummary guest,
        BookingShelterSummary shelter,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static BookingResponse fromEntity(ShelterBooking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getStatus(),
                booking.getRequestChannel(),
                booking.getRequestedBedDate(),
                booking.getRequestedAt(),
                booking.getDecidedAt(),
                booking.getCheckedInAt(),
                booking.getCheckedOutAt(),
                booking.getRequestedBy(),
                booking.getDecidedBy(),
                booking.getCheckedInBy(),
                booking.getCheckedOutBy(),
                booking.getDecisionNotes(),
                booking.getIntakeNotes(),
                BookingGuestSummary.fromEntity(booking.getGuest()),
                BookingShelterSummary.fromEntity(booking.getShelter()),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }
}

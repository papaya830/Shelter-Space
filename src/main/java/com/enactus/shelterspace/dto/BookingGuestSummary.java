package com.enactus.shelterspace.dto;

import com.enactus.shelterspace.model.GuestProfile;

import java.time.LocalDate;

public record BookingGuestSummary(
        Long id,
        String displayName,
        LocalDate birthDate,
        String phoneNumber
) {

    public static BookingGuestSummary fromEntity(GuestProfile guestProfile) {
        return new BookingGuestSummary(
                guestProfile.getId(),
                guestProfile.getDisplayName(),
                guestProfile.getBirthDate(),
                guestProfile.getPhoneNumber()
        );
    }
}

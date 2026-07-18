package com.enactus.shelterspace.dto;

import com.enactus.shelterspace.model.TurnAwayLog;
import com.enactus.shelterspace.model.enums.TurnAwayReason;

import java.time.LocalDateTime;

public record TurnAwayLogResponse(
        Long id,
        TurnAwayReason reason,
        String notes,
        LocalDateTime occurredAt,
        String recordedBy,
        BookingGuestSummary guest,
        BookingShelterSummary shelter
) {

    public static TurnAwayLogResponse fromEntity(TurnAwayLog turnAwayLog) {
        return new TurnAwayLogResponse(
                turnAwayLog.getId(),
                turnAwayLog.getReason(),
                turnAwayLog.getNotes(),
                turnAwayLog.getOccurredAt(),
                turnAwayLog.getRecordedBy(),
                turnAwayLog.getGuest() != null ? BookingGuestSummary.fromEntity(turnAwayLog.getGuest()) : null,
                BookingShelterSummary.fromEntity(turnAwayLog.getShelter())
        );
    }
}

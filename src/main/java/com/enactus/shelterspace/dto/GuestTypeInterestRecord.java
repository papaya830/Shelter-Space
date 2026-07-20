package com.enactus.shelterspace.dto;

import com.enactus.shelterspace.model.GuestTypeInterest;
import com.enactus.shelterspace.model.enums.PopulationType;

import java.time.LocalDateTime;

public record GuestTypeInterestRecord(
        Long id,
        String deviceId,
        PopulationType populationType,
        LocalDateTime recordedAt
) {
    public static GuestTypeInterestRecord fromEntity(GuestTypeInterest e) {
        return new GuestTypeInterestRecord(e.getId(), e.getDeviceId(), e.getPopulationType(), e.getRecordedAt());
    }
}

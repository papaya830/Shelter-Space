package com.enactus.shelterspace.dto;

import com.enactus.shelterspace.model.enums.PopulationType;

public record GuestTypeDemandSummary(
        PopulationType populationType,
        long uniqueDevices
) {}

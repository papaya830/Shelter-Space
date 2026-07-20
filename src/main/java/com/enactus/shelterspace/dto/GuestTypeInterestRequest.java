package com.enactus.shelterspace.dto;

import com.enactus.shelterspace.model.enums.PopulationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GuestTypeInterestRequest {

    @NotBlank(message = "Device ID is required")
    private String deviceId;

    @NotNull(message = "Population type is required")
    private PopulationType populationType;
}

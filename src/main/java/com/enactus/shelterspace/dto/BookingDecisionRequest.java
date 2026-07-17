package com.enactus.shelterspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingDecisionRequest {

    @NotBlank(message = "Staff name is required")
    @Size(max = 120)
    private String staffName;

    @Size(max = 1000)
    private String notes;
}

package com.enactus.shelterspace.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PublicBookingRequest {

    @NotNull(message = "Shelter id is required")
    private Long shelterId;

    @NotBlank(message = "Display name or alias is required")
    @Size(max = 120)
    private String displayName;

    @Size(max = 120)
    private String legalName;

    private LocalDate birthDate;

    @Size(max = 50)
    private String phoneNumber;

    @NotNull(message = "Requested bed date is required")
    @FutureOrPresent(message = "Requested bed date cannot be in the past")
    private LocalDate requestedBedDate;

    @Size(max = 120)
    private String requestedBy;

    @Size(max = 1000)
    private String intakeNotes;
}

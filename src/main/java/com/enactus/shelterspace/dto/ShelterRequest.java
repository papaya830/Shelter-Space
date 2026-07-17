package com.enactus.shelterspace.dto;

import com.enactus.shelterspace.model.enums.BarrierLevel;
import com.enactus.shelterspace.model.enums.IntakeType;
import com.enactus.shelterspace.model.enums.PopulationType;
import com.enactus.shelterspace.model.enums.ShelterStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class ShelterRequest {

    @NotBlank(message = "Shelter name is required")
    @Size(max = 150)
    private String name;

    @Size(max = 150)
    private String organizationName;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @NotBlank(message = "Address is required")
    @Size(max = 255)
    private String address;

    private boolean confidentialAddress;

    @Size(max = 50)
    private String phoneNumber;

    @NotNull(message = "Operational status is required")
    private ShelterStatus operationalStatus;

    @NotNull(message = "Barrier level is required")
    private BarrierLevel barrierLevel;

    @NotNull(message = "Population type is required")
    private PopulationType populationType;

    @NotNull(message = "Intake type is required")
    private IntakeType intakeType;

    @NotNull(message = "Total capacity is required")
    @Min(value = 0, message = "Capacity cannot be negative")
    private Integer totalCapacity;

    private boolean open24Hours = true;
    private boolean callAheadRequired;
    private boolean petsAllowed;
    private boolean wheelchairAccessible;
    private boolean acceptsLargeItems;
    private boolean legalNameRequired;

    private LocalTime intakeStartTime;
    private LocalTime intakeCutoffTime;

    @Min(value = 0, message = "Max stay must be at least 0")
    private Integer maxStayDays;

    @Min(value = 0, message = "Minimum age must be at least 0")
    @Max(value = 130, message = "Minimum age must be realistic")
    private Integer minimumAge;

    @Min(value = 0, message = "Maximum age must be at least 0")
    @Max(value = 130, message = "Maximum age must be realistic")
    private Integer maximumAge;

    @Size(max = 1000)
    private String programs;

    @Size(max = 2000)
    private String rules;

    @Size(max = 2000)
    private String intakeInstructions;

    @Size(max = 2000)
    private String notes;

    @Size(max = 1000)
    private String perks;

    @AssertTrue(message = "Maximum age must be greater than or equal to minimum age")
    public boolean isAgeRangeValid() {
        return minimumAge == null || maximumAge == null || maximumAge >= minimumAge;
    }

    @AssertTrue(message = "Intake cutoff time must be after intake start time")
    public boolean isIntakeWindowValid() {
        return intakeStartTime == null || intakeCutoffTime == null || intakeCutoffTime.isAfter(intakeStartTime);
    }
}

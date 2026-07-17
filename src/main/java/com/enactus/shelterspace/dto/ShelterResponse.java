package com.enactus.shelterspace.dto;

import com.enactus.shelterspace.model.Shelter;
import com.enactus.shelterspace.model.enums.BarrierLevel;
import com.enactus.shelterspace.model.enums.IntakeType;
import com.enactus.shelterspace.model.enums.PopulationType;
import com.enactus.shelterspace.model.enums.ShelterStatus;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record ShelterResponse(
        Long id,
        String name,
        String organizationName,
        String city,
        String address,
        boolean confidentialAddress,
        String phoneNumber,
        ShelterStatus operationalStatus,
        BarrierLevel barrierLevel,
        PopulationType populationType,
        IntakeType intakeType,
        Integer totalCapacity,
        Integer currentOccupancy,
        int availableBeds,
        boolean open24Hours,
        boolean callAheadRequired,
        boolean petsAllowed,
        boolean wheelchairAccessible,
        boolean acceptsLargeItems,
        boolean legalNameRequired,
        LocalTime intakeStartTime,
        LocalTime intakeCutoffTime,
        Integer maxStayDays,
        Integer minimumAge,
        Integer maximumAge,
        String programs,
        String rules,
        String intakeInstructions,
        String notes,
        String perks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ShelterResponse fromEntity(Shelter shelter) {
        return new ShelterResponse(
                shelter.getId(),
                shelter.getName(),
                shelter.getOrganizationName(),
                shelter.getCity(),
                shelter.getAddress(),
                shelter.isConfidentialAddress(),
                shelter.getPhoneNumber(),
                shelter.getOperationalStatus(),
                shelter.getBarrierLevel(),
                shelter.getPopulationType(),
                shelter.getIntakeType(),
                shelter.getTotalCapacity(),
                shelter.getCurrentOccupancy(),
                shelter.getAvailableBeds(),
                shelter.isOpen24Hours(),
                shelter.isCallAheadRequired(),
                shelter.isPetsAllowed(),
                shelter.isWheelchairAccessible(),
                shelter.isAcceptsLargeItems(),
                shelter.isLegalNameRequired(),
                shelter.getIntakeStartTime(),
                shelter.getIntakeCutoffTime(),
                shelter.getMaxStayDays(),
                shelter.getMinimumAge(),
                shelter.getMaximumAge(),
                shelter.getPrograms(),
                shelter.getRules(),
                shelter.getIntakeInstructions(),
                shelter.getNotes(),
                shelter.getPerks(),
                shelter.getCreatedAt(),
                shelter.getUpdatedAt()
        );
    }
}

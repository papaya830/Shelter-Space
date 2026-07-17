package com.enactus.shelterspace.dto;

import com.enactus.shelterspace.model.Shelter;
import com.enactus.shelterspace.model.enums.BarrierLevel;
import com.enactus.shelterspace.model.enums.PopulationType;
import com.enactus.shelterspace.model.enums.ShelterStatus;

public record BookingShelterSummary(
        Long id,
        String name,
        String city,
        ShelterStatus operationalStatus,
        BarrierLevel barrierLevel,
        PopulationType populationType,
        Integer totalCapacity,
        Integer currentOccupancy,
        int availableBeds
) {

    public static BookingShelterSummary fromEntity(Shelter shelter) {
        return new BookingShelterSummary(
                shelter.getId(),
                shelter.getName(),
                shelter.getCity(),
                shelter.getOperationalStatus(),
                shelter.getBarrierLevel(),
                shelter.getPopulationType(),
                shelter.getTotalCapacity(),
                shelter.getCurrentOccupancy(),
                shelter.getAvailableBeds()
        );
    }
}

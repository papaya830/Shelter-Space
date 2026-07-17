package com.enactus.shelterspace.service;

import com.enactus.shelterspace.dto.ShelterRequest;
import com.enactus.shelterspace.exception.ResourceNotFoundException;
import com.enactus.shelterspace.model.Shelter;
import com.enactus.shelterspace.repository.ShelterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShelterService {

    private final ShelterRepository shelterRepository;

    public List<Shelter> getAll() {
        return shelterRepository.findAll();
    }

    public Shelter getById(Long id) {
        return shelterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shelter not found: " + id));
    }

    public Shelter create(ShelterRequest request) {
        Shelter shelter = new Shelter();
        applyRequest(shelter, request);
        shelter.setCurrentOccupancy(0);
        return shelterRepository.save(shelter);
    }

    public Shelter update(Long id, ShelterRequest request) {
        Shelter shelter = getById(id);
        if (request.getTotalCapacity() < shelter.getCurrentOccupancy()) {
            throw new IllegalArgumentException("Capacity cannot be lower than current occupancy");
        }
        applyRequest(shelter, request);
        return shelterRepository.save(shelter);
    }

    public void delete(Long id) {
        Shelter shelter = getById(id);
        shelterRepository.delete(shelter);
    }

    private void applyRequest(Shelter shelter, ShelterRequest request) {
        shelter.setName(request.getName());
        shelter.setOrganizationName(request.getOrganizationName());
        shelter.setCity(request.getCity());
        shelter.setAddress(request.getAddress());
        shelter.setConfidentialAddress(request.isConfidentialAddress());
        shelter.setPhoneNumber(request.getPhoneNumber());
        shelter.setOperationalStatus(request.getOperationalStatus());
        shelter.setBarrierLevel(request.getBarrierLevel());
        shelter.setPopulationType(request.getPopulationType());
        shelter.setIntakeType(request.getIntakeType());
        shelter.setTotalCapacity(request.getTotalCapacity());
        shelter.setOpen24Hours(request.isOpen24Hours());
        shelter.setCallAheadRequired(request.isCallAheadRequired());
        shelter.setPetsAllowed(request.isPetsAllowed());
        shelter.setWheelchairAccessible(request.isWheelchairAccessible());
        shelter.setAcceptsLargeItems(request.isAcceptsLargeItems());
        shelter.setLegalNameRequired(request.isLegalNameRequired());
        shelter.setIntakeStartTime(request.getIntakeStartTime());
        shelter.setIntakeCutoffTime(request.getIntakeCutoffTime());
        shelter.setMaxStayDays(request.getMaxStayDays());
        shelter.setMinimumAge(request.getMinimumAge());
        shelter.setMaximumAge(request.getMaximumAge());
        shelter.setPrograms(request.getPrograms());
        shelter.setRules(request.getRules());
        shelter.setIntakeInstructions(request.getIntakeInstructions());
        shelter.setNotes(request.getNotes());
        shelter.setPerks(request.getPerks());
    }
}

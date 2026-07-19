package com.enactus.shelterspace.service;

import com.enactus.shelterspace.dto.ShelterRequest;
import com.enactus.shelterspace.dto.ShelterResponse;
import com.enactus.shelterspace.exception.ResourceNotFoundException;
import com.enactus.shelterspace.model.Shelter;
import com.enactus.shelterspace.repository.ShelterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShelterService {

    private final ShelterRepository shelterRepository;

    public List<ShelterResponse> getAll() {
        return shelterRepository.findAll(Sort.by(Sort.Order.asc("city"), Sort.Order.asc("name")))
                .stream()
                .map(ShelterResponse::fromEntity)
                .toList();
    }

    public ShelterResponse getById(Long id) {
        return ShelterResponse.fromEntity(getShelterEntity(id));
    }

    public ShelterResponse create(ShelterRequest request) {
        Shelter shelter = new Shelter();
        applyRequest(shelter, request);
        shelter.setCurrentOccupancy(0);
        return ShelterResponse.fromEntity(shelterRepository.save(shelter));
    }

    public ShelterResponse update(Long id, ShelterRequest request) {
        Shelter shelter = getShelterEntity(id);
        if (request.getTotalCapacity() < shelter.getCurrentOccupancy()) {
            throw new IllegalArgumentException("Capacity cannot be lower than current occupancy");
        }
        applyRequest(shelter, request);
        return ShelterResponse.fromEntity(shelterRepository.save(shelter));
    }

    public void delete(Long id) {
        Shelter shelter = getShelterEntity(id);
        shelterRepository.delete(shelter);
    }

    public List<ShelterResponse> getNearby(double userLat, double userLng, double radiusKm) {
        return shelterRepository.findAll()
                .stream()
                .filter(s -> s.getLatitude() != null && s.getLongitude() != null)
                .map(s -> {
                    double dist = haversineKm(userLat, userLng, s.getLatitude(), s.getLongitude());
                    return new Object[]{ s, dist };
                })
                .filter(pair -> (double) pair[1] <= radiusKm)
                .sorted(Comparator.comparingDouble(pair -> (double) ((Object[]) pair)[1]))
                .map(pair -> ShelterResponse.fromEntityWithDistance((Shelter) pair[0], (double) pair[1]))
                .toList();
    }

    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private Shelter getShelterEntity(Long id) {
        return shelterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shelter not found: " + id));
    }

    private void applyRequest(Shelter shelter, ShelterRequest request) {
        shelter.setName(trimToNull(request.getName()));
        shelter.setOrganizationName(trimToNull(request.getOrganizationName()));
        shelter.setCity(trimToNull(request.getCity()));
        shelter.setAddress(trimToNull(request.getAddress()));
        shelter.setConfidentialAddress(request.isConfidentialAddress());
        shelter.setPhoneNumber(trimToNull(request.getPhoneNumber()));
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
        shelter.setPrograms(trimToNull(request.getPrograms()));
        shelter.setRules(trimToNull(request.getRules()));
        shelter.setIntakeInstructions(trimToNull(request.getIntakeInstructions()));
        shelter.setNotes(trimToNull(request.getNotes()));
        shelter.setPerks(trimToNull(request.getPerks()));
        shelter.setLatitude(request.getLatitude());
        shelter.setLongitude(request.getLongitude());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

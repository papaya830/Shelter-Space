package com.enactus.shelterspace.service;

import com.enactus.shelterspace.dto.TurnAwayLogRequest;
import com.enactus.shelterspace.dto.TurnAwayLogResponse;
import com.enactus.shelterspace.exception.ResourceNotFoundException;
import com.enactus.shelterspace.model.GuestProfile;
import com.enactus.shelterspace.model.Shelter;
import com.enactus.shelterspace.model.TurnAwayLog;
import com.enactus.shelterspace.repository.GuestProfileRepository;
import com.enactus.shelterspace.repository.ShelterRepository;
import com.enactus.shelterspace.repository.TurnAwayLogRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TurnAwayLogService {

    private final TurnAwayLogRepository turnAwayLogRepository;
    private final ShelterRepository shelterRepository;
    private final GuestProfileRepository guestProfileRepository;

    @Transactional
    public List<TurnAwayLogResponse> getAll(Long shelterId) {
        List<TurnAwayLog> logs = shelterId != null
                ? turnAwayLogRepository.findByShelterIdOrderByOccurredAtDescIdDesc(shelterId)
                : turnAwayLogRepository.findAll(Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id")));

        return logs.stream()
                .map(TurnAwayLogResponse::fromEntity)
                .toList();
    }

    @Transactional
    public TurnAwayLogResponse create(TurnAwayLogRequest request) {
        Shelter shelter = shelterRepository.findById(request.getShelterId())
                .orElseThrow(() -> new ResourceNotFoundException("Shelter not found: " + request.getShelterId()));
        GuestProfile guest = request.getGuestId() == null
                ? null
                : guestProfileRepository.findById(request.getGuestId())
                        .orElseThrow(() -> new ResourceNotFoundException("Guest not found: " + request.getGuestId()));

        TurnAwayLog turnAwayLog = new TurnAwayLog();
        turnAwayLog.setShelter(shelter);
        turnAwayLog.setGuest(guest);
        turnAwayLog.setReason(request.getReason());
        turnAwayLog.setNotes(trimToNull(request.getNotes()));
        turnAwayLog.setOccurredAt(request.getOccurredAt());
        turnAwayLog.setRecordedBy(trimToNull(request.getRecordedBy()));

        return TurnAwayLogResponse.fromEntity(turnAwayLogRepository.save(turnAwayLog));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

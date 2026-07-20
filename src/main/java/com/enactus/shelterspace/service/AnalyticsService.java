package com.enactus.shelterspace.service;

import com.enactus.shelterspace.dto.GuestTypeDemandSummary;
import com.enactus.shelterspace.dto.GuestTypeInterestRecord;
import com.enactus.shelterspace.model.GuestTypeInterest;
import com.enactus.shelterspace.model.enums.PopulationType;
import com.enactus.shelterspace.repository.GuestTypeInterestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final GuestTypeInterestRepository guestTypeInterestRepository;

    public void recordInterest(String deviceId, PopulationType populationType) {
        if (guestTypeInterestRepository.existsByDeviceIdAndPopulationType(deviceId, populationType)) {
            return;
        }
        try {
            GuestTypeInterest interest = new GuestTypeInterest();
            interest.setDeviceId(deviceId);
            interest.setPopulationType(populationType);
            guestTypeInterestRepository.save(interest);
        } catch (DataIntegrityViolationException ignored) {
            // Race condition — duplicate insert from concurrent requests. Silently ignore.
        }
    }

    public List<GuestTypeDemandSummary> getDemandSummary() {
        return guestTypeInterestRepository.countByPopulationType()
                .stream()
                .map(row -> new GuestTypeDemandSummary(
                        (PopulationType) row[0],
                        (long) row[1]
                ))
                .toList();
    }

    public List<GuestTypeInterestRecord> getAllRecords() {
        return guestTypeInterestRepository.findAll(Sort.by(Sort.Order.desc("recordedAt")))
                .stream()
                .map(GuestTypeInterestRecord::fromEntity)
                .toList();
    }

    public String exportCsv() {
        StringBuilder sb = new StringBuilder("id,device_id,population_type,recorded_at\n");
        guestTypeInterestRepository.findAll(Sort.by(Sort.Order.asc("recordedAt")))
                .forEach(r -> sb
                        .append(r.getId()).append(",")
                        .append(r.getDeviceId()).append(",")
                        .append(r.getPopulationType()).append(",")
                        .append(r.getRecordedAt()).append("\n")
                );
        return sb.toString();
    }
}

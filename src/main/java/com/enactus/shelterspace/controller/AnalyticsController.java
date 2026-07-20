package com.enactus.shelterspace.controller;

import com.enactus.shelterspace.dto.GuestTypeDemandSummary;
import com.enactus.shelterspace.dto.GuestTypeInterestRecord;
import com.enactus.shelterspace.dto.GuestTypeInterestRequest;
import com.enactus.shelterspace.service.AnalyticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @PostMapping("/interest")
    public ResponseEntity<Void> recordInterest(@Valid @RequestBody GuestTypeInterestRequest request) {
        analyticsService.recordInterest(request.getDeviceId(), request.getPopulationType());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/interest/summary")
    public List<GuestTypeDemandSummary> getDemandSummary() {
        return analyticsService.getDemandSummary();
    }

    @GetMapping("/interest/records")
    public List<GuestTypeInterestRecord> getAllRecords() {
        return analyticsService.getAllRecords();
    }

    @GetMapping("/interest/export.csv")
    public ResponseEntity<String> exportCsv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"guest-type-demand.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(analyticsService.exportCsv());
    }
}

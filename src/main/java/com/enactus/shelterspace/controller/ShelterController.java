package com.enactus.shelterspace.controller;

import com.enactus.shelterspace.dto.ShelterRequest;
import com.enactus.shelterspace.dto.ShelterResponse;
import com.enactus.shelterspace.service.ShelterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/shelters")
@RequiredArgsConstructor
public class ShelterController {

    private final ShelterService shelterService;

    private static final CacheControl SHELTER_CACHE = CacheControl
            .maxAge(60, TimeUnit.SECONDS)
            .staleWhileRevalidate(300, TimeUnit.SECONDS)
            .cachePublic();

    @GetMapping
    public ResponseEntity<List<ShelterResponse>> getAll() {
        return ResponseEntity.ok()
                .cacheControl(SHELTER_CACHE)
                .body(shelterService.getAll());
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<ShelterResponse>> getNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "50") double radius) {
        return ResponseEntity.ok()
                .cacheControl(SHELTER_CACHE)
                .body(shelterService.getNearby(lat, lng, radius));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShelterResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok()
                .cacheControl(SHELTER_CACHE)
                .body(shelterService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ShelterResponse> create(@Valid @RequestBody ShelterRequest request) {
        ShelterResponse createdShelter = shelterService.create(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdShelter.id())
                .toUri();
        return ResponseEntity.created(location).body(createdShelter);
    }

    @PutMapping("/{id}")
    public ShelterResponse update(@PathVariable Long id, @Valid @RequestBody ShelterRequest request) {
        return shelterService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        shelterService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

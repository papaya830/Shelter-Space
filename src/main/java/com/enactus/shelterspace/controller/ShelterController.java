package com.enactus.shelterspace.controller;

import com.enactus.shelterspace.dto.ShelterRequest;
import com.enactus.shelterspace.dto.ShelterResponse;
import com.enactus.shelterspace.service.ShelterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/shelters")
@RequiredArgsConstructor
public class ShelterController {

    private final ShelterService shelterService;

    @GetMapping
    public List<ShelterResponse> getAll() {
        return shelterService.getAll();
    }

    @GetMapping("/{id}")
    public ShelterResponse getById(@PathVariable Long id) {
        return shelterService.getById(id);
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

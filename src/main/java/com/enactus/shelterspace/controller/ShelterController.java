package com.enactus.shelterspace.controller;

import com.enactus.shelterspace.dto.ShelterRequest;
import com.enactus.shelterspace.model.Shelter;
import com.enactus.shelterspace.service.ShelterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shelters")
@RequiredArgsConstructor
public class ShelterController {

    private final ShelterService shelterService;

    @GetMapping
    public List<Shelter> getAll() {
        return shelterService.getAll();
    }

    @GetMapping("/{id}")
    public Shelter getById(@PathVariable Long id) {
        return shelterService.getById(id);
    }

    @PostMapping
    public ResponseEntity<Shelter> create(@Valid @RequestBody ShelterRequest request) {
        return ResponseEntity.status(201).body(shelterService.create(request));
    }

    @PutMapping("/{id}")
    public Shelter update(@PathVariable Long id, @Valid @RequestBody ShelterRequest request) {
        return shelterService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        shelterService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

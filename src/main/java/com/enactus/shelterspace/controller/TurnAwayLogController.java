package com.enactus.shelterspace.controller;

import com.enactus.shelterspace.dto.TurnAwayLogRequest;
import com.enactus.shelterspace.dto.TurnAwayLogResponse;
import com.enactus.shelterspace.service.TurnAwayLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/turn-away-logs")
@RequiredArgsConstructor
public class TurnAwayLogController {

    private final TurnAwayLogService turnAwayLogService;

    @GetMapping
    public List<TurnAwayLogResponse> getAll(@RequestParam(required = false) Long shelterId) {
        return turnAwayLogService.getAll(shelterId);
    }

    @PostMapping
    public ResponseEntity<TurnAwayLogResponse> create(@Valid @RequestBody TurnAwayLogRequest request) {
        TurnAwayLogResponse createdLog = turnAwayLogService.create(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdLog.id())
                .toUri();
        return ResponseEntity.created(location).body(createdLog);
    }
}

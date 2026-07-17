package com.enactus.shelterspace.controller;

import com.enactus.shelterspace.dto.BookingDecisionRequest;
import com.enactus.shelterspace.dto.BookingRequest;
import com.enactus.shelterspace.dto.BookingResponse;
import com.enactus.shelterspace.dto.PublicBookingRequest;
import com.enactus.shelterspace.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @GetMapping
    public List<BookingResponse> getAll() {
        return bookingService.getAll();
    }

    @GetMapping("/{id}")
    public BookingResponse getById(@PathVariable Long id) {
        return bookingService.getById(id);
    }

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest request) {
        BookingResponse createdBooking = bookingService.createRequest(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdBooking.id())
                .toUri();
        return ResponseEntity.created(location).body(createdBooking);
    }

    @PostMapping("/public")
    public ResponseEntity<BookingResponse> createPublic(@Valid @RequestBody PublicBookingRequest request) {
        BookingResponse createdBooking = bookingService.createPublicRequest(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/bookings/{id}")
                .buildAndExpand(createdBooking.id())
                .toUri();
        return ResponseEntity.created(location).body(createdBooking);
    }

    @PostMapping("/{id}/admit")
    public BookingResponse admit(@PathVariable Long id, @Valid @RequestBody BookingDecisionRequest request) {
        return bookingService.admit(id, request);
    }

    @PostMapping("/{id}/reject")
    public BookingResponse reject(@PathVariable Long id, @Valid @RequestBody BookingDecisionRequest request) {
        return bookingService.reject(id, request);
    }

    @PostMapping("/{id}/check-in")
    public BookingResponse checkIn(@PathVariable Long id, @Valid @RequestBody BookingDecisionRequest request) {
        return bookingService.checkIn(id, request);
    }

    @PostMapping("/{id}/check-out")
    public BookingResponse checkOut(@PathVariable Long id, @Valid @RequestBody BookingDecisionRequest request) {
        return bookingService.checkOut(id, request);
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable Long id, @Valid @RequestBody BookingDecisionRequest request) {
        return bookingService.cancel(id, request);
    }
}

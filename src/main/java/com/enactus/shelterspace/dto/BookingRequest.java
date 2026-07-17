package com.enactus.shelterspace.dto;

import com.enactus.shelterspace.model.enums.BookingChannel;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class BookingRequest {

    @NotNull(message = "Shelter id is required")
    private Long shelterId;

    @NotNull(message = "Guest id is required")
    private Long guestId;

    @NotNull(message = "Requested bed date is required")
    @FutureOrPresent(message = "Requested bed date cannot be in the past")
    private LocalDate requestedBedDate;

    @NotNull(message = "Request channel is required")
    private BookingChannel requestChannel;

    @Size(max = 120)
    private String requestedBy;

    @Size(max = 1000)
    private String intakeNotes;
}

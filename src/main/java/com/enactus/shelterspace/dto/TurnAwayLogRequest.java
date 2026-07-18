package com.enactus.shelterspace.dto;

import com.enactus.shelterspace.model.enums.TurnAwayReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TurnAwayLogRequest {

    @NotNull(message = "Shelter id is required")
    private Long shelterId;

    private Long guestId;

    @NotNull(message = "Turn-away reason is required")
    private TurnAwayReason reason;

    @Size(max = 1000)
    private String notes;

    private LocalDateTime occurredAt;

    @NotBlank(message = "Recorded by is required")
    @Size(max = 120)
    private String recordedBy;
}

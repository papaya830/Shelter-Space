package com.enactus.shelterspace.dto;

import java.util.List;

public record ChatbotMessageResponse(
        List<String> messages,
        List<String> nextInputs,
        String state,
        Long bookingId
) {
}

package com.enactus.shelterspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatbotMessageRequest {

    @NotBlank(message = "Client session id is required")
    @Size(max = 120)
    private String clientSessionId;

    @NotBlank(message = "Message is required")
    @Size(max = 280)
    private String message;

    @Size(max = 120)
    private String alias;
}

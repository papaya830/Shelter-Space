package com.enactus.shelterspace.controller;

import com.enactus.shelterspace.dto.ChatbotMessageRequest;
import com.enactus.shelterspace.dto.ChatbotMessageResponse;
import com.enactus.shelterspace.service.KeywordChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final KeywordChatbotService keywordChatbotService;

    @PostMapping("/messages")
    public ChatbotMessageResponse sendMessage(@Valid @RequestBody ChatbotMessageRequest request) {
        return keywordChatbotService.handleMessage(request);
    }
}

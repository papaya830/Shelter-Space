package com.enactus.shelterspace.service;

import com.enactus.shelterspace.model.Shelter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OllamaChatService {

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${app.chatbot.ollama.enabled:false}")
    private boolean enabled;

    @Value("${app.chatbot.ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${app.chatbot.ollama.model:llama3.2:3b}")
    private String model;

    @Value("${app.chatbot.ollama.timeout-seconds:20}")
    private int timeoutSeconds;

    public Optional<String> answer(String question, String state, List<Shelter> shelters) {
        if (!enabled) {
            return Optional.empty();
        }

        String prompt = buildPrompt(question, state, shelters);
        try {
            String requestBody = objectMapper.writeValueAsString(new OllamaRequest(model, prompt, false));
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(2_000);
            requestFactory.setReadTimeout(timeoutSeconds * 1_000);
            String responseBody = restClientBuilder.baseUrl(baseUrl).requestFactory(requestFactory).build()
                    .post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
            JsonNode response = objectMapper.readTree(responseBody);
            String answer = response.path("response").asText("").trim();
            return answer.isBlank() ? Optional.empty() : Optional.of(answer);
        } catch (Exception ignored) {
            // The keyword workflow must remain usable when the local model is stopped.
            return Optional.empty();
        }
    }

    private String buildPrompt(String question, String state, List<Shelter> shelters) {
        StringBuilder context = new StringBuilder();
        for (Shelter shelter : shelters) {
            context.append("- ").append(shelter.getName())
                    .append(" | city: ").append(shelter.getCity())
                    .append(" | available beds: ").append(shelter.getAvailableBeds())
                    .append(" | status: ").append(shelter.getOperationalStatus())
                    .append(" | serves: ").append(shelter.getPopulationType())
                    .append(" | barrier: ").append(shelter.getBarrierLevel())
                    .append(" | wheelchair accessible: ").append(shelter.isWheelchairAccessible())
                    .append(" | pets allowed: ").append(shelter.isPetsAllowed())
                    .append('\n');
        }

        return """
                You are the local Shelter Space assistant. Be concise, calm, and practical.
                Use only the shelter facts below; never invent availability, policies, addresses, or phone numbers.
                Never claim that a booking was created, changed, cancelled, or confirmed.
                Answer informational shelter questions directly from the supplied facts.
                The only supported commands are exactly: BED starts a bed request; STATUS checks the current request;
                CANCEL cancels an eligible current request; DIR shows address and phone only after a booking exists; HELP lists commands.
                Never add words or arguments to a command, never invent command syntax, and never tell the user to use a command
                for information already present in the shelter data. If the user wants an action, tell them the one exact supported command.
                For an emergency, advise calling local emergency services.
                Current chat state: %s

                Current shelter data:
                %s
                User message: %s
                """.formatted(state, context, question);
    }

    private record OllamaRequest(String model, String prompt, boolean stream) {
    }
}

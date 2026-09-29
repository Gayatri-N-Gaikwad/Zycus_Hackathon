package com.stockpulse.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class LLMGateway {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String provider;
    private final String apiKey;
    private final String model;

    public LLMGateway(RestClient.Builder restClientBuilder,
                      ObjectMapper objectMapper,
                      @Value("${llm.provider:}") String provider,
                      @Value("${llm.api-key:}") String apiKey,
                      @Value("${llm.model:}") String model,
                      @Value("${llm.base-url:}") String baseUrl,
                      @Value("${llm.timeout-ms:5000}") int timeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.objectMapper = objectMapper;
        this.provider = provider;
        this.apiKey = apiKey;
        this.model = model;
    }

    public String complete(String prompt) {
        if (provider.isBlank() || apiKey.isBlank() || model.isBlank()) {
            throw new IllegalStateException("LLM configuration is incomplete");
        }

        String response = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .body(Map.of(
                        "model", model,
                        "messages", List.of(Map.of("role", "user", "content", prompt)),
                        "response_format", Map.of("type", "json_object")
                ))
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (!content.isTextual()) {
                throw new IllegalStateException("LLM response did not contain message content");
            }
            return content.textValue();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to parse LLM response", exception);
        }
    }
}
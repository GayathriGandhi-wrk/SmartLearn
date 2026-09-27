package com.student.performance.service.impl;

import com.student.performance.config.AppProperties;
import com.student.performance.service.AiServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiServiceClientImpl implements AiServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AiServiceClientImpl.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public AiServiceClientImpl(AppProperties properties) {
        this.webClient = WebClient.builder()
                .baseUrl(properties.getAiService().getBaseUrl())
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Map<String, Object> predict(Map<String, Object> features) {
        return post("/api/predict", features);
    }

    @Override
    public Map<String, Object> explainShap(Map<String, Object> features) {
        return post("/api/explain/shap", features);
    }

    @Override
    public Map<String, Object> explainLime(Map<String, Object> features) {
        return post("/api/explain/lime", features);
    }

    @Override
    public Map<String, Object> recommend(Map<String, Object> context) {
        return post("/api/recommend", context);
    }

    @Override
    public Map<String, Object> generatePlan(Map<String, Object> context) {
        return post("/api/planner", context);
    }

    @Override
    public Map<String, Object> chat(String message, String provider, List<Map<String, String>> history) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        if (provider != null && !provider.isBlank()) {
            body.put("provider", provider);
        }
        if (history != null && !history.isEmpty()) {
            body.put("history", history);
        }
        return post("/api/chat", body);
    }

    @Override
    public Map<String, Object> generateQuestions(String topic, String subject, String resourceTitle,
                                                 String difficulty, int count, List<String> avoid) {
        Map<String, Object> body = new HashMap<>();
        body.put("topic", topic);
        body.put("subject", subject);
        body.put("resource_title", resourceTitle);
        body.put("difficulty", difficulty);
        body.put("count", count);
        body.put("avoid", avoid == null ? List.of() : avoid);
        return post("/api/generate-questions", body);
    }

    private Map<String, Object> post(String path, Map<String, Object> body) {
        try {
            return webClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body == null ? Collections.emptyMap() : body)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(30))
                    .block();
        } catch (Exception ex) {
            log.warn("AI service call failed for {}: {}", path, ex.getMessage());
            return fallback(path, body);
        }
    }

    private Map<String, Object> fallback(String path, Map<String, Object> body) {
        return Map.of(
                "available", false,
                "message", "AI service is currently unavailable. Please try again later.",
                "path", path);
    }
}

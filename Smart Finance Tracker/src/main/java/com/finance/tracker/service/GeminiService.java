
package com.finance.tracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.tracker.exception.ApiKeyMissingException;
import com.finance.tracker.exception.GeminiApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GeminiService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public String chat(String prompt, String context) {
        if (geminiApiKey == null || geminiApiKey.isBlank() || geminiApiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
            throw new ApiKeyMissingException("Gemini API key missing");
        }

        String fullPrompt = buildFinancePrompt(prompt, context);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", fullPrompt)))));

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key="
                    + geminiApiKey;

            String responseBody = this.webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,
                            response -> response.bodyToMono(String.class).flatMap(errorBody -> {
                                log.error("Gemini API Error Status: {}, Body: {}", response.statusCode(), errorBody);
                                return Mono.error(new GeminiApiException("Gemini API error: " + errorBody));
                            }))
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            return parseGeminiResponse(responseBody);

        } catch (GeminiApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini service error: {}", e.getMessage(), e);
            throw new GeminiApiException("Gemini API error: " + e.getMessage());
        }
    }

    public String vision(String prompt, byte[] imageBytes, String contentType) {
        if (geminiApiKey == null || geminiApiKey.isBlank() || geminiApiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
            throw new ApiKeyMissingException("Gemini API key missing");
        }

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt),
                                Map.of("inline_data", Map.of(
                                        "mime_type", contentType,
                                        "data", base64Image))))));

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key="
                    + geminiApiKey;

            String responseBody = this.webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,
                            response -> response.bodyToMono(String.class).flatMap(errorBody -> {
                                log.error("Gemini Vision Error Status: {}, Body: {}", response.statusCode(), errorBody);
                                return Mono.error(new GeminiApiException("Gemini API error: " + errorBody));
                            }))
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            return parseGeminiResponse(responseBody);

        } catch (GeminiApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini vision error: {}", e.getMessage(), e);
            throw new GeminiApiException("Gemini API error: " + e.getMessage());
        }
    }

    private String buildFinancePrompt(String userPrompt, String context) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a helpful personal finance assistant for an Indian user. Use ₹ for amounts.\n\n");
        if (context != null && !context.isBlank()) {
            sb.append("Context information:\n").append(context).append("\n\n");
        }
        sb.append("User question: ").append(userPrompt);
        return sb.toString();
    }

    private String parseGeminiResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode textNode = root.path("candidates").get(0)
                    .path("content").path("parts").get(0).path("text");

            if (textNode.isMissingNode()) {
                log.error("Invalid Gemini response structure: {}", responseBody);
                throw new GeminiApiException("Gemini API error");
            }
            return textNode.asText();
        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", e.getMessage());
            throw new GeminiApiException("Gemini API error");
        }
    }
}

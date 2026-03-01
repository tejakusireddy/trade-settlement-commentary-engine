package com.tsengine.commentary.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsengine.commentary.config.AnthropicProperties;
import com.tsengine.schema.BreachEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClaudeApiService {

    private static final String PROMPT_VERSION = "v1";
    private static final URI ANTHROPIC_URI = URI.create("https://api.anthropic.com/v1/messages");
    private static final BigDecimal INPUT_TOKEN_PRICE = new BigDecimal("0.000003");
    private static final BigDecimal OUTPUT_TOKEN_PRICE = new BigDecimal("0.000015");

    private final ObjectMapper objectMapper;
    private final AnthropicProperties anthropicProperties;
    private final HttpClient httpClient;

    public ClaudeApiService(
            ObjectMapper objectMapper,
            AnthropicProperties anthropicProperties,
            HttpClient httpClient
    ) {
        this.objectMapper = objectMapper;
        this.anthropicProperties = anthropicProperties;
        this.httpClient = httpClient;
    }

    @CircuitBreaker(name = "claude-api", fallbackMethod = "fallbackCommentary")
    public ClaudeResponse generateCommentary(BreachEvent event) {
        long startNanos = System.nanoTime();
        try {
            String prompt = buildPrompt(event);
            String requestBody = objectMapper.createObjectNode()
                    .put("model", anthropicProperties.getModel())
                    .put("max_tokens", 300)
                    .set("messages", objectMapper.createArrayNode()
                            .add(objectMapper.createObjectNode()
                                    .put("role", "user")
                                    .put("content", prompt)))
                    .toString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(ANTHROPIC_URI)
                    .timeout(Duration.ofSeconds(30))
                    .header("x-api-key", anthropicProperties.getApiKey())
                    .header("anthropic-version", "2023-06-01")
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new RuntimeException("Claude API error status=" + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("content").path(0).path("text").asText();
            int tokensInput = root.path("usage").path("input_tokens").asInt();
            int tokensOutput = root.path("usage").path("output_tokens").asInt();
            BigDecimal costUsd = calculateCost(tokensInput, tokensOutput);
            long latencyMs = (System.nanoTime() - startNanos) / 1_000_000L;

            return new ClaudeResponse(content, tokensInput, tokensOutput, costUsd, latencyMs);
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("Failed to call Claude API", ex);
        }
    }

    public ClaudeResponse fallbackCommentary(BreachEvent event) {
        throw new RuntimeException("Claude API circuit breaker fallback for breachId=" + event.getBreachId());
    }

    public BigDecimal calculateCost(int tokensInput, int tokensOutput) {
        BigDecimal inputCost = INPUT_TOKEN_PRICE.multiply(BigDecimal.valueOf(tokensInput));
        BigDecimal outputCost = OUTPUT_TOKEN_PRICE.multiply(BigDecimal.valueOf(tokensOutput));
        return inputCost.add(outputCost).setScale(6, RoundingMode.HALF_UP);
    }

    public String promptVersion() {
        return PROMPT_VERSION;
    }

    public String model() {
        return anthropicProperties.getModel();
    }

    private String buildPrompt(BreachEvent event) {
        return """
                You are a financial operations analyst. Generate a concise 2-3 sentence
                management commentary for the following trade settlement breach:

                Trade ID: %s
                Instrument: %s
                Counterparty: %s
                Breach Type: %s (%d business days overdue)
                Breach Reason: %s
                Trade Date: %s

                The commentary should be professional, factual, and suitable for
                senior management reporting. Do not include any disclaimers or
                introductory phrases like 'Here is the commentary'.
                """.formatted(
                event.getTradeId(),
                event.getInstrument(),
                event.getCounterparty(),
                event.getBreachType(),
                event.getDaysOverdue(),
                event.getBreachReason(),
                event.getTradeDate()
        );
    }

    public record ClaudeResponse(
            String content,
            int tokensInput,
            int tokensOutput,
            BigDecimal costUsd,
            long latencyMs
    ) {
    }
}

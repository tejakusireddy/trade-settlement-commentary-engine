package com.tsengine.gateway.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsengine.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DownstreamHttpClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownstreamHttpClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DownstreamHttpClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public ResponseEntity<String> forward(HttpServletRequest request, String targetUrl) {
        long startNs = System.nanoTime();
        String requestId = request.getHeader("X-Request-ID");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        try {
            String fullTarget = appendQuery(targetUrl, request.getQueryString());
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(fullTarget))
                    .timeout(TIMEOUT);

            copyHeaders(request, builder, requestId);
            applyMethodAndBody(request, builder);

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            long latencyMs = (System.nanoTime() - startNs) / 1_000_000L;
            LOGGER.info(
                    "Forwarded request method={} path={} targetUrl={} latencyMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    fullTarget,
                    latencyMs
            );
            return ResponseEntity.status(response.statusCode()).body(response.body());
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return badGateway("Downstream service call failed: " + ex.getMessage());
        }
    }

    public ResponseEntity<String> badGateway(String message) {
        try {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(objectMapper.writeValueAsString(ApiResponse.error(message)));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("{\"success\":false,\"message\":\"" + message + "\"}");
        }
    }

    private void copyHeaders(HttpServletRequest request, HttpRequest.Builder builder, String requestId) {
        Collections.list(request.getHeaderNames()).forEach(header -> {
            if (!"host".equalsIgnoreCase(header)) {
                Collections.list(request.getHeaders(header)).forEach(value -> builder.header(header, value));
            }
        });
        String clientIp = request.getRemoteAddr();
        builder.header("X-Forwarded-For", clientIp);
        builder.header("X-Request-ID", requestId);
    }

    private void applyMethodAndBody(HttpServletRequest request, HttpRequest.Builder builder) throws IOException {
        String method = request.getMethod();
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            byte[] body = request.getInputStream().readAllBytes();
            builder.method(method, HttpRequest.BodyPublishers.ofByteArray(body));
            return;
        }
        if ("DELETE".equals(method)) {
            builder.DELETE();
            return;
        }
        builder.GET();
    }

    private String appendQuery(String url, String query) {
        if (query == null || query.isBlank()) {
            return url;
        }
        return url + "?" + query;
    }
}

package com.tsengine.commentary.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsengine.commentary.application.ClaudeApiService;
import com.tsengine.commentary.config.AnthropicProperties;
import com.tsengine.schema.BreachEvent;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.junit.jupiter.api.Test;

class ClaudeApiServiceTest {

    @Test
    void testSuccessfulCommentaryGeneration() {
        String responseJson = """
                {
                  "content": [{"text":"Trade settlement delay due to missing assignment."}],
                  "usage": {"input_tokens": 100, "output_tokens": 50}
                }
                """;
        ClaudeApiService service = new ClaudeApiService(
                new ObjectMapper(),
                anthropicProps(),
                new StubHttpClient(responseJson, 200)
        );

        ClaudeApiService.ClaudeResponse result = service.generateCommentary(sampleBreachEvent());

        assertEquals("Trade settlement delay due to missing assignment.", result.content());
        assertEquals(100, result.tokensInput());
        assertEquals(50, result.tokensOutput());
    }

    @Test
    void testCostCalculation() {
        ClaudeApiService service = new ClaudeApiService(
                new ObjectMapper(),
                anthropicProps(),
                new StubHttpClient("{}", 200)
        );

        BigDecimal total = service.calculateCost(100, 50);

        assertEquals(new BigDecimal("0.001050"), total);
    }

    @Test
    void testLatencyRecorded() {
        String responseJson = """
                {
                  "content": [{"text":"Commentary"}],
                  "usage": {"input_tokens": 10, "output_tokens": 5}
                }
                """;
        ClaudeApiService service = new ClaudeApiService(
                new ObjectMapper(),
                anthropicProps(),
                new StubHttpClient(responseJson, 200)
        );

        ClaudeApiService.ClaudeResponse result = service.generateCommentary(sampleBreachEvent());

        assertTrue(result.latencyMs() >= 0);
        assertTrue(result.latencyMs() < 10_000);
    }

    private AnthropicProperties anthropicProps() {
        AnthropicProperties props = new AnthropicProperties();
        props.setApiKey("test-api-key");
        props.setModel("claude-sonnet-4-6");
        props.setDailyCostCapUsd(new BigDecimal("10.00"));
        return props;
    }

    private BreachEvent sampleBreachEvent() {
        return BreachEvent.newBuilder()
                .setBreachId(UUID.randomUUID().toString())
                .setTradeId(UUID.randomUUID().toString())
                .setInstrument("AAPL")
                .setCounterparty("CP-A")
                .setBreachType("T3")
                .setBreachReason("MISSING_ASSIGNMENT")
                .setDaysOverdue(3)
                .setTradeDate("2026-03-01")
                .setDetectedAt(System.currentTimeMillis())
                .build();
    }

    private static class StubHttpClient extends HttpClient {
        private final String responseBody;
        private final int statusCode;

        private StubHttpClient(String responseBody, int statusCode) {
            this.responseBody = responseBody;
            this.statusCode = statusCode;
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
                throws IOException {
            @SuppressWarnings("unchecked")
            T body = (T) responseBody;
            return new StubHttpResponse<>(body, statusCode, request.uri());
        }

        @Override
        public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler,
                HttpResponse.PushPromiseHandler<T> pushPromiseHandler
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            try {
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, new TrustManager[]{new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }}, new SecureRandom());
                return context;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public SSLParameters sslParameters() {
            return new SSLParameters();
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }

        @Override
        public Optional<java.util.concurrent.Executor> executor() {
            return Optional.empty();
        }
    }

    private record StubHttpResponse<T>(T body, int statusCode, URI uri) implements HttpResponse<T> {
        @Override
        public int statusCode() {
            return statusCode;
        }

        @Override
        public HttpRequest request() {
            return HttpRequest.newBuilder(uri).build();
        }

        @Override
        public Optional<HttpResponse<T>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(java.util.Map.of(), (a, b) -> true);
        }

        @Override
        public T body() {
            return body;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return uri;
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}

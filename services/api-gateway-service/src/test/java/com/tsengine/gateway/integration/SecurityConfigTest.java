package com.tsengine.gateway.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    private static HttpServer downstreamServer;
    private static final AtomicReference<String> lastAuthorizationHeader = new AtomicReference<>();
    private static final AtomicReference<String> lastRequestIdHeader = new AtomicReference<>();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RedisOperations<String, String> redisTemplate;

    @SuppressWarnings("unchecked")
    private final ZSetOperations<String, String> zSetOperations = Mockito.mock(ZSetOperations.class);

    @BeforeAll
    static void startServer() throws Exception {
        downstreamServer = HttpServer.create(new InetSocketAddress(19082), 0);
        downstreamServer.createContext("/api/v1/trades", exchange -> {
            lastAuthorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            lastRequestIdHeader.set(exchange.getRequestHeaders().getFirst("X-Request-ID"));
            byte[] body = "{\"ok\":true}".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        downstreamServer.start();
    }

    @AfterAll
    static void stopServer() {
        if (downstreamServer != null) {
            downstreamServer.stop(0);
        }
    }

    @DynamicPropertySource
    static void dynamicProps(DynamicPropertyRegistry registry) {
        registry.add("services.trade-ingest-url", () -> "http://localhost:19082");
        registry.add("services.breach-detector-url", () -> "http://localhost:19082");
        registry.add("services.commentary-url", () -> "http://localhost:19082");
    }

    @BeforeEach
    void setup() {
        org.mockito.Mockito.when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        org.mockito.Mockito.when(zSetOperations.zCard(org.mockito.ArgumentMatchers.any())).thenReturn(1L);
        org.mockito.Mockito.when(redisTemplate.expire(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(Duration.class))).thenReturn(true);
        lastAuthorizationHeader.set(null);
        lastRequestIdHeader.set(null);
    }

    @Test
    void testUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/trades"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void testUnauthorizedRole() throws Exception {
        mockMvc.perform(get("/api/v1/trades")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_guest"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Forbidden"));
    }

    @Test
    void testAuthorizedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/trades")
                        .header("Authorization", "Bearer forwarded-token")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ops-user"))))
                .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals("Bearer forwarded-token", lastAuthorizationHeader.get());
        org.junit.jupiter.api.Assertions.assertNotNull(lastRequestIdHeader.get());
    }

    @Test
    void testHealthEndpointPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}

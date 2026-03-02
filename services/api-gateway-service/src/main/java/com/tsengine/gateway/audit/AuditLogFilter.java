package com.tsengine.gateway.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuditLogFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogFilter.class);

    private final ObjectMapper objectMapper;
    @Qualifier("gatewayRequestCounter")
    private final Counter gatewayRequestCounter;
    @Qualifier("gatewayLatencyTimer")
    private final Timer gatewayLatencyTimer;

    public AuditLogFilter(
            ObjectMapper objectMapper,
            @Qualifier("gatewayRequestCounter") Counter gatewayRequestCounter,
            @Qualifier("gatewayLatencyTimer") Timer gatewayLatencyTimer
    ) {
        this.objectMapper = objectMapper;
        this.gatewayRequestCounter = gatewayRequestCounter;
        this.gatewayLatencyTimer = gatewayLatencyTimer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startNs = System.nanoTime();
        String userId = extractUserId();
        String requestId = request.getHeader("X-Request-ID");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put("userId", userId);
        MDC.put("requestId", requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            try {
                long latencyMs = (System.nanoTime() - startNs) / 1_000_000L;
                gatewayRequestCounter.increment();
                gatewayLatencyTimer.record(latencyMs, java.util.concurrent.TimeUnit.MILLISECONDS);
                Map<String, Object> audit = new LinkedHashMap<>();
                audit.put("timestamp", Instant.now().toString());
                audit.put("userId", userId);
                audit.put("method", request.getMethod());
                audit.put("path", request.getRequestURI());
                audit.put("status", response.getStatus());
                audit.put("latencyMs", latencyMs);
                LOGGER.info(objectMapper.writeValueAsString(audit));
            } catch (Exception ex) {
                // Never fail request lifecycle because of audit logging/metrics errors.
                LOGGER.error("Audit logging failed method={} path={} reason={}",
                        request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
            }
            MDC.clear();
        }
    }

    private String extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return "anonymous";
    }
}

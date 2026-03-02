package com.tsengine.gateway.security;

import io.micrometer.core.instrument.Counter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RedisOperations<String, String> redisTemplate;
    private final Environment environment;
    @Qualifier("gatewayRateLimitCounter")
    private final Counter gatewayRateLimitCounter;

    public RateLimitFilter(
            RedisOperations<String, String> redisTemplate,
            Environment environment,
            @Qualifier("gatewayRateLimitCounter") Counter gatewayRateLimitCounter
    ) {
        this.redisTemplate = redisTemplate;
        this.environment = environment;
        this.gatewayRateLimitCounter = gatewayRateLimitCounter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String userId = extractUserId();
        String endpointPrefix = endpointPrefix(request.getRequestURI());
        String key = "rate_limit:" + userId + ":" + endpointPrefix;

        long now = System.currentTimeMillis();
        long windowSeconds = readLongProperty("rate-limit.window-seconds", 60L);
        long windowMs = windowSeconds * 1000L;
        long maxPerWindow = readLongProperty("rate-limit.requests-per-minute", 100L);

        Long count = null;
        try {
            ZSetOperations<String, String> zSet = redisTemplate.opsForZSet();
            if (zSet == null) {
                LOGGER.error("Rate limiter unavailable: Redis ZSet operations bean is null; failing open endpoint={}",
                        endpointPrefix);
                filterChain.doFilter(request, response);
                return;
            }
            // Ensure uniqueness under high concurrency; duplicated members under the same millisecond
            // make counts appear lower than real traffic.
            String scoreMember = now + "-" + UUID.randomUUID();
            zSet.add(key, scoreMember, now); // ZADD
            zSet.removeRangeByScore(key, 0, now - windowMs); // ZREMRANGEBYSCORE
            count = zSet.zCard(key); // ZCARD
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds)); // EXPIRE
        } catch (RuntimeException ex) {
            LOGGER.error(
                    "Rate limiter failed open userId={} endpoint={} reason={}",
                    userId,
                    endpointPrefix,
                    ex.getMessage()
            );
            filterChain.doFilter(request, response);
            return;
        }

        if (count != null && count > maxPerWindow) {
            gatewayRateLimitCounter.increment();
            LOGGER.warn("Rate limit exceeded userId={} endpoint={} count={}", userId, endpointPrefix, count);
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(windowSeconds));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            try {
                response.getWriter().write("{\"success\":false,\"data\":null,\"message\":\"Rate limit exceeded\"}");
            } catch (IOException ioEx) {
                // Client may disconnect while server is writing the rate-limit payload.
                LOGGER.warn("Rate limit response write failed endpoint={} reason={}", endpointPrefix, ioEx.getMessage());
            }
            return;
        }

        filterChain.doFilter(request, response);
    }

    private long readLongProperty(String key, long defaultValue) {
        String raw = environment.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            LOGGER.warn("Invalid rate-limit config {}='{}'; using default={}", key, raw, defaultValue);
            return defaultValue;
        }
    }

    private String extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return Objects.toString(jwt.getSubject(), "anonymous");
        }
        return "anonymous";
    }

    String endpointPrefix(String path) {
        String[] parts = path.split("/");
        if (parts.length >= 4) {
            return "/" + parts[1] + "/" + parts[2] + "/" + parts[3];
        }
        return path;
    }
}

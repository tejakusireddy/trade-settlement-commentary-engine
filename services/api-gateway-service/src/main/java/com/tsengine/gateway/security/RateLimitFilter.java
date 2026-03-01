package com.tsengine.gateway.security;

import io.micrometer.core.instrument.Counter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
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
        long windowSeconds = Long.parseLong(environment.getProperty("rate-limit.window-seconds", "60"));
        long windowMs = windowSeconds * 1000L;
        long maxPerWindow = Long.parseLong(environment.getProperty("rate-limit.requests-per-minute", "100"));

        ZSetOperations<String, String> zSet = redisTemplate.opsForZSet();
        String scoreMember = String.valueOf(now);
        zSet.add(key, scoreMember, now); // ZADD
        zSet.removeRangeByScore(key, 0, now - windowMs); // ZREMRANGEBYSCORE
        Long count = zSet.zCard(key); // ZCARD
        redisTemplate.expire(key, Duration.ofSeconds(windowSeconds)); // EXPIRE

        if (count != null && count > maxPerWindow) {
            gatewayRateLimitCounter.increment();
            LOGGER.warn("Rate limit exceeded userId={} endpoint={} count={}", userId, endpointPrefix, count);
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(windowSeconds));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"success\":false,\"data\":null,\"message\":\"Rate limit exceeded\"}");
            return;
        }

        filterChain.doFilter(request, response);
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

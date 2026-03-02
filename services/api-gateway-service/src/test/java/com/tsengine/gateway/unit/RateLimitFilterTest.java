package com.tsengine.gateway.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tsengine.gateway.security.RateLimitFilter;
import io.micrometer.core.instrument.Counter;
import jakarta.servlet.FilterChain;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

class RateLimitFilterTest {

    @Test
    void testRequestUnderLimit() throws Exception {
        RateLimitFixture fixture = fixtureWithCount(99L);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fixture.filter.doFilter(fixture.request, response, fixture.chain);

        assertEquals(200, response.getStatus());
    }

    @Test
    void testRequestAtLimit() throws Exception {
        RateLimitFixture fixture = fixtureWithCount(100L);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fixture.filter.doFilter(fixture.request, response, fixture.chain);

        assertEquals(200, response.getStatus());
    }

    @Test
    void testRequestOverLimit() throws Exception {
        RateLimitFixture fixture = fixtureWithCount(101L);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fixture.filter.doFilter(fixture.request, response, fixture.chain);

        assertEquals(429, response.getStatus());
        assertEquals("60", response.getHeader("Retry-After"));
    }

    @Test
    void testRateLimitKeyFormat() throws Exception {
        @SuppressWarnings("unchecked")
        RedisOperations<String, String> redis = Mockito.mock(RedisOperations.class);
        Environment env = Mockito.mock(Environment.class);
        Counter counter = Mockito.mock(Counter.class);
        @SuppressWarnings("unchecked")
        ZSetOperations<String, String> zset = Mockito.mock(ZSetOperations.class);
        when(env.getProperty("rate-limit.window-seconds", "60")).thenReturn("60");
        when(env.getProperty("rate-limit.requests-per-minute", "100")).thenReturn("100");
        when(redis.opsForZSet()).thenReturn(zset);
        when(zset.zCard(any())).thenReturn(1L);
        when(redis.expire(any(), eq(Duration.ofSeconds(60)))).thenReturn(true);

        RateLimitFilter filter = new RateLimitFilter(redis, env, counter);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/trades/123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> { };

        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("sub", "user-x").build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(jwt, null));
        filter.doFilter(request, response, chain);

        verify(zset).add(any(), any(), any(Double.class));
        String key = "rate_limit:user-x:/api/v1/trades";
        verify(zset).zCard(eq(key));
        assertTrue(true);
    }

    @Test
    void testFailOpenWhenRedisUnavailable() throws Exception {
        @SuppressWarnings("unchecked")
        RedisOperations<String, String> redis = Mockito.mock(RedisOperations.class);
        Environment env = Mockito.mock(Environment.class);
        Counter counter = Mockito.mock(Counter.class);
        when(env.getProperty("rate-limit.window-seconds", "60")).thenReturn("60");
        when(env.getProperty("rate-limit.requests-per-minute", "100")).thenReturn("100");
        when(redis.opsForZSet()).thenReturn(null);

        RateLimitFilter filter = new RateLimitFilter(redis, env, counter);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/trades");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> ((MockHttpServletResponse) res).setStatus(200);

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        verify(counter, never()).increment();
    }

    @Test
    void testFailOpenWhenRedisThrows() throws Exception {
        @SuppressWarnings("unchecked")
        RedisOperations<String, String> redis = Mockito.mock(RedisOperations.class);
        Environment env = Mockito.mock(Environment.class);
        Counter counter = Mockito.mock(Counter.class);
        when(env.getProperty("rate-limit.window-seconds", "60")).thenReturn("60");
        when(env.getProperty("rate-limit.requests-per-minute", "100")).thenReturn("100");
        when(redis.opsForZSet()).thenThrow(new RuntimeException("redis-down"));

        RateLimitFilter filter = new RateLimitFilter(redis, env, counter);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/trades");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> ((MockHttpServletResponse) res).setStatus(200);

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        verify(counter, never()).increment();
    }

    private RateLimitFixture fixtureWithCount(long count) {
        @SuppressWarnings("unchecked")
        RedisOperations<String, String> redis = Mockito.mock(RedisOperations.class);
        Environment env = Mockito.mock(Environment.class);
        Counter counter = Mockito.mock(Counter.class);
        @SuppressWarnings("unchecked")
        ZSetOperations<String, String> zset = Mockito.mock(ZSetOperations.class);

        when(env.getProperty("rate-limit.window-seconds", "60")).thenReturn("60");
        when(env.getProperty("rate-limit.requests-per-minute", "100")).thenReturn("100");
        when(redis.opsForZSet()).thenReturn(zset);
        when(zset.zCard(any())).thenReturn(count);
        when(redis.expire(any(), eq(Duration.ofSeconds(60)))).thenReturn(true);

        RateLimitFilter filter = new RateLimitFilter(redis, env, counter);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/trades");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> ((MockHttpServletResponse) res).setStatus(200);

        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("sub", "user-1").build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(jwt, null));

        return new RateLimitFixture(filter, request, response, chain);
    }

    private record RateLimitFixture(
            RateLimitFilter filter,
            MockHttpServletRequest request,
            MockHttpServletResponse response,
            FilterChain chain
    ) {
    }
}

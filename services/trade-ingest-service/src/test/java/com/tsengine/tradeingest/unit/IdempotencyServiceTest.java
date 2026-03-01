package com.tsengine.tradeingest.unit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tsengine.tradeingest.infrastructure.RedisIdempotencyStore;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.ValueOperations;

class IdempotencyServiceTest {

    @Mock
    private RedisOperations<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisIdempotencyStore redisIdempotencyStore;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        redisIdempotencyStore = new RedisIdempotencyStore(redisTemplate);
    }

    @Test
    void shouldReturnTrueWhenKeyExistsInRedis() {
        when(redisTemplate.hasKey("trade:idempotency:idem-key")).thenReturn(true);

        Boolean processed = redisIdempotencyStore.isAlreadyProcessed("idem-key");

        assertTrue(processed);
    }

    @Test
    void shouldSetRedisKeyWith24HourTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        redisIdempotencyStore.markAsProcessed("idem-key");

        verify(valueOperations).set("trade:idempotency:idem-key", "processed", Duration.ofHours(24));
    }
}

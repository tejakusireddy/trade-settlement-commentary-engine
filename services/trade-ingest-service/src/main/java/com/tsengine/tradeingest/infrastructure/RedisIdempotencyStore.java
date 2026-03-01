package com.tsengine.tradeingest.infrastructure;

import com.tsengine.tradeingest.application.IdempotencyService;
import java.time.Duration;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.stereotype.Service;

@Service
public class RedisIdempotencyStore implements IdempotencyService {

    static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);
    private static final String KEY_PREFIX = "trade:idempotency:";
    private final RedisOperations<String, String> redisTemplate;

    public RedisIdempotencyStore(RedisOperations<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Boolean isAlreadyProcessed(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + key));
    }

    @Override
    public void markAsProcessed(String key) {
        redisTemplate.opsForValue().set(KEY_PREFIX + key, "processed", IDEMPOTENCY_TTL);
    }
}

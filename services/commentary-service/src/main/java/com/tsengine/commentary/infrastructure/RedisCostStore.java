package com.tsengine.commentary.infrastructure;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisCostStore {

    private final StringRedisTemplate redisTemplate;

    public RedisCostStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public BigDecimal incrementByFloat(String key, BigDecimal amount) {
        Object result = redisTemplate.execute((RedisConnection connection) ->
                connection.execute(
                        "INCRBYFLOAT",
                        key.getBytes(StandardCharsets.UTF_8),
                        amount.toPlainString().getBytes(StandardCharsets.UTF_8)
                )
        );

        if (result instanceof byte[] bytes) {
            return new BigDecimal(new String(bytes, StandardCharsets.UTF_8));
        }
        if (result instanceof String stringValue) {
            return new BigDecimal(stringValue);
        }
        return BigDecimal.ZERO;
    }

    public void expireIfNoTtl(String key, Duration ttl) {
        Long currentTtl = redisTemplate.getExpire(key);
        if (currentTtl == null || currentTtl < 0) {
            redisTemplate.expire(key, ttl);
        }
    }

    public BigDecimal get(String key) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value);
    }
}

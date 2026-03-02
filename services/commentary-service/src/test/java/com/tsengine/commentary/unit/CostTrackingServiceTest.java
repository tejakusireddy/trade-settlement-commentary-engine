package com.tsengine.commentary.unit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tsengine.commentary.application.CostTrackingService;
import com.tsengine.commentary.config.AnthropicProperties;
import com.tsengine.commentary.infrastructure.RedisCostStore;
import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class CostTrackingServiceTest {

    @Test
    void testIncrementDailyCost() {
        FakeRedisCostStore redisCostStore = new FakeRedisCostStore();
        redisCostStore.value = new BigDecimal("0.00");
        AnthropicProperties properties = props(new BigDecimal("10.00"));
        CostTrackingService service = new CostTrackingService(redisCostStore, properties);

        service.incrementDailyCost(new BigDecimal("1.25"));

        assertTrue(redisCostStore.lastIncrementAmount.compareTo(new BigDecimal("1.25")) == 0);
        assertTrue(redisCostStore.expireCalled);
    }

    @Test
    void testCostCapExceeded() {
        FakeRedisCostStore redisCostStore = new FakeRedisCostStore();
        redisCostStore.value = new BigDecimal("10.00");
        CostTrackingService service = new CostTrackingService(redisCostStore, props(new BigDecimal("10.00")));

        assertTrue(service.isCostCapExceeded());
    }

    @Test
    void testCostCapNotExceeded() {
        FakeRedisCostStore redisCostStore = new FakeRedisCostStore();
        redisCostStore.value = new BigDecimal("9.99");
        CostTrackingService service = new CostTrackingService(redisCostStore, props(new BigDecimal("10.00")));

        assertFalse(service.isCostCapExceeded());
    }

    @Test
    void testDailyKeyFormat() {
        FakeRedisCostStore redisCostStore = new FakeRedisCostStore();
        CostTrackingService service = new CostTrackingService(redisCostStore, props(new BigDecimal("10.00")));

        String key = service.dailyCostKey();

        assertTrue(key.matches("ai:cost:daily:\\d{4}-\\d{2}-\\d{2}"));
    }

    private AnthropicProperties props(BigDecimal cap) {
        AnthropicProperties properties = new AnthropicProperties();
        properties.setApiKey("x");
        properties.setModel("claude-sonnet-4-6");
        properties.setDailyCostCapUsd(cap);
        return properties;
    }

    private static final class FakeRedisCostStore extends RedisCostStore {
        private BigDecimal value = BigDecimal.ZERO;
        private BigDecimal lastIncrementAmount = BigDecimal.ZERO;
        private boolean expireCalled;

        private FakeRedisCostStore() {
            super(null);
        }

        @Override
        public BigDecimal incrementByFloat(String key, BigDecimal amount) {
            this.lastIncrementAmount = amount;
            this.value = this.value.add(amount);
            return this.value;
        }

        @Override
        public void expireIfNoTtl(String key, Duration ttl) {
            this.expireCalled = true;
        }

        @Override
        public BigDecimal get(String key) {
            return this.value;
        }
    }
}

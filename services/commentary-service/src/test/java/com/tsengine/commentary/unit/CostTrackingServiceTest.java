package com.tsengine.commentary.unit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tsengine.commentary.application.CostTrackingService;
import com.tsengine.commentary.config.AnthropicProperties;
import com.tsengine.commentary.infrastructure.RedisCostStore;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CostTrackingServiceTest {

    @Test
    void testIncrementDailyCost() {
        RedisCostStore redisCostStore = Mockito.mock(RedisCostStore.class);
        when(redisCostStore.incrementByFloat(any(), any())).thenReturn(new BigDecimal("1.25"));
        AnthropicProperties properties = props(new BigDecimal("10.00"));
        CostTrackingService service = new CostTrackingService(redisCostStore, properties);

        service.incrementDailyCost(new BigDecimal("1.25"));

        verify(redisCostStore).incrementByFloat(any(), eq(new BigDecimal("1.25")));
    }

    @Test
    void testCostCapExceeded() {
        RedisCostStore redisCostStore = Mockito.mock(RedisCostStore.class);
        when(redisCostStore.get(any())).thenReturn(new BigDecimal("10.00"));
        CostTrackingService service = new CostTrackingService(redisCostStore, props(new BigDecimal("10.00")));

        assertTrue(service.isCostCapExceeded());
    }

    @Test
    void testCostCapNotExceeded() {
        RedisCostStore redisCostStore = Mockito.mock(RedisCostStore.class);
        when(redisCostStore.get(any())).thenReturn(new BigDecimal("9.99"));
        CostTrackingService service = new CostTrackingService(redisCostStore, props(new BigDecimal("10.00")));

        assertFalse(service.isCostCapExceeded());
    }

    @Test
    void testDailyKeyFormat() {
        RedisCostStore redisCostStore = Mockito.mock(RedisCostStore.class);
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
}

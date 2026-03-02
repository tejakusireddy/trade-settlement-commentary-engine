package com.tsengine.commentary.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tsengine.commentary.api.AiCostSummaryResponse;
import com.tsengine.commentary.api.CommentaryController;
import com.tsengine.commentary.application.AiUsageQueryService;
import com.tsengine.commentary.application.CostTrackingService;
import com.tsengine.commentary.infrastructure.RedisCostStore;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CommentaryControllerTest {

    @Test
    void shouldReturnAlignedAiCostSchema() {
        CostTrackingService costTrackingService = new StubCostTrackingService(
                new BigDecimal("1.50"),
                new BigDecimal("10.00")
        );
        CircuitBreakerRegistry circuitBreakerRegistry = mock(CircuitBreakerRegistry.class);
        CircuitBreaker circuitBreaker = mock(CircuitBreaker.class);
        AiUsageQueryService aiUsageQueryService = mock(AiUsageQueryService.class);

        when(circuitBreakerRegistry.circuitBreaker("claude-api")).thenReturn(circuitBreaker);
        when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);

        CommentaryController controller = new CommentaryController(
                null,
                null,
                costTrackingService,
                aiUsageQueryService,
                circuitBreakerRegistry
        );

        AiCostSummaryResponse result = controller.getDailyCost().data();

        assertEquals(new BigDecimal("1.50"), result.dailyCostUsd());
        assertEquals(new BigDecimal("10.00"), result.dailyCapUsd());
        assertEquals(15.0d, result.percentUsed());
        assertEquals("CLOSED", result.circuitBreakerStatus());
    }

    private static final class StubCostTrackingService extends CostTrackingService {
        private final BigDecimal dailyCost;
        private final BigDecimal dailyCap;

        private StubCostTrackingService(BigDecimal dailyCost, BigDecimal dailyCap) {
            super(new StubRedisCostStore(), new com.tsengine.commentary.config.AnthropicProperties());
            this.dailyCost = dailyCost;
            this.dailyCap = dailyCap;
        }

        @Override
        public BigDecimal getDailyCost() {
            return dailyCost;
        }

        @Override
        public BigDecimal getDailyCostCap() {
            return dailyCap;
        }
    }

    private static final class StubRedisCostStore extends RedisCostStore {
        private StubRedisCostStore() {
            super(null);
        }
    }
}

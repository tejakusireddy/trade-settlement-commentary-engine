package com.tsengine.commentary.infrastructure;

import com.tsengine.commentary.application.CostTrackingService;
import java.math.BigDecimal;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class CostCapHealthIndicator implements HealthIndicator {

    private final CostTrackingService costTrackingService;

    public CostCapHealthIndicator(CostTrackingService costTrackingService) {
        this.costTrackingService = costTrackingService;
    }

    @Override
    public Health health() {
        BigDecimal dailyCost = costTrackingService.getDailyCost();
        BigDecimal cap = costTrackingService.getDailyCostCap();
        if (costTrackingService.isCostCapExceeded()) {
            return Health.outOfService()
                    .withDetail("dailyCost", dailyCost)
                    .withDetail("cap", cap)
                    .withDetail("status", "COST_CAP_EXCEEDED")
                    .build();
        }
        return Health.up()
                .withDetail("dailyCost", dailyCost)
                .withDetail("cap", cap)
                .withDetail("status", "UNDER_CAP")
                .build();
    }
}

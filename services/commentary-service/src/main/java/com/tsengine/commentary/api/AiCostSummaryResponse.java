package com.tsengine.commentary.api;

import java.math.BigDecimal;

public record AiCostSummaryResponse(
        BigDecimal dailyCostUsd,
        BigDecimal dailyCapUsd,
        double percentUsed,
        String circuitBreakerStatus
) {
}

package com.tsengine.commentary.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AiUsageDailyPointResponse(
        LocalDate day,
        BigDecimal costUsd,
        int callCount,
        long tokensInput,
        long tokensOutput
) {
}

package com.tsengine.commentary.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AiUsageCallResponse(
        UUID commentaryId,
        String model,
        int tokensInput,
        int tokensOutput,
        BigDecimal costUsd,
        long latencyMs,
        String promptVersion,
        Instant createdAt
) {
}

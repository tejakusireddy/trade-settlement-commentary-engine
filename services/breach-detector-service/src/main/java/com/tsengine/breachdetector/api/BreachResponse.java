package com.tsengine.breachdetector.api;

import java.time.Instant;
import java.util.UUID;

public record BreachResponse(
        UUID id,
        String tradeId,
        String instrument,
        String counterparty,
        String breachType,
        String breachReason,
        Integer daysOverdue,
        Instant detectedAt,
        String status
) {
}

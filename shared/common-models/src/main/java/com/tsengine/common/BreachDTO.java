package com.tsengine.common;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable settlement breach data transfer object shared across services.
 *
 * @param id unique breach identifier
 * @param tradeId related trade identifier
 * @param breachType breach severity category (T2/T3/T5)
 * @param breachReason classified reason for breach
 * @param detectedAt timestamp when breach was detected
 * @param daysOverdue number of days beyond expected settlement
 * @param status current breach workflow status
 */
public record BreachDTO(
        UUID id,
        UUID tradeId,
        BreachType breachType,
        BreachReason breachReason,
        Instant detectedAt,
        Integer daysOverdue,
        String status
) {
}

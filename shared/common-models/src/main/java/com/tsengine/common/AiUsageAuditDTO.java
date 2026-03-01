package com.tsengine.common;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable AI usage audit data transfer object shared across services.
 *
 * @param id unique audit record identifier
 * @param commentaryId related commentary identifier
 * @param model model name used for generation
 * @param tokensInput input token count
 * @param tokensOutput output token count
 * @param costUsd USD cost for the model invocation
 * @param latencyMs model call latency in milliseconds
 * @param createdAt audit record creation timestamp
 */
public record AiUsageAuditDTO(
        UUID id,
        UUID commentaryId,
        String model,
        Integer tokensInput,
        Integer tokensOutput,
        BigDecimal costUsd,
        Integer latencyMs,
        Instant createdAt
) {
}

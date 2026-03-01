package com.tsengine.common;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable commentary data transfer object shared across services.
 *
 * @param id unique commentary identifier
 * @param breachId related breach identifier
 * @param content generated commentary content
 * @param generationType source used to generate commentary
 * @param promptVersion prompt template/version used for generation
 * @param approvedBy username who approved commentary, when applicable
 * @param approvedAt timestamp when commentary was approved
 * @param createdAt commentary creation timestamp
 */
public record CommentaryDTO(
        UUID id,
        UUID breachId,
        String content,
        CommentaryGenerationType generationType,
        String promptVersion,
        String approvedBy,
        Instant approvedAt,
        Instant createdAt
) {
}

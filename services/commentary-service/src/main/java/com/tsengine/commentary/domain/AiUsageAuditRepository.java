package com.tsengine.commentary.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiUsageAuditRepository extends JpaRepository<AiUsageAudit, UUID> {

    List<AiUsageAudit> findByCommentaryId(UUID commentaryId);

    @Query("select coalesce(sum(a.costUsd), 0) from AiUsageAudit a where a.createdAt >= :since")
    BigDecimal sumCostUsdByCreatedAtAfter(@Param("since") Instant since);
}

package com.tsengine.commentary.infrastructure;

import com.tsengine.commentary.domain.AiUsageAudit;
import com.tsengine.commentary.domain.AiUsageAuditRepository;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AiUsageAuditJpaRepository {

    private final AiUsageAuditRepository aiUsageAuditRepository;

    public AiUsageAuditJpaRepository(AiUsageAuditRepository aiUsageAuditRepository) {
        this.aiUsageAuditRepository = aiUsageAuditRepository;
    }

    public AiUsageAudit save(AiUsageAudit aiUsageAudit) {
        return aiUsageAuditRepository.save(aiUsageAudit);
    }

    public BigDecimal sumCostUsdByCreatedAtAfter(Instant since) {
        return aiUsageAuditRepository.sumCostUsdByCreatedAtAfter(since);
    }
}

package com.tsengine.commentary.infrastructure;

import com.tsengine.commentary.domain.AiUsageAudit;
import com.tsengine.commentary.domain.AiUsageAuditRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class AiUsageAuditJpaRepository {

    private final AiUsageAuditRepository aiUsageAuditRepository;

    public AiUsageAuditJpaRepository(AiUsageAuditRepository aiUsageAuditRepository) {
        this.aiUsageAuditRepository = aiUsageAuditRepository;
    }

    public AiUsageAudit save(AiUsageAudit aiUsageAudit) {
        return aiUsageAuditRepository.save(aiUsageAudit);
    }

    public List<AiUsageAudit> findByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(Instant since) {
        return aiUsageAuditRepository.findByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(since);
    }

    public BigDecimal sumCostUsdByCreatedAtAfter(Instant since) {
        return aiUsageAuditRepository.sumCostUsdByCreatedAtAfter(since);
    }
}

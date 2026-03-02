package com.tsengine.commentary.application;

import com.tsengine.commentary.api.AiUsageCallResponse;
import com.tsengine.commentary.api.AiUsageDailyPointResponse;
import com.tsengine.commentary.api.AiUsageHistoryResponse;
import com.tsengine.commentary.domain.AiUsageAudit;
import com.tsengine.commentary.infrastructure.AiUsageAuditJpaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiUsageQueryService {

    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 90;
    private static final int MIN_RECENT_LIMIT = 1;
    private static final int MAX_RECENT_LIMIT = 200;

    private final AiUsageAuditJpaRepository aiUsageAuditJpaRepository;

    public AiUsageQueryService(AiUsageAuditJpaRepository aiUsageAuditJpaRepository) {
        this.aiUsageAuditJpaRepository = aiUsageAuditJpaRepository;
    }

    @Transactional(readOnly = true)
    public AiUsageHistoryResponse getUsageHistory(int days, int recentLimit) {
        int safeDays = clamp(days, MIN_DAYS, MAX_DAYS);
        int safeRecentLimit = clamp(recentLimit, MIN_RECENT_LIMIT, MAX_RECENT_LIMIT);

        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        LocalDate startDate = todayUtc.minusDays(safeDays - 1L);
        var since = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);

        List<AiUsageAudit> audits = aiUsageAuditJpaRepository.findByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(since);
        Map<LocalDate, DailyAggregate> byDay = aggregateByDayUtc(audits);

        List<AiUsageDailyPointResponse> daily = new ArrayList<>(safeDays);
        for (int i = 0; i < safeDays; i++) {
            LocalDate day = startDate.plusDays(i);
            DailyAggregate aggregate = byDay.getOrDefault(day, DailyAggregate.empty());
            daily.add(new AiUsageDailyPointResponse(
                    day,
                    aggregate.costUsd(),
                    aggregate.callCount(),
                    aggregate.tokensInput(),
                    aggregate.tokensOutput()
            ));
        }

        List<AiUsageCallResponse> recentCalls = audits.stream()
                .limit(safeRecentLimit)
                .map(audit -> new AiUsageCallResponse(
                        audit.getCommentaryId(),
                        audit.getModel(),
                        audit.getTokensInput(),
                        audit.getTokensOutput(),
                        audit.getCostUsd(),
                        audit.getLatencyMs(),
                        audit.getPromptVersion(),
                        audit.getCreatedAt()
                ))
                .toList();

        return new AiUsageHistoryResponse(daily, recentCalls);
    }

    private Map<LocalDate, DailyAggregate> aggregateByDayUtc(List<AiUsageAudit> audits) {
        Map<LocalDate, DailyAggregate> byDay = new HashMap<>();
        for (AiUsageAudit audit : audits) {
            LocalDate day = audit.getCreatedAt().atOffset(ZoneOffset.UTC).toLocalDate();
            DailyAggregate current = byDay.getOrDefault(day, DailyAggregate.empty());
            byDay.put(day, current.add(
                    audit.getCostUsd(),
                    audit.getTokensInput(),
                    audit.getTokensOutput()
            ));
        }
        return byDay;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record DailyAggregate(BigDecimal costUsd, int callCount, long tokensInput, long tokensOutput) {
        private static DailyAggregate empty() {
            return new DailyAggregate(BigDecimal.ZERO, 0, 0L, 0L);
        }

        private DailyAggregate add(BigDecimal cost, int input, int output) {
            return new DailyAggregate(
                    costUsd.add(cost),
                    callCount + 1,
                    tokensInput + input,
                    tokensOutput + output
            );
        }
    }
}

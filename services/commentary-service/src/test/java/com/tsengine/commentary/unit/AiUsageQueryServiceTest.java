package com.tsengine.commentary.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tsengine.commentary.application.AiUsageQueryService;
import com.tsengine.commentary.domain.AiUsageAudit;
import com.tsengine.commentary.infrastructure.AiUsageAuditJpaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiUsageQueryServiceTest {

    @Test
    void shouldAggregateDailyUsageAndRecentCalls() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant todayMorning = today.atStartOfDay().plusHours(9).toInstant(ZoneOffset.UTC);
        Instant yesterdayMorning = today.minusDays(1).atStartOfDay().plusHours(10).toInstant(ZoneOffset.UTC);
        Instant yesterdayNoon = today.minusDays(1).atStartOfDay().plusHours(12).toInstant(ZoneOffset.UTC);

        InMemoryAiUsageAuditJpaRepository repository = new InMemoryAiUsageAuditJpaRepository(List.of(
                buildAudit("0.004200", 120, 80, 900, "v1", todayMorning),
                buildAudit("0.001100", 60, 40, 1100, "v1", yesterdayMorning),
                buildAudit("0.002000", 70, 50, 800, "v1", yesterdayNoon)
        ));
        AiUsageQueryService service = new AiUsageQueryService(repository);

        var response = service.getUsageHistory(2, 2);

        assertEquals(2, response.daily().size());
        assertEquals(today.minusDays(1), response.daily().get(0).day());
        assertEquals(new BigDecimal("0.003100"), response.daily().get(0).costUsd());
        assertEquals(2, response.daily().get(0).callCount());
        assertEquals(130L, response.daily().get(0).tokensInput());
        assertEquals(90L, response.daily().get(0).tokensOutput());

        assertEquals(today, response.daily().get(1).day());
        assertEquals(new BigDecimal("0.004200"), response.daily().get(1).costUsd());
        assertEquals(1, response.daily().get(1).callCount());

        assertEquals(2, response.recentCalls().size());
        assertEquals(todayMorning, response.recentCalls().get(0).createdAt());
        assertEquals(yesterdayNoon, response.recentCalls().get(1).createdAt());
    }

    private static AiUsageAudit buildAudit(
            String cost,
            int tokensIn,
            int tokensOut,
            long latencyMs,
            String promptVersion,
            Instant createdAt
    ) {
        AiUsageAudit audit = new AiUsageAudit();
        audit.setId(UUID.randomUUID());
        audit.setCommentaryId(UUID.randomUUID());
        audit.setModel("claude-sonnet-4-6");
        audit.setTokensInput(tokensIn);
        audit.setTokensOutput(tokensOut);
        audit.setCostUsd(new BigDecimal(cost));
        audit.setLatencyMs(latencyMs);
        audit.setPromptVersion(promptVersion);
        audit.setCreatedAt(createdAt);
        return audit;
    }

    private static final class InMemoryAiUsageAuditJpaRepository extends AiUsageAuditJpaRepository {
        private final List<AiUsageAudit> audits;

        private InMemoryAiUsageAuditJpaRepository(List<AiUsageAudit> audits) {
            super(null);
            this.audits = audits;
        }

        @Override
        public List<AiUsageAudit> findByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(Instant since) {
            return audits.stream()
                    .filter(audit -> !audit.getCreatedAt().isBefore(since))
                    .sorted(Comparator.comparing(AiUsageAudit::getCreatedAt).reversed())
                    .toList();
        }
    }
}

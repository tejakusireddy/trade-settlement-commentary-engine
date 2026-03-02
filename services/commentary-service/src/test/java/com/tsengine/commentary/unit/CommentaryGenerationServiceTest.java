package com.tsengine.commentary.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsengine.commentary.application.ClaudeApiService;
import com.tsengine.commentary.application.CommentaryGenerationService;
import com.tsengine.commentary.application.CostTrackingService;
import com.tsengine.commentary.application.TemplateCommentaryService;
import com.tsengine.commentary.config.AnthropicProperties;
import com.tsengine.commentary.config.KafkaTopicsProperties;
import com.tsengine.commentary.domain.AiUsageAudit;
import com.tsengine.commentary.domain.Commentary;
import com.tsengine.commentary.infrastructure.AiUsageAuditJpaRepository;
import com.tsengine.commentary.infrastructure.CommentaryJpaRepository;
import com.tsengine.commentary.infrastructure.KafkaCommentaryProducer;
import com.tsengine.common.CommentaryGenerationType;
import com.tsengine.common.CostCapExceededException;
import com.tsengine.schema.BreachEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CommentaryGenerationServiceTest {

    @Test
    void testGenerateCommentaryAiSuccess() {
        ClaudeApiService.ClaudeResponse response =
                new ClaudeApiService.ClaudeResponse("AI content", 10, 20, new BigDecimal("0.000330"), 100);
        StubClaudeApiService claudeApiService = new StubClaudeApiService(response, null);
        StubCostTrackingService costTrackingService = new StubCostTrackingService(false);
        TemplateCommentaryService templateService = new TemplateCommentaryService();
        InMemoryCommentaryJpaRepository commentaryRepo = new InMemoryCommentaryJpaRepository();
        InMemoryAiUsageAuditJpaRepository auditRepo = new InMemoryAiUsageAuditJpaRepository();
        KafkaCommentaryProducer producer = new NoopKafkaCommentaryProducer();
        Counter aiCounter = mock(Counter.class);
        Counter templateCounter = mock(Counter.class);
        Counter breakerCounter = mock(Counter.class);
        Timer timer = mock(Timer.class);

        CommentaryGenerationService service = new CommentaryGenerationService(
                claudeApiService,
                costTrackingService,
                templateService,
                commentaryRepo,
                auditRepo,
                producer,
                aiCounter,
                templateCounter,
                breakerCounter,
                timer
        );

        BreachEvent event = sampleEvent();
        Commentary result = service.generateCommentary(event);

        assertEquals(CommentaryGenerationType.AI, result.getGenerationType());
        verify(aiCounter).increment();
    }

    @Test
    void testGenerateCommentaryFallbackOnCircuitBreaker() {
        StubClaudeApiService claudeApiService = new StubClaudeApiService(
                null,
                new RuntimeException("Circuit open")
        );
        StubCostTrackingService costTrackingService = new StubCostTrackingService(false);
        TemplateCommentaryService templateService = new TemplateCommentaryService();
        InMemoryCommentaryJpaRepository commentaryRepo = new InMemoryCommentaryJpaRepository();
        InMemoryAiUsageAuditJpaRepository auditRepo = new InMemoryAiUsageAuditJpaRepository();
        KafkaCommentaryProducer producer = new NoopKafkaCommentaryProducer();
        Counter aiCounter = mock(Counter.class);
        Counter templateCounter = mock(Counter.class);
        Counter breakerCounter = mock(Counter.class);
        Timer timer = mock(Timer.class);

        CommentaryGenerationService service = new CommentaryGenerationService(
                claudeApiService,
                costTrackingService,
                templateService,
                commentaryRepo,
                auditRepo,
                producer,
                aiCounter,
                templateCounter,
                breakerCounter,
                timer
        );

        BreachEvent event = sampleEvent();
        Commentary result = service.generateCommentary(event);

        assertEquals(CommentaryGenerationType.TEMPLATE, result.getGenerationType());
        verify(templateCounter).increment();
    }

    @Test
    void testGenerateCommentaryCostCapExceeded() {
        StubClaudeApiService claudeApiService = new StubClaudeApiService(
                new ClaudeApiService.ClaudeResponse("unused", 0, 0, BigDecimal.ZERO, 0),
                null
        );
        StubCostTrackingService costTrackingService = new StubCostTrackingService(true);
        TemplateCommentaryService templateService = new TemplateCommentaryService();
        InMemoryCommentaryJpaRepository commentaryRepo = new InMemoryCommentaryJpaRepository();
        InMemoryAiUsageAuditJpaRepository auditRepo = new InMemoryAiUsageAuditJpaRepository();
        KafkaCommentaryProducer producer = new NoopKafkaCommentaryProducer();
        Counter aiCounter = mock(Counter.class);
        Counter templateCounter = mock(Counter.class);
        Counter breakerCounter = mock(Counter.class);
        Timer timer = mock(Timer.class);

        CommentaryGenerationService service = new CommentaryGenerationService(
                claudeApiService,
                costTrackingService,
                templateService,
                commentaryRepo,
                auditRepo,
                producer,
                aiCounter,
                templateCounter,
                breakerCounter,
                timer
        );

        BreachEvent event = sampleEvent();
        Commentary result = service.generateCommentary(event);

        assertEquals(CommentaryGenerationType.TEMPLATE, result.getGenerationType());
        verify(templateCounter).increment();
    }

    private BreachEvent sampleEvent() {
        return BreachEvent.newBuilder()
                .setBreachId(UUID.randomUUID().toString())
                .setTradeId(UUID.randomUUID().toString())
                .setInstrument("AAPL")
                .setCounterparty("CP-A")
                .setBreachType("T3")
                .setBreachReason("MISSING_ASSIGNMENT")
                .setDaysOverdue(3)
                .setTradeDate("2026-03-01")
                .setDetectedAt(Instant.now())
                .build();
    }

    private static final class StubClaudeApiService extends ClaudeApiService {
        private final ClaudeResponse response;
        private final RuntimeException runtimeException;

        private StubClaudeApiService(ClaudeResponse response, RuntimeException runtimeException) {
            super(new ObjectMapper(), props(), HttpClient.newHttpClient());
            this.response = response;
            this.runtimeException = runtimeException;
        }

        @Override
        public ClaudeResponse generateCommentary(BreachEvent event) {
            if (runtimeException != null) {
                throw runtimeException;
            }
            return response;
        }

        @Override
        public String promptVersion() {
            return "v1";
        }

        @Override
        public String model() {
            return "claude-sonnet-4-6";
        }

        private static AnthropicProperties props() {
            AnthropicProperties properties = new AnthropicProperties();
            properties.setApiKey("test-key");
            properties.setModel("claude-sonnet-4-6");
            properties.setDailyCostCapUsd(new BigDecimal("10.00"));
            return properties;
        }
    }

    private static final class StubCostTrackingService extends CostTrackingService {
        private final boolean throwOnValidate;

        private StubCostTrackingService(boolean throwOnValidate) {
            super(new StubRedisCostStore(), StubClaudeApiService.props());
            this.throwOnValidate = throwOnValidate;
        }

        @Override
        public void validateUnderCap() {
            if (throwOnValidate) {
                throw new CostCapExceededException("Cap exceeded");
            }
        }

        @Override
        public void incrementDailyCost(BigDecimal amount) {
            // no-op in unit test
        }
    }

    private static final class StubRedisCostStore extends com.tsengine.commentary.infrastructure.RedisCostStore {
        private StubRedisCostStore() {
            super(null);
        }
    }

    private static final class InMemoryCommentaryJpaRepository extends CommentaryJpaRepository {
        private InMemoryCommentaryJpaRepository() {
            super(null);
        }

        @Override
        public Commentary save(Commentary commentary) {
            if (commentary.getId() == null) {
                commentary.setId(UUID.randomUUID());
            }
            return commentary;
        }
    }

    private static final class InMemoryAiUsageAuditJpaRepository extends AiUsageAuditJpaRepository {
        private InMemoryAiUsageAuditJpaRepository() {
            super(null);
        }

        @Override
        public AiUsageAudit save(AiUsageAudit aiUsageAudit) {
            return aiUsageAudit;
        }
    }

    private static final class NoopKafkaCommentaryProducer extends KafkaCommentaryProducer {
        private NoopKafkaCommentaryProducer() {
            super(null, null, new KafkaTopicsProperties());
        }

        @Override
        public void publishCommentaryCompleted(
                Commentary commentary,
                double costUsd,
                int tokensInput,
                int tokensOutput
        ) {
            // no-op in unit test
        }

        @Override
        public void publishCommentaryApproved(Commentary commentary) {
            // no-op in unit test
        }
    }
}

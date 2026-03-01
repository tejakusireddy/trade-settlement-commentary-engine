package com.tsengine.commentary.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tsengine.commentary.application.ClaudeApiService;
import com.tsengine.commentary.application.CommentaryGenerationService;
import com.tsengine.commentary.application.CostTrackingService;
import com.tsengine.commentary.application.TemplateCommentaryService;
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
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CommentaryGenerationServiceTest {

    @Test
    void testGenerateCommentaryAiSuccess() {
        ClaudeApiService claudeApiService = mock(ClaudeApiService.class);
        CostTrackingService costTrackingService = mock(CostTrackingService.class);
        TemplateCommentaryService templateService = mock(TemplateCommentaryService.class);
        CommentaryJpaRepository commentaryRepo = mock(CommentaryJpaRepository.class);
        AiUsageAuditJpaRepository auditRepo = mock(AiUsageAuditJpaRepository.class);
        KafkaCommentaryProducer producer = mock(KafkaCommentaryProducer.class);
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
        ClaudeApiService.ClaudeResponse response =
                new ClaudeApiService.ClaudeResponse("AI content", 10, 20, new BigDecimal("0.000330"), 100);
        when(claudeApiService.generateCommentary(event)).thenReturn(response);
        when(claudeApiService.promptVersion()).thenReturn("v1");
        when(claudeApiService.model()).thenReturn("claude-sonnet-4-6");
        when(commentaryRepo.save(org.mockito.ArgumentMatchers.any(Commentary.class)))
                .thenAnswer(invocation -> {
                    Commentary c = invocation.getArgument(0);
                    c.setId(UUID.randomUUID());
                    return c;
                });

        Commentary result = service.generateCommentary(event);

        assertEquals(CommentaryGenerationType.AI, result.getGenerationType());
        verify(aiCounter).increment();
    }

    @Test
    void testGenerateCommentaryFallbackOnCircuitBreaker() {
        ClaudeApiService claudeApiService = mock(ClaudeApiService.class);
        CostTrackingService costTrackingService = mock(CostTrackingService.class);
        TemplateCommentaryService templateService = mock(TemplateCommentaryService.class);
        CommentaryJpaRepository commentaryRepo = mock(CommentaryJpaRepository.class);
        AiUsageAuditJpaRepository auditRepo = mock(AiUsageAuditJpaRepository.class);
        KafkaCommentaryProducer producer = mock(KafkaCommentaryProducer.class);
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
        when(claudeApiService.generateCommentary(event)).thenThrow(new RuntimeException("Circuit open"));
        when(templateService.generateCommentary(event)).thenReturn("Template");
        when(commentaryRepo.save(org.mockito.ArgumentMatchers.any(Commentary.class)))
                .thenAnswer(invocation -> {
                    Commentary c = invocation.getArgument(0);
                    c.setId(UUID.randomUUID());
                    return c;
                });

        Commentary result = service.generateCommentary(event);

        assertEquals(CommentaryGenerationType.TEMPLATE, result.getGenerationType());
        verify(templateCounter).increment();
    }

    @Test
    void testGenerateCommentaryCostCapExceeded() {
        ClaudeApiService claudeApiService = mock(ClaudeApiService.class);
        CostTrackingService costTrackingService = mock(CostTrackingService.class);
        TemplateCommentaryService templateService = mock(TemplateCommentaryService.class);
        CommentaryJpaRepository commentaryRepo = mock(CommentaryJpaRepository.class);
        AiUsageAuditJpaRepository auditRepo = mock(AiUsageAuditJpaRepository.class);
        KafkaCommentaryProducer producer = mock(KafkaCommentaryProducer.class);
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
        org.mockito.Mockito.doThrow(new CostCapExceededException("Cap exceeded"))
                .when(costTrackingService).validateUnderCap();
        when(templateService.generateCommentary(event)).thenReturn("Template");
        when(commentaryRepo.save(org.mockito.ArgumentMatchers.any(Commentary.class)))
                .thenAnswer(invocation -> {
                    Commentary c = invocation.getArgument(0);
                    c.setId(UUID.randomUUID());
                    return c;
                });

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
                .setDetectedAt(System.currentTimeMillis())
                .build();
    }
}

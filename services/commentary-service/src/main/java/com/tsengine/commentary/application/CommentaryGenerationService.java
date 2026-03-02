package com.tsengine.commentary.application;

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
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentaryGenerationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommentaryGenerationService.class);

    private final ClaudeApiService claudeApiService;
    private final CostTrackingService costTrackingService;
    private final TemplateCommentaryService templateCommentaryService;
    private final CommentaryJpaRepository commentaryJpaRepository;
    private final AiUsageAuditJpaRepository aiUsageAuditJpaRepository;
    private final KafkaCommentaryProducer kafkaCommentaryProducer;
    @Qualifier("commentaryGeneratedAiCounter")
    private final Counter commentaryGeneratedAiCounter;
    @Qualifier("commentaryGeneratedTemplateCounter")
    private final Counter commentaryGeneratedTemplateCounter;
    @Qualifier("circuitBreakerOpenCounter")
    private final Counter circuitBreakerOpenCounter;
    @Qualifier("commentaryGenerationTimer")
    private final Timer commentaryGenerationTimer;

    public CommentaryGenerationService(
            ClaudeApiService claudeApiService,
            CostTrackingService costTrackingService,
            TemplateCommentaryService templateCommentaryService,
            CommentaryJpaRepository commentaryJpaRepository,
            AiUsageAuditJpaRepository aiUsageAuditJpaRepository,
            KafkaCommentaryProducer kafkaCommentaryProducer,
            @Qualifier("commentaryGeneratedAiCounter") Counter commentaryGeneratedAiCounter,
            @Qualifier("commentaryGeneratedTemplateCounter") Counter commentaryGeneratedTemplateCounter,
            @Qualifier("circuitBreakerOpenCounter") Counter circuitBreakerOpenCounter,
            @Qualifier("commentaryGenerationTimer") Timer commentaryGenerationTimer
    ) {
        this.claudeApiService = claudeApiService;
        this.costTrackingService = costTrackingService;
        this.templateCommentaryService = templateCommentaryService;
        this.commentaryJpaRepository = commentaryJpaRepository;
        this.aiUsageAuditJpaRepository = aiUsageAuditJpaRepository;
        this.kafkaCommentaryProducer = kafkaCommentaryProducer;
        this.commentaryGeneratedAiCounter = commentaryGeneratedAiCounter;
        this.commentaryGeneratedTemplateCounter = commentaryGeneratedTemplateCounter;
        this.circuitBreakerOpenCounter = circuitBreakerOpenCounter;
        this.commentaryGenerationTimer = commentaryGenerationTimer;
    }

    @Transactional
    public Commentary generateCommentary(BreachEvent event) {
        Timer.Sample sample = Timer.start();
        try {
            costTrackingService.validateUnderCap();
            ClaudeApiService.ClaudeResponse response = claudeApiService.generateCommentary(event);

            Commentary commentary = buildCommentary(event, response.content(), CommentaryGenerationType.AI, "v1");
            Commentary saved = commentaryJpaRepository.save(commentary);

            AiUsageAudit audit = buildAudit(saved, response);
            aiUsageAuditJpaRepository.save(audit);
            costTrackingService.incrementDailyCost(response.costUsd());
            commentaryGeneratedAiCounter.increment();

            kafkaCommentaryProducer.publishCommentaryCompleted(
                    saved,
                    response.costUsd().doubleValue(),
                    response.tokensInput(),
                    response.tokensOutput()
            );
            return saved;
        } catch (CostCapExceededException ex) {
            circuitBreakerOpenCounter.increment();
            return fallbackToTemplate(event);
        } catch (Exception ex) {
            circuitBreakerOpenCounter.increment();
            LOGGER.warn("Claude commentary generation failed, using template breachId={}", event.getBreachId(), ex);
            return fallbackToTemplate(event);
        } finally {
            sample.stop(commentaryGenerationTimer);
        }
    }

    private Commentary fallbackToTemplate(BreachEvent event) {
        try {
            String content = templateCommentaryService.generateCommentary(event);
            Commentary commentary = buildCommentary(event, content, CommentaryGenerationType.TEMPLATE, "v1");
            Commentary saved = commentaryJpaRepository.save(commentary);
            commentaryGeneratedTemplateCounter.increment();
            kafkaCommentaryProducer.publishCommentaryCompleted(saved, 0.0d, 0, 0);
            return saved;
        } catch (Exception ex) {
            LOGGER.error("Template fallback failed breachId={}", event.getBreachId(), ex);
            return buildCommentary(
                    event,
                    "Commentary generation failed due to downstream dependency errors.",
                    CommentaryGenerationType.TEMPLATE,
                    "v1"
            );
        }
    }

    private Commentary buildCommentary(
            BreachEvent event,
            String content,
            CommentaryGenerationType generationType,
            String promptVersion
    ) {
        Commentary commentary = new Commentary();
        commentary.setBreachId(UUID.fromString(event.getBreachId()));
        commentary.setTradeId(toStableUuid(event.getTradeId()));
        commentary.setContent(content);
        commentary.setGenerationType(generationType);
        commentary.setPromptVersion(promptVersion);
        commentary.setCreatedAt(Instant.now());
        return commentary;
    }

    private AiUsageAudit buildAudit(Commentary commentary, ClaudeApiService.ClaudeResponse response) {
        AiUsageAudit audit = new AiUsageAudit();
        audit.setCommentaryId(commentary.getId());
        audit.setModel(claudeApiService.model());
        audit.setTokensInput(response.tokensInput());
        audit.setTokensOutput(response.tokensOutput());
        audit.setCostUsd(response.costUsd());
        audit.setLatencyMs(response.latencyMs());
        audit.setPromptVersion(claudeApiService.promptVersion());
        audit.setCreatedAt(Instant.now());
        return audit;
    }

    private UUID toStableUuid(String tradeId) {
        try {
            return UUID.fromString(tradeId);
        } catch (IllegalArgumentException ex) {
            return UUID.nameUUIDFromBytes(tradeId.getBytes(StandardCharsets.UTF_8));
        }
    }
}

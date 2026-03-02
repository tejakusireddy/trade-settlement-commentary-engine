package com.tsengine.commentary.infrastructure;

import com.tsengine.commentary.config.KafkaTopicsProperties;
import com.tsengine.commentary.domain.Commentary;
import com.tsengine.schema.CommentaryApproved;
import com.tsengine.schema.CommentaryCompleted;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
public class KafkaCommentaryProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaCommentaryProducer.class);

    private final KafkaTemplate<String, CommentaryCompleted> commentaryCompletedKafkaTemplate;
    private final KafkaTemplate<String, CommentaryApproved> commentaryApprovedKafkaTemplate;
    private final KafkaTopicsProperties topicsProperties;

    public KafkaCommentaryProducer(
            KafkaTemplate<String, CommentaryCompleted> commentaryCompletedKafkaTemplate,
            KafkaTemplate<String, CommentaryApproved> commentaryApprovedKafkaTemplate,
            KafkaTopicsProperties topicsProperties
    ) {
        this.commentaryCompletedKafkaTemplate = commentaryCompletedKafkaTemplate;
        this.commentaryApprovedKafkaTemplate = commentaryApprovedKafkaTemplate;
        this.topicsProperties = topicsProperties;
    }

    public void publishCommentaryCompleted(
            Commentary commentary,
            double costUsd,
            int tokensInput,
            int tokensOutput
    ) {
        CommentaryCompleted event = CommentaryCompleted.newBuilder()
                .setCommentaryId(commentary.getId().toString())
                .setBreachId(commentary.getBreachId().toString())
                .setTradeId(commentary.getTradeId().toString())
                .setContent(commentary.getContent())
                .setGenerationType(commentary.getGenerationType().name())
                .setPromptVersion(commentary.getPromptVersion())
                .setCostUsd(costUsd)
                .setTokensInput(tokensInput)
                .setTokensOutput(tokensOutput)
                .setCompletedAt(Instant.now())
                .build();

        try {
            SendResult<String, CommentaryCompleted> result = commentaryCompletedKafkaTemplate
                    .send(topicsProperties.getCompleted(), commentary.getTradeId().toString(), event)
                    .get();
            LOGGER.info(
                    "Published commentary completed commentaryId={} partition={} offset={}",
                    commentary.getId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while publishing commentary.completed event", ex);
        } catch (ExecutionException ex) {
            throw new RuntimeException("Failed to publish commentary.completed event", ex);
        }
    }

    public void publishCommentaryApproved(Commentary commentary) {
        if (commentary.getApprovedAt() == null || commentary.getApprovedBy() == null) {
            throw new IllegalArgumentException("Commentary approval fields must be set before publishing");
        }

        CommentaryApproved event = CommentaryApproved.newBuilder()
                .setCommentaryId(commentary.getId().toString())
                .setBreachId(commentary.getBreachId().toString())
                .setTradeId(commentary.getTradeId().toString())
                .setApprovedBy(commentary.getApprovedBy())
                .setApprovedAt(commentary.getApprovedAt())
                .build();

        try {
            SendResult<String, CommentaryApproved> result = commentaryApprovedKafkaTemplate
                    .send(topicsProperties.getApproved(), commentary.getTradeId().toString(), event)
                    .get();
            LOGGER.info(
                    "Published commentary approved commentaryId={} partition={} offset={}",
                    commentary.getId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while publishing commentary.approved event", ex);
        } catch (ExecutionException ex) {
            throw new RuntimeException("Failed to publish commentary.approved event", ex);
        }
    }
}

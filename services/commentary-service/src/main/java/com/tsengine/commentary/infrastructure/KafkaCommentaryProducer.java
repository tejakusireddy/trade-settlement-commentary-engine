package com.tsengine.commentary.infrastructure;

import com.tsengine.commentary.config.KafkaTopicsProperties;
import com.tsengine.commentary.domain.Commentary;
import com.tsengine.schema.CommentaryCompleted;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaCommentaryProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaCommentaryProducer.class);

    private final KafkaTemplate<String, CommentaryCompleted> commentaryCompletedKafkaTemplate;
    private final KafkaTopicsProperties topicsProperties;

    public KafkaCommentaryProducer(
            KafkaTemplate<String, CommentaryCompleted> commentaryCompletedKafkaTemplate,
            KafkaTopicsProperties topicsProperties
    ) {
        this.commentaryCompletedKafkaTemplate = commentaryCompletedKafkaTemplate;
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
                .setCompletedAt(System.currentTimeMillis())
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
}

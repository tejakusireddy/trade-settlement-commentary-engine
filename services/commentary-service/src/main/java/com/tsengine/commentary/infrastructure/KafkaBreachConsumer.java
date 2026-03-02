package com.tsengine.commentary.infrastructure;

import com.tsengine.commentary.application.CommentaryGenerationService;
import com.tsengine.commentary.config.KafkaTopicsProperties;
import com.tsengine.schema.BreachEvent;
import com.tsengine.schema.DlqEvent;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class KafkaBreachConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaBreachConsumer.class);

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 200L;

    private final CommentaryGenerationService commentaryGenerationService;
    private final KafkaTemplate<String, DlqEvent> commentaryDlqKafkaTemplate;
    private final KafkaTopicsProperties topicsProperties;

    public KafkaBreachConsumer(
            CommentaryGenerationService commentaryGenerationService,
            KafkaTemplate<String, DlqEvent> commentaryDlqKafkaTemplate,
            KafkaTopicsProperties topicsProperties
    ) {
        this.commentaryGenerationService = commentaryGenerationService;
        this.commentaryDlqKafkaTemplate = commentaryDlqKafkaTemplate;
        this.topicsProperties = topicsProperties;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.breaches:trade.breaches}",
            groupId = "commentary-service",
            containerFactory = "commentaryKafkaListenerContainerFactory"
    )
    public void consume(
            BreachEvent event,
            Acknowledgment acknowledgment,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic
    ) {
        Exception lastFailure = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                commentaryGenerationService.generateCommentary(event);
                acknowledgment.acknowledge();
                return;
            } catch (Exception ex) {
                lastFailure = ex;
                LOGGER.warn(
                        "Failed to process breach event breachId={} attempt={}/{}",
                        event.getBreachId(),
                        attempt,
                        MAX_RETRIES,
                        ex
                );
                if (attempt < MAX_RETRIES) {
                    sleepWithExponentialBackoff(attempt);
                }
            }
        }

        publishToDlq(event, topic, lastFailure);
        acknowledgment.acknowledge();
    }

    private void publishToDlq(BreachEvent event, String topic, Exception lastFailure) {
        try {
            DlqEvent dlqEvent = DlqEvent.newBuilder()
                    .setOriginalTopic(topic)
                    .setOriginalPayload(event.toString())
                    .setErrorMessage(lastFailure == null ? "Unknown error" : lastFailure.getMessage())
                    .setErrorClass(lastFailure == null ? "UnknownException" : lastFailure.getClass().getName())
                    .setRetryCount(MAX_RETRIES)
                    .setFailedAt(Instant.now())
                    .build();
            commentaryDlqKafkaTemplate.send(topicsProperties.getDlq(), event.getTradeId(), dlqEvent);
        } catch (Exception ex) {
            LOGGER.error("Failed to publish breach event to DLQ breachId={}", event.getBreachId(), ex);
        }
    }

    private void sleepWithExponentialBackoff(int attempt) {
        long backoff = INITIAL_BACKOFF_MS * (1L << (attempt - 1));
        try {
            Thread.sleep(backoff);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}

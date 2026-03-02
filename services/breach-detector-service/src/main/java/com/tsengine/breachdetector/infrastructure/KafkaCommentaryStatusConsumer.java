package com.tsengine.breachdetector.infrastructure;

import com.tsengine.breachdetector.application.BreachStatusService;
import com.tsengine.schema.CommentaryApproved;
import com.tsengine.schema.CommentaryCompleted;
import com.tsengine.schema.DlqEvent;
import io.micrometer.core.instrument.Counter;
import java.time.Instant;
import java.util.UUID;
import org.apache.avro.specific.SpecificRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class KafkaCommentaryStatusConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaCommentaryStatusConsumer.class);
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 200L;

    private final BreachStatusService breachStatusService;
    private final KafkaTemplate<String, DlqEvent> dlqKafkaTemplate;
    private final Counter eventsSentToDlqCounter;

    @Value("${app.kafka.topics.trade-dlq:trade.dlq}")
    private String dlqTopic;

    public KafkaCommentaryStatusConsumer(
            BreachStatusService breachStatusService,
            KafkaTemplate<String, DlqEvent> dlqKafkaTemplate,
            @Qualifier("eventsSentToDlqCounter") Counter eventsSentToDlqCounter
    ) {
        this.breachStatusService = breachStatusService;
        this.dlqKafkaTemplate = dlqKafkaTemplate;
        this.eventsSentToDlqCounter = eventsSentToDlqCounter;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.commentary-completed:commentary.completed}",
            groupId = "breach-detector-commentary-status",
            containerFactory = "commentaryCompletedKafkaListenerContainerFactory"
    )
    public void consumeCompleted(
            CommentaryCompleted event,
            Acknowledgment acknowledgment,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic
    ) {
        processWithRetry(
                event.getBreachId(),
                event,
                topic,
                () -> breachStatusService.markCommentaryGenerated(UUID.fromString(event.getBreachId())),
                acknowledgment
        );
    }

    @KafkaListener(
            topics = "${app.kafka.topics.commentary-approved:commentary.approved}",
            groupId = "breach-detector-commentary-status",
            containerFactory = "commentaryApprovedKafkaListenerContainerFactory"
    )
    public void consumeApproved(
            CommentaryApproved event,
            Acknowledgment acknowledgment,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic
    ) {
        processWithRetry(
                event.getBreachId(),
                event,
                topic,
                () -> breachStatusService.markCommentaryApproved(UUID.fromString(event.getBreachId())),
                acknowledgment
        );
    }

    private void processWithRetry(
            String breachId,
            SpecificRecord event,
            String topic,
            Runnable work,
            Acknowledgment acknowledgment
    ) {
        Exception lastFailure = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                work.run();
                acknowledgment.acknowledge();
                return;
            } catch (Exception ex) {
                lastFailure = ex;
                LOGGER.warn(
                        "Commentary status processing failed breachId={} attempt={}/{}",
                        breachId,
                        attempt,
                        MAX_RETRIES,
                        ex
                );
                if (attempt < MAX_RETRIES) {
                    sleepWithExponentialBackoff(attempt);
                }
            }
        }

        publishToDlq(event, topic, breachId, lastFailure);
        acknowledgment.acknowledge();
    }

    private void publishToDlq(SpecificRecord event, String topic, String key, Exception ex) {
        try {
            DlqEvent dlqEvent = DlqEvent.newBuilder()
                    .setOriginalTopic(topic)
                    .setOriginalPayload(event.toString())
                    .setErrorMessage(ex == null ? "Unknown error" : ex.getMessage())
                    .setErrorClass(ex == null ? "UnknownException" : ex.getClass().getName())
                    .setRetryCount(MAX_RETRIES)
                    .setFailedAt(Instant.now())
                    .build();
            dlqKafkaTemplate.send(dlqTopic, key, dlqEvent);
            eventsSentToDlqCounter.increment();
        } catch (Exception publishException) {
            LOGGER.error("Failed to publish commentary status event to DLQ key={}", key, publishException);
        }
    }

    private void sleepWithExponentialBackoff(int attempt) {
        long backoffMs = INITIAL_BACKOFF_MS * (1L << (attempt - 1));
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }
}

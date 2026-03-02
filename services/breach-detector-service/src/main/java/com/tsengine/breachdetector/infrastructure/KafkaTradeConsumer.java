package com.tsengine.breachdetector.infrastructure;

import com.tsengine.breachdetector.application.BreachDetectionService;
import com.tsengine.schema.DlqEvent;
import com.tsengine.schema.TradeEvent;
import io.micrometer.core.instrument.Counter;
import java.time.Instant;
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
public class KafkaTradeConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaTradeConsumer.class);
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 200L;

    private final BreachDetectionService breachDetectionService;
    private final KafkaTemplate<String, DlqEvent> dlqKafkaTemplate;
    private final Counter eventsSentToDlqCounter;

    @Value("${app.kafka.topics.trade-dlq:trade.dlq}")
    private String dlqTopic;

    public KafkaTradeConsumer(
            BreachDetectionService breachDetectionService,
            KafkaTemplate<String, DlqEvent> dlqKafkaTemplate,
            @Qualifier("eventsSentToDlqCounter") Counter eventsSentToDlqCounter
    ) {
        this.breachDetectionService = breachDetectionService;
        this.dlqKafkaTemplate = dlqKafkaTemplate;
        this.eventsSentToDlqCounter = eventsSentToDlqCounter;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.trade-events:trade.events}",
            groupId = "breach-detector",
            containerFactory = "manualAckKafkaListenerContainerFactory"
    )
    public void consume(
            TradeEvent event,
            Acknowledgment acknowledgment,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic
    ) {
        Exception lastFailure = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                breachDetectionService.processTrade(event);
                acknowledgment.acknowledge();
                return;
            } catch (Exception ex) {
                lastFailure = ex;
                LOGGER.warn(
                        "Trade event processing failed tradeId={} attempt={}/{}",
                        event.getTradeId(),
                        attempt,
                        MAX_RETRIES,
                        ex
                );
                if (attempt < MAX_RETRIES) {
                    sleepWithExponentialBackoff(attempt);
                }
            }
        }

        publishToDlq(event, topic, lastFailure, MAX_RETRIES);
        acknowledgment.acknowledge();
    }

    private void publishToDlq(TradeEvent event, String topic, Exception ex, int retryCount) {
        try {
            DlqEvent dlqEvent = DlqEvent.newBuilder()
                    .setOriginalTopic(topic)
                    .setOriginalPayload(event.toString())
                    .setErrorMessage(ex == null ? "Unknown error" : ex.getMessage())
                    .setErrorClass(ex == null ? "UnknownException" : ex.getClass().getName())
                    .setRetryCount(retryCount)
                    .setFailedAt(Instant.now())
                    .build();
            dlqKafkaTemplate.send(dlqTopic, event.getTradeId(), dlqEvent);
            eventsSentToDlqCounter.increment();
        } catch (Exception publishException) {
            LOGGER.error("Failed to publish event to DLQ for tradeId={}", event.getTradeId(), publishException);
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

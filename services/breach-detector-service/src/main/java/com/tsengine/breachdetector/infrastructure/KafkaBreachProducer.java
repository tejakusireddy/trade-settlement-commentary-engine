package com.tsengine.breachdetector.infrastructure;

import com.tsengine.breachdetector.application.BreachEventPublisher;
import com.tsengine.breachdetector.domain.Breach;
import com.tsengine.breachdetector.exception.BreachDetectionException;
import com.tsengine.schema.BreachEvent;
import com.tsengine.schema.TradeEvent;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
public class KafkaBreachProducer implements BreachEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaBreachProducer.class);

    private final KafkaTemplate<String, BreachEvent> breachEventKafkaTemplate;

    @Value("${app.kafka.topics.trade-breaches:trade.breaches}")
    private String breachTopic;

    public KafkaBreachProducer(KafkaTemplate<String, BreachEvent> breachEventKafkaTemplate) {
        this.breachEventKafkaTemplate = breachEventKafkaTemplate;
    }

    @Override
    public void publishBreachEvent(Breach breach, TradeEvent originalEvent) {
        BreachEvent breachEvent = BreachEvent.newBuilder()
                .setBreachId(breach.getId().toString())
                .setTradeId(breach.getTradeId().toString())
                .setInstrument(breach.getInstrument())
                .setCounterparty(breach.getCounterparty())
                .setBreachType(breach.getBreachType().name())
                .setBreachReason(breach.getBreachReason().name())
                .setDaysOverdue(breach.getDaysOverdue())
                .setTradeDate(originalEvent.getTradeDate())
                .setDetectedAt(breach.getDetectedAt().toEpochMilli())
                .build();

        try {
            SendResult<String, BreachEvent> result = breachEventKafkaTemplate
                    .send(breachTopic, breach.getTradeId().toString(), breachEvent)
                    .get();
            LOGGER.info(
                    "Published breach event breachId={} partition={} offset={}",
                    breach.getId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BreachDetectionException("Interrupted while publishing breach event", ex);
        } catch (ExecutionException ex) {
            throw new BreachDetectionException("Failed to publish breach event for tradeId=" + breach.getTradeId(), ex);
        }
    }
}

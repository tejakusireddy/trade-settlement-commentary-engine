package com.tsengine.tradeingest.infrastructure;

import com.tsengine.schema.TradeEvent;
import com.tsengine.tradeingest.application.TradeEventPublisher;
import com.tsengine.tradeingest.domain.Trade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
public class KafkaTradeProducer implements TradeEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaTradeProducer.class);
    private final KafkaTemplate<String, TradeEvent> kafkaTemplate;

    @Value("${app.kafka.topics.trade-events:trade.events}")
    private String tradeEventsTopic;

    public KafkaTradeProducer(KafkaTemplate<String, TradeEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishTradeEvent(Trade trade) {
        TradeEvent event = TradeEvent.newBuilder()
                .setTradeId(trade.getTradeId())
                .setInstrument(trade.getInstrument())
                .setTradeDate(trade.getTradeDate().toString())
                .setExpectedSettlementDate(trade.getExpectedSettlementDate().toString())
                .setCounterparty(trade.getCounterparty())
                .setQuantity(trade.getQuantity().toPlainString())
                .setPrice(trade.getPrice().toPlainString())
                .setCurrency(trade.getCurrency())
                .setStatus(trade.getStatus().name())
                .setIdempotencyKey(trade.getIdempotencyKey())
                .setTimestamp(System.currentTimeMillis())
                .build();

        try {
            SendResult<String, TradeEvent> result = kafkaTemplate
                    .send(tradeEventsTopic, trade.getTradeId(), event)
                    .get();
            LOGGER.info(
                    "Published trade event tradeId={} partition={} offset={}",
                    trade.getTradeId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
        } catch (Exception ex) {
            throw new RuntimeException("Failed to publish trade event for tradeId=" + trade.getTradeId(), ex);
        }
    }
}

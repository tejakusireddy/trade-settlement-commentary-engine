package com.tsengine.tradeingest.api;

import com.tsengine.common.TradeDTO;
import com.tsengine.common.TradeStatus;
import com.tsengine.tradeingest.domain.Trade;
import org.springframework.stereotype.Component;

@Component
public class TradeMapper {

    public Trade toEntity(TradeRequest request) {
        Trade trade = new Trade();
        trade.setTradeId(request.tradeId());
        trade.setInstrument(request.instrument());
        trade.setTradeDate(request.tradeDate());
        trade.setExpectedSettlementDate(request.expectedSettlementDate());
        trade.setCounterparty(request.counterparty());
        trade.setQuantity(request.quantity());
        trade.setPrice(request.price());
        trade.setCurrency(request.currency());
        trade.setStatus(TradeStatus.PENDING);
        trade.setIdempotencyKey(request.idempotencyKey());
        return trade;
    }

    public TradeDTO toDto(Trade trade) {
        return new TradeDTO(
                trade.getId(),
                trade.getTradeId(),
                trade.getInstrument(),
                trade.getTradeDate(),
                trade.getExpectedSettlementDate(),
                trade.getCounterparty(),
                trade.getQuantity(),
                trade.getPrice(),
                trade.getCurrency(),
                trade.getStatus(),
                trade.getCreatedAt(),
                trade.getUpdatedAt()
        );
    }
}

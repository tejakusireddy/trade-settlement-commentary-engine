package com.tsengine.tradeingest.application;

import com.tsengine.tradeingest.domain.Trade;

public interface TradeEventPublisher {

    void publishTradeEvent(Trade trade);
}

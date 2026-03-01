package com.tsengine.breachdetector.application;

import com.tsengine.breachdetector.domain.Breach;
import com.tsengine.schema.TradeEvent;

public interface BreachEventPublisher {

    void publishBreachEvent(Breach breach, TradeEvent originalEvent);
}

package com.tsengine.breachdetector.api;

import com.tsengine.breachdetector.domain.Breach;
import org.springframework.stereotype.Component;

@Component
public class BreachMapper {

    public BreachResponse toResponse(Breach breach) {
        return new BreachResponse(
                breach.getId(),
                breach.getTradeId().toString(),
                breach.getInstrument(),
                breach.getCounterparty(),
                breach.getBreachType().name(),
                breach.getBreachReason().name(),
                breach.getDaysOverdue(),
                breach.getDetectedAt(),
                breach.getStatus()
        );
    }
}

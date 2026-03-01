package com.tsengine.breachdetector.application;

import com.tsengine.breachdetector.domain.Breach;
import com.tsengine.breachdetector.domain.detector.BreachDetectionEngine;
import com.tsengine.schema.TradeEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BreachDetectionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BreachDetectionService.class);

    private final BreachDetectionEngine breachDetector;
    private final BreachPersistencePort breachPersistencePort;
    private final BreachEventPublisher breachEventPublisher;
    private final Counter breachesDetectedT2Counter;
    private final Counter breachesDetectedT3Counter;
    private final Counter breachesDetectedT5Counter;
    private final Timer breachDetectionLatencyTimer;

    public BreachDetectionService(
            BreachDetectionEngine breachDetector,
            BreachPersistencePort breachPersistencePort,
            BreachEventPublisher breachEventPublisher,
            @Qualifier("breachesDetectedT2Counter") Counter breachesDetectedT2Counter,
            @Qualifier("breachesDetectedT3Counter") Counter breachesDetectedT3Counter,
            @Qualifier("breachesDetectedT5Counter") Counter breachesDetectedT5Counter,
            @Qualifier("breachDetectionLatencyTimer") Timer breachDetectionLatencyTimer
    ) {
        this.breachDetector = breachDetector;
        this.breachPersistencePort = breachPersistencePort;
        this.breachEventPublisher = breachEventPublisher;
        this.breachesDetectedT2Counter = breachesDetectedT2Counter;
        this.breachesDetectedT3Counter = breachesDetectedT3Counter;
        this.breachesDetectedT5Counter = breachesDetectedT5Counter;
        this.breachDetectionLatencyTimer = breachDetectionLatencyTimer;
    }

    @Transactional
    public void processTrade(TradeEvent event) {
        Timer.Sample sample = Timer.start();
        try {
            breachDetector.detect(event).ifPresentOrElse(breach -> {
                Breach savedBreach = breachPersistencePort.save(breach);
                incrementCounter(savedBreach);
                breachEventPublisher.publishBreachEvent(savedBreach, event);
            }, () -> {
                LOGGER.info("No settlement breach detected for tradeId={}", event.getTradeId());
            });
        } finally {
            sample.stop(breachDetectionLatencyTimer);
        }
    }

    private void incrementCounter(Breach breach) {
        switch (breach.getBreachType()) {
            case T2 -> breachesDetectedT2Counter.increment();
            case T3 -> breachesDetectedT3Counter.increment();
            case T5 -> breachesDetectedT5Counter.increment();
            default -> {
                // No-op
            }
        }
    }
}

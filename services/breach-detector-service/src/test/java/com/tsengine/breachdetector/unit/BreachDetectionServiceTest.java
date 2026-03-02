package com.tsengine.breachdetector.unit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tsengine.breachdetector.application.BreachDetectionService;
import com.tsengine.breachdetector.application.BreachEventPublisher;
import com.tsengine.breachdetector.application.BreachPersistencePort;
import com.tsengine.breachdetector.domain.Breach;
import com.tsengine.breachdetector.domain.detector.BreachDetectionEngine;
import com.tsengine.common.BreachType;
import com.tsengine.schema.TradeEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BreachDetectionServiceTest {

    @Test
    void testProcessTrade_breachDetected() {
        BreachDetectionEngine breachDetector = mock(BreachDetectionEngine.class);
        BreachPersistencePort persistencePort = mock(BreachPersistencePort.class);
        BreachEventPublisher eventPublisher = mock(BreachEventPublisher.class);
        Counter t2Counter = mock(Counter.class);
        Counter t3Counter = mock(Counter.class);
        Counter t5Counter = mock(Counter.class);
        Timer timer = mock(Timer.class);

        BreachDetectionService service = new BreachDetectionService(
                breachDetector,
                persistencePort,
                eventPublisher,
                t2Counter,
                t3Counter,
                t5Counter,
                timer
        );

        TradeEvent event = baseEvent();
        Breach breach = new Breach();
        breach.setId(UUID.randomUUID());
        breach.setTradeId(UUID.randomUUID());
        breach.setBreachType(BreachType.T5);
        breach.setInstrument("AAPL");
        breach.setCounterparty("CPTY-A");

        when(breachDetector.detect(event)).thenReturn(Optional.of(breach));
        when(persistencePort.save(breach)).thenReturn(breach);

        service.processTrade(event);

        verify(persistencePort).save(breach);
        verify(eventPublisher).publishBreachEvent(breach, event);
        verify(t5Counter).increment();
    }

    @Test
    void testProcessTrade_noBreachDetected() {
        BreachDetectionEngine breachDetector = mock(BreachDetectionEngine.class);
        BreachPersistencePort persistencePort = mock(BreachPersistencePort.class);
        BreachEventPublisher eventPublisher = mock(BreachEventPublisher.class);
        Counter t2Counter = mock(Counter.class);
        Counter t3Counter = mock(Counter.class);
        Counter t5Counter = mock(Counter.class);
        Timer timer = mock(Timer.class);

        BreachDetectionService service = new BreachDetectionService(
                breachDetector,
                persistencePort,
                eventPublisher,
                t2Counter,
                t3Counter,
                t5Counter,
                timer
        );

        TradeEvent event = baseEvent();
        when(breachDetector.detect(event)).thenReturn(Optional.empty());

        service.processTrade(event);

        verify(breachDetector).detect(event);
        assertTrue(Optional.empty().isEmpty());
    }

    private TradeEvent baseEvent() {
        return TradeEvent.newBuilder()
                .setTradeId(UUID.randomUUID().toString())
                .setInstrument("AAPL")
                .setTradeDate("2026-03-01")
                .setExpectedSettlementDate("2026-03-02")
                .setCounterparty("CPTY-A")
                .setQuantity("100.0")
                .setPrice("200.0")
                .setCurrency("USD")
                .setStatus("PENDING")
                .setIdempotencyKey(UUID.randomUUID().toString())
                .setTimestamp(Instant.now())
                .build();
    }
}

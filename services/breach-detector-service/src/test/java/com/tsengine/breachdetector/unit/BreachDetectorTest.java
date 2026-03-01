package com.tsengine.breachdetector.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.tsengine.breachdetector.domain.Breach;
import com.tsengine.breachdetector.domain.BreachRepository;
import com.tsengine.breachdetector.domain.calendar.SettlementCalendar;
import com.tsengine.breachdetector.domain.detector.BreachDetector;
import com.tsengine.common.BreachType;
import com.tsengine.schema.TradeEvent;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BreachDetectorTest {

    @Test
    void testBreachDetected() {
        BreachRepository breachRepository = Mockito.mock(BreachRepository.class);
        SettlementCalendar settlementCalendar = new SettlementCalendar();
        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-09T00:00:00Z"), ZoneOffset.UTC);
        BreachDetector detector = new BreachDetector(settlementCalendar, breachRepository, fixedClock);
        TradeEvent event = buildTradeEvent("2026-03-02", UUID.randomUUID().toString());

        when(breachRepository.existsByTradeIdAndBreachType(Mockito.any(UUID.class), Mockito.eq(BreachType.T5)))
                .thenReturn(false);

        Optional<Breach> result = detector.detect(event);

        assertTrue(result.isPresent());
        assertEquals(BreachType.T5, result.get().getBreachType());
    }

    @Test
    void testNoBreachWhenNotOverdue() {
        BreachRepository breachRepository = Mockito.mock(BreachRepository.class);
        SettlementCalendar settlementCalendar = new SettlementCalendar();
        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-02T00:00:00Z"), ZoneOffset.UTC);
        BreachDetector detector = new BreachDetector(settlementCalendar, breachRepository, fixedClock);
        TradeEvent event = buildTradeEvent("2026-03-02", UUID.randomUUID().toString());

        Optional<Breach> result = detector.detect(event);

        assertTrue(result.isEmpty());
    }

    @Test
    void testDuplicateBreachSkipped() {
        BreachRepository breachRepository = Mockito.mock(BreachRepository.class);
        SettlementCalendar settlementCalendar = new SettlementCalendar();
        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-09T00:00:00Z"), ZoneOffset.UTC);
        BreachDetector detector = new BreachDetector(settlementCalendar, breachRepository, fixedClock);
        TradeEvent event = buildTradeEvent("2026-03-02", UUID.randomUUID().toString());

        when(breachRepository.existsByTradeIdAndBreachType(Mockito.any(UUID.class), Mockito.eq(BreachType.T5)))
                .thenReturn(true);

        Optional<Breach> result = detector.detect(event);

        assertTrue(result.isEmpty());
    }

    private TradeEvent buildTradeEvent(String expectedSettlementDate, String tradeId) {
        return TradeEvent.newBuilder()
                .setTradeId(tradeId)
                .setInstrument("AAPL")
                .setTradeDate(LocalDate.of(2026, 2, 26).toString())
                .setExpectedSettlementDate(expectedSettlementDate)
                .setCounterparty("CPTY-A")
                .setQuantity(new BigDecimal("100.00").toPlainString())
                .setPrice(new BigDecimal("120.50").toPlainString())
                .setCurrency("USD")
                .setStatus("PENDING")
                .setIdempotencyKey(UUID.randomUUID().toString())
                .setTimestamp(System.currentTimeMillis())
                .build();
    }
}

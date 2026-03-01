package com.tsengine.breachdetector.domain.detector;

import com.tsengine.breachdetector.domain.Breach;
import com.tsengine.breachdetector.domain.BreachRepository;
import com.tsengine.breachdetector.domain.calendar.SettlementCalendar;
import com.tsengine.common.BreachReason;
import com.tsengine.common.BreachType;
import com.tsengine.schema.TradeEvent;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BreachDetector implements BreachDetectionEngine {

    private final SettlementCalendar settlementCalendar;
    private final BreachRepository breachRepository;
    private Clock clock = Clock.systemUTC();

    @Autowired
    public BreachDetector(SettlementCalendar settlementCalendar, BreachRepository breachRepository) {
        this.settlementCalendar = settlementCalendar;
        this.breachRepository = breachRepository;
    }

    public BreachDetector(SettlementCalendar settlementCalendar, BreachRepository breachRepository, Clock clock) {
        this.settlementCalendar = settlementCalendar;
        this.breachRepository = breachRepository;
        this.clock = clock;
    }

    @Override
    public Optional<Breach> detect(TradeEvent event) {
        LocalDate expectedSettlement = LocalDate.parse(event.getExpectedSettlementDate());
        LocalDate today = LocalDate.now(clock);
        int daysOverdue = settlementCalendar.getDaysOverdue(expectedSettlement, today);

        if (daysOverdue <= 0) {
            return Optional.empty();
        }

        BreachType breachType = classifyBreachType(daysOverdue);
        UUID tradeId = toStableTradeUuid(event.getTradeId());
        if (breachRepository.existsByTradeIdAndBreachType(tradeId, breachType)) {
            return Optional.empty();
        }

        Breach breach = new Breach();
        breach.setTradeId(tradeId);
        breach.setInstrument(event.getInstrument());
        breach.setCounterparty(event.getCounterparty());
        breach.setBreachType(breachType);
        breach.setBreachReason(classifyBreachReason(event));
        breach.setDaysOverdue(daysOverdue);
        breach.setStatus("PENDING_COMMENTARY");
        return Optional.of(breach);
    }

    public BreachReason classifyBreachReason(TradeEvent event) {
        return BreachReason.MISSING_ASSIGNMENT;
    }

    private BreachType classifyBreachType(int daysOverdue) {
        if (daysOverdue <= 2) {
            return BreachType.T2;
        }
        if (daysOverdue <= 4) {
            return BreachType.T3;
        }
        return BreachType.T5;
    }

    private UUID toStableTradeUuid(String tradeId) {
        try {
            return UUID.fromString(tradeId);
        } catch (IllegalArgumentException ignored) {
            return UUID.nameUUIDFromBytes(tradeId.getBytes(StandardCharsets.UTF_8));
        }
    }
}

package com.tsengine.breachdetector.domain.calendar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SettlementCalendar {

    private static final Set<LocalDate> US_MARKET_HOLIDAYS = Set.of(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 1, 15),
            LocalDate.of(2024, 2, 19),
            LocalDate.of(2024, 3, 29),
            LocalDate.of(2024, 5, 27),
            LocalDate.of(2024, 6, 19),
            LocalDate.of(2024, 7, 4),
            LocalDate.of(2024, 9, 2),
            LocalDate.of(2024, 11, 28),
            LocalDate.of(2024, 12, 25),
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 1, 20),
            LocalDate.of(2025, 2, 17),
            LocalDate.of(2025, 4, 18),
            LocalDate.of(2025, 5, 26),
            LocalDate.of(2025, 6, 19),
            LocalDate.of(2025, 7, 4),
            LocalDate.of(2025, 9, 1),
            LocalDate.of(2025, 11, 27),
            LocalDate.of(2025, 12, 25),
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 19),
            LocalDate.of(2026, 2, 16),
            LocalDate.of(2026, 4, 3),
            LocalDate.of(2026, 5, 25),
            LocalDate.of(2026, 6, 19),
            LocalDate.of(2026, 7, 3),
            LocalDate.of(2026, 9, 7),
            LocalDate.of(2026, 11, 26),
            LocalDate.of(2026, 12, 25)
    );

    public boolean isSettlementDay(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }
        return !US_MARKET_HOLIDAYS.contains(date);
    }

    public LocalDate calculateSettlementDate(LocalDate tradeDate, int settlementDays) {
        LocalDate cursor = tradeDate;
        int remaining = settlementDays;
        while (remaining > 0) {
            cursor = cursor.plusDays(1);
            if (isSettlementDay(cursor)) {
                remaining--;
            }
        }
        return cursor;
    }

    public int getDaysOverdue(LocalDate expectedSettlement, LocalDate today) {
        if (!today.isAfter(expectedSettlement)) {
            return 0;
        }

        int overdue = 0;
        LocalDate cursor = expectedSettlement.plusDays(1);
        while (!cursor.isAfter(today)) {
            if (isSettlementDay(cursor)) {
                overdue++;
            }
            cursor = cursor.plusDays(1);
        }
        return overdue;
    }
}

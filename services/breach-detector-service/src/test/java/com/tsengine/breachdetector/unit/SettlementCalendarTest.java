package com.tsengine.breachdetector.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tsengine.breachdetector.domain.calendar.SettlementCalendar;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SettlementCalendarTest {

    private final SettlementCalendar calendar = new SettlementCalendar();

    @Test
    void testWeekendExclusion() {
        LocalDate tradeDate = LocalDate.of(2026, 3, 6); // Friday
        LocalDate settlementDate = calendar.calculateSettlementDate(tradeDate, 2);
        assertEquals(LocalDate.of(2026, 3, 10), settlementDate); // Tuesday
    }

    @Test
    void testHolidayExclusion() {
        LocalDate tradeDate = LocalDate.of(2025, 12, 23);
        LocalDate settlementDate = calendar.calculateSettlementDate(tradeDate, 2);
        assertEquals(LocalDate.of(2025, 12, 26), settlementDate); // Dec 25 skipped
    }

    @Test
    void testT2Detection() {
        int overdue = calendar.getDaysOverdue(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 4));
        assertEquals(2, overdue);
    }

    @Test
    void testT3Detection() {
        int overdue = calendar.getDaysOverdue(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 5));
        assertEquals(3, overdue);
    }

    @Test
    void testT5Detection() {
        int overdue = calendar.getDaysOverdue(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 9));
        assertEquals(5, overdue);
    }

    @Test
    void testNoBreachWhenSettled() {
        int overdue = calendar.getDaysOverdue(LocalDate.of(2026, 3, 3), LocalDate.of(2026, 3, 3));
        assertTrue(overdue == 0);
    }
}

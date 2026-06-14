package com.lovettj.surfspotsapi.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class ContestDateRangeParserTests {

    @Test
    void testParseShouldHandleSameMonthRange() {
        ContestDateRangeParser.DateRange dateRange = ContestDateRangeParser.parse("Apr 1 - 11", 2026);

        assertEquals(LocalDate.of(2026, 4, 1), dateRange.start());
        assertEquals(LocalDate.of(2026, 4, 11), dateRange.end());
    }

    @Test
    void testParseShouldHandleCrossMonthRange() {
        ContestDateRangeParser.DateRange dateRange = ContestDateRangeParser.parse("May 30 - Jun 1", 2026);

        assertEquals(LocalDate.of(2026, 5, 30), dateRange.start());
        assertEquals(LocalDate.of(2026, 6, 1), dateRange.end());
    }

    @Test
    void testParseShouldHandleYearBoundaryRange() {
        ContestDateRangeParser.DateRange dateRange = ContestDateRangeParser.parse("Dec 28 - Jan 3", 2026);

        assertEquals(LocalDate.of(2026, 12, 28), dateRange.start());
        assertEquals(LocalDate.of(2027, 1, 3), dateRange.end());
    }

    @Test
    void testParseShouldRejectUnknownFormat() {
        assertThrows(IllegalArgumentException.class, () -> ContestDateRangeParser.parse("TBD", 2026));
    }
}

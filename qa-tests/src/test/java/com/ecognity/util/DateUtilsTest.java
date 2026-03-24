package com.ecognity.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests – DateUtils
 *
 * Coverage:
 *   ✔ Positive  – normal months, leap year, timezones
 *   ✔ Negative  – invalid date strings
 *   ✔ Edge      – month boundaries, year wrap, Feb in leap/non-leap year
 */
@DisplayName("DateUtils Unit Tests")
class DateUtilsTest {

    // ─────────────────────────────────────────────────────────────
    // 1. daysInMonth
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("daysInMonth()")
    class DaysInMonthTests {

        @ParameterizedTest(name = "{0}-{1} should have {2} days")
        @CsvSource({
            "2026, 1,  31",   // January
            "2026, 2,  28",   // February non-leap
            "2024, 2,  29",   // February leap year
            "2026, 4,  30",   // April
            "2026, 12, 31"    // December
        })
        void positiveDaysInMonth(int year, int month, int expected) {
            LocalDate d = LocalDate.of(year, month, 1);
            assertEquals(expected, DateUtils.daysInMonth(d),
                "Days in " + year + "-" + month + " should be " + expected);
        }

        @Test
        @DisplayName("Edge – last day of year / first day of next year")
        void yearBoundary() {
            assertEquals(31, DateUtils.daysInMonth(LocalDate.of(2026, 12, 31)));
            assertEquals(31, DateUtils.daysInMonth(LocalDate.of(2027, 1, 1)));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 2. startDayOfWeek
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("startDayOfWeek()")
    class StartDayTests {

        @ParameterizedTest(name = "{0}-{1}-01 starts on day {2}")
        @CsvSource({
            "2026, 3,  7",   // March 2026 starts on Sunday (7)
            "2026, 1,  4",   // Jan 2026 starts on Thursday (4)
            "2024, 1,  1",   // Jan 2024 starts on Monday (1)
            "2026, 2,  7",   // Feb 2026 starts on Sunday (7)
        })
        void positiveStartDay(int year, int month, int expectedDow) {
            LocalDate d = LocalDate.of(year, month, 1);
            assertEquals(expectedDow, DateUtils.startDayOfWeek(d));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 3. isToday
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isToday()")
    class IsTodayTests {

        @Test
        @DisplayName("Positive – today returns true")
        void todayReturnsTrue() {
            assertTrue(DateUtils.isToday(LocalDate.now()));
        }

        @Test
        @DisplayName("Negative – yesterday returns false")
        void yesterdayReturnsFalse() {
            assertFalse(DateUtils.isToday(LocalDate.now().minusDays(1)));
        }

        @Test
        @DisplayName("Negative – tomorrow returns false")
        void tomorrowReturnsFalse() {
            assertFalse(DateUtils.isToday(LocalDate.now().plusDays(1)));
        }

        @Test
        @DisplayName("Edge – far future date is not today")
        void farFutureNotToday() {
            assertFalse(DateUtils.isToday(LocalDate.of(2099, 12, 31)));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 4. calendarGridDays
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("calendarGridDays()")
    class CalendarGridTests {

        @Test
        @DisplayName("Positive – grid contains all days of the month")
        void allDaysPresent() {
            LocalDate march2026 = LocalDate.of(2026, 3, 1);
            List<LocalDate> grid = DateUtils.calendarGridDays(march2026);
            long realDays = grid.stream().filter(d -> d != null).count();
            assertEquals(31, realDays, "March 2026 must have 31 real days in grid");
        }

        @Test
        @DisplayName("Positive – padding nulls before 1st are correct")
        void paddingCountCorrect() {
            LocalDate march2026 = LocalDate.of(2026, 3, 1); // Sunday = 7, offset = 6
            List<LocalDate> grid = DateUtils.calendarGridDays(march2026);
            long nulls = grid.stream().filter(d -> d == null).count();
            assertEquals(6, nulls, "Sunday start means 6 padding nulls (Mon-Sat)");
        }

        @Test
        @DisplayName("Edge – month starting on Monday has zero padding")
        void noNullsForMondayStart() {
            LocalDate jan2024 = LocalDate.of(2024, 1, 1); // Monday
            List<LocalDate> grid = DateUtils.calendarGridDays(jan2024);
            assertFalse(grid.isEmpty());
            assertNotNull(grid.get(0), "Monday start: first cell should be the 1st");
        }

        @Test
        @DisplayName("Edge – February leap year has 29 days in grid")
        void leapFebGrid() {
            LocalDate feb2024 = LocalDate.of(2024, 2, 1);
            List<LocalDate> grid = DateUtils.calendarGridDays(feb2024);
            long realDays = grid.stream().filter(d -> d != null).count();
            assertEquals(29, realDays);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 5. parse / format
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("parse() and format()")
    class ParseFormatTests {

        @ParameterizedTest(name = "parse({0}) should succeed")
        @ValueSource(strings = {"2026-01-01", "2026-03-24", "2024-02-29", "2026-12-31"})
        void positiveParseValidDates(String dateStr) {
            assertDoesNotThrow(() -> DateUtils.parse(dateStr));
        }

        @ParameterizedTest(name = "parse({0}) should throw")
        @ValueSource(strings = {"not-a-date", "2026-13-01", "2026-00-15", "", "2026/03/24"})
        void negativeParseInvalidDates(String badStr) {
            assertThrows(DateTimeParseException.class, () -> DateUtils.parse(badStr));
        }

        @Test
        @DisplayName("Positive – format produces ISO string")
        void formatRoundTrip() {
            LocalDate d = LocalDate.of(2026, 3, 24);
            assertEquals("2026-03-24", DateUtils.format(d));
        }

        @Test
        @DisplayName("Edge – parse then format is identity")
        void parseFormatIdentity() {
            String original = "2026-03-24";
            assertEquals(original, DateUtils.format(DateUtils.parse(original)));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 6. Timezone awareness
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Edge – todayIn different timezone may differ by ±1 day")
    void timezoneDateDifference() {
        LocalDate utcDate = DateUtils.todayIn(ZoneId.of("UTC"));
        LocalDate istDate = DateUtils.todayIn(ZoneId.of("Asia/Kolkata"));
        // They are either equal or differ by 1 day
        long diff = Math.abs(utcDate.toEpochDay() - istDate.toEpochDay());
        assertTrue(diff <= 1, "UTC and IST dates should differ by at most 1 day");
    }
}

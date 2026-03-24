package com.ecognity.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Date / Calendar utility – mirrors the logic used in app.js.
 */
public class DateUtils {

    private DateUtils() {}

    /** Returns the current date in the system default timezone. */
    public static LocalDate today() {
        return LocalDate.now();
    }

    /** Returns the current date in the given timezone. */
    public static LocalDate todayIn(ZoneId zone) {
        return LocalDate.now(zone);
    }

    /** Total days in the month of the supplied date. */
    public static int daysInMonth(LocalDate date) {
        return date.lengthOfMonth();
    }

    /** 1-based day of the week for the 1st of the month (Mon=1 … Sun=7). */
    public static int startDayOfWeek(LocalDate date) {
        return date.withDayOfMonth(1).getDayOfWeek().getValue();
    }

    /** True when the supplied date is today (system tz). */
    public static boolean isToday(LocalDate date) {
        return date.equals(today());
    }

    /**
     * Generates all days that should appear in a calendar grid
     * (including padding nulls for days before the 1st).
     */
    public static List<LocalDate> calendarGridDays(LocalDate month) {
        List<LocalDate> days = new ArrayList<>();
        int startOffset = startDayOfWeek(month) - 1; // Mon=0 … Sun=6
        for (int i = 0; i < startOffset; i++) days.add(null);
        for (int d = 1; d <= daysInMonth(month); d++) {
            days.add(month.withDayOfMonth(d));
        }
        return days;
    }

    /** Format a date as "YYYY-MM-DD". */
    public static String format(LocalDate date) {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /** Parse "YYYY-MM-DD" into a LocalDate; throws DateTimeParseException on bad input. */
    public static LocalDate parse(String dateStr) {
        return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
    }
}

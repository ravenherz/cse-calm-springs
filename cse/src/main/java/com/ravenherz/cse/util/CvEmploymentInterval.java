package com.ravenherz.cse.util;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Turns {@code yyyy-MM-dd;Now} / {@code yyyy-MM-dd;yyyy-MM-dd} into a CV date line.
 */
public final class CvEmploymentInterval {

    private static final DateTimeFormatter MONTH_YEAR =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH =
            DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH);

    private CvEmploymentInterval() {
    }

    public static String format(String raw) {
        return format(raw, Clock.systemDefaultZone());
    }

    public static String format(String raw, Clock clock) {
        if (raw == null || raw.isBlank() || clock == null) {
            return "";
        }
        String[] parts = raw.split(";", 2);
        if (parts.length != 2) {
            return "";
        }
        LocalDate start = parseDate(parts[0]);
        if (start == null) {
            return "";
        }
        String endRaw = parts[1].trim();
        boolean ongoing = isNow(endRaw);
        LocalDate end = ongoing ? LocalDate.now(clock) : parseDate(endRaw);
        if (end == null || end.isBefore(start)) {
            return "";
        }
        return range(start, end, ongoing) + " (" + duration(start, end) + ")";
    }

    private static boolean isNow(String value) {
        return "now".equalsIgnoreCase(value) || "present".equalsIgnoreCase(value);
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static String range(LocalDate start, LocalDate end, boolean ongoing) {
        String startText = start.format(MONTH_YEAR);
        if (ongoing) {
            return startText + " – Present";
        }
        if (start.getYear() == end.getYear()) {
            return startText + " – " + end.format(MONTH);
        }
        return startText + " – " + end.format(MONTH_YEAR);
    }

    private static String duration(LocalDate start, LocalDate end) {
        Period period = Period.between(start, end);
        int years = period.getYears();
        int months = period.getMonths();
        if (period.getDays() >= 15) {
            months++;
        }
        if (months >= 12) {
            years += months / 12;
            months = months % 12;
        }
        List<String> parts = new ArrayList<>();
        if (years > 0) {
            parts.add(years == 1 ? "1 year" : years + " years");
        }
        if (months > 0) {
            parts.add(months == 1 ? "1 month" : months + " months");
        }
        if (parts.isEmpty()) {
            return "less than a month";
        }
        return String.join(" ", parts);
    }
}

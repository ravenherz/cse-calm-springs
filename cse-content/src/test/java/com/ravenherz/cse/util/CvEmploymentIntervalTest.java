package com.ravenherz.cse.util;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CvEmploymentIntervalTest {

    private static final Clock NOW = Clock.fixed(Instant.parse("2026-09-19T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void ongoingEmploymentUsesPresentAndRoundedMonths() {
        assertEquals("September 2025 – Present (1 year 1 month)",
                CvEmploymentInterval.format("2025-09-01;Now", NOW));
        assertEquals("September 2025 – Present (1 year 1 month)",
                CvEmploymentInterval.format("2025-09-01;present", NOW));
    }

    @Test
    void finishedSameYearOmitsEndYear() {
        assertEquals("September 2025 – October (1 month)",
                CvEmploymentInterval.format("2025-09-01;2025-10-01", NOW));
    }

    @Test
    void finishedDifferentYearsKeepsBothYears() {
        assertEquals("May 2022 – March 2025 (2 years 11 months)",
                CvEmploymentInterval.format("2022-05-01;2025-03-31", NOW));
    }

    @Test
    void blankAndInvalidAreEmpty() {
        assertEquals("", CvEmploymentInterval.format(null, NOW));
        assertEquals("", CvEmploymentInterval.format("2025-09-01", NOW));
        assertEquals("", CvEmploymentInterval.format("2025-09-01;never", NOW));
        assertEquals("", CvEmploymentInterval.format("2025-10-01;2025-09-01", NOW));
    }

    @Test
    void sameDayIsLessThanAMonth() {
        assertTrue(CvEmploymentInterval.format("2025-09-01;2025-09-01", NOW)
                .contains("less than a month"));
    }
}

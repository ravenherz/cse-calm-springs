package com.ravenherz.cse.util;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthRateLimiterTest {

    @Test
    void tenFailuresInWindowThenDeniedUntilSuccessOrExpiry() {
        MutableClock clock = new MutableClock();
        AuthRateLimiter limiter = new AuthRateLimiter(AuthRateLimiter.MAX_ATTEMPTS, AuthRateLimiter.WINDOW_MS, clock);
        String ip = "203.0.113.9";

        for (int i = 0; i < AuthRateLimiter.MAX_ATTEMPTS; i++) {
            assertTrue(limiter.allow(ip), "attempt " + i);
            limiter.recordFailure(ip);
        }
        assertFalse(limiter.allow(ip));

        limiter.recordSuccess(ip);
        assertTrue(limiter.allow(ip));

        for (int i = 0; i < AuthRateLimiter.MAX_ATTEMPTS; i++) {
            limiter.recordFailure(ip);
        }
        assertFalse(limiter.allow(ip));

        clock.advance(Duration.ofMinutes(15).plusMillis(1));
        assertTrue(limiter.allow(ip));
    }

    @Test
    void blankKeySharesUnknownBucket() {
        AuthRateLimiter limiter = new AuthRateLimiter();
        for (int i = 0; i < AuthRateLimiter.MAX_ATTEMPTS; i++) {
            limiter.recordFailure("  ");
        }
        assertFalse(limiter.allow(null));
        limiter.recordSuccess("unknown");
        assertTrue(limiter.allow(""));
    }

    private static final class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-08-22T00:00:00Z");

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}

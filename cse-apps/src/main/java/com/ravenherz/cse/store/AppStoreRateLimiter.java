package com.ravenherz.cse.store;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AppStoreRateLimiter {

    static final int MAX_WRITES = 120;
    static final long WINDOW_MS = 60_000L;

    private final int maxWrites;
    private final long windowMs;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> attempts = new ConcurrentHashMap<>();

    public AppStoreRateLimiter() {
        this(MAX_WRITES, WINDOW_MS, Clock.systemUTC());
    }

    AppStoreRateLimiter(int maxWrites, long windowMs, Clock clock) {
        this.maxWrites = maxWrites;
        this.windowMs = windowMs;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public boolean allow(String key) {
        prune();
        if (key == null || key.isBlank()) {
            key = "unknown";
        }
        Window window = attempts.compute(key, (ignored, current) -> {
            long now = now();
            if (current == null || now - current.startedAt > windowMs) {
                return new Window(now, 1);
            }
            current.count++;
            return current;
        });
        return window.count <= maxWrites;
    }

    private void prune() {
        long now = now();
        Iterator<Map.Entry<String, Window>> iterator = attempts.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Window> entry = iterator.next();
            if (now - entry.getValue().startedAt > windowMs) {
                iterator.remove();
            }
        }
    }

    private long now() {
        return clock.millis();
    }

    private static final class Window {
        private final long startedAt;
        private int count;

        private Window(long startedAt, int count) {
            this.startedAt = startedAt;
            this.count = count;
        }
    }
}

package com.ravenherz.cse.util.video;

final class FfmpegProgress {

    private Double durationSeconds;
    private int percent;

    FfmpegProgress(Double durationSeconds) {
        this.durationSeconds = durationSeconds != null && durationSeconds > 0 ? durationSeconds : null;
        this.percent = 0;
    }

    synchronized int accept(String line) {
        Double banner = durationFromBanner(line);
        if (banner != null) {
            durationSeconds = banner;
        }
        percent = compute(line, durationSeconds, percent);
        return percent;
    }

    static int percent(String line, Double durationSeconds, int current) {
        return compute(line, durationSeconds, current);
    }

    static int compute(String line, Double durationSeconds, int current) {
        if (line == null || line.isBlank()) {
            return current;
        }
        String trimmed = line.trim();
        if ("progress=end".equals(trimmed)) {
            return 100;
        }
        Long micros = micros(trimmed);
        if (micros == null || durationSeconds == null || durationSeconds <= 0) {
            return current;
        }
        long total = Math.max(1L, Math.round(durationSeconds * 1_000_000d));
        int next = (int) Math.round(100d * micros / total);
        return Math.max(current, Math.max(0, Math.min(99, next)));
    }

    static Double durationFromBanner(String line) {
        if (line == null) {
            return null;
        }
        int idx = line.indexOf("Duration:");
        if (idx < 0) {
            return null;
        }
        String rest = line.substring(idx + "Duration:".length()).trim();
        int comma = rest.indexOf(',');
        if (comma >= 0) {
            rest = rest.substring(0, comma).trim();
        }
        Long micros = parseClock(rest);
        if (micros == null || micros <= 0) {
            return null;
        }
        return micros / 1_000_000d;
    }

    static Long micros(String line) {
        if (line.startsWith("out_time_us=")) {
            return parseLong(line.substring("out_time_us=".length()));
        }
        // FFmpeg names this "ms" but the value is microseconds (AV_TIME_BASE).
        if (line.startsWith("out_time_ms=")) {
            return parseLong(line.substring("out_time_ms=".length()));
        }
        if (line.startsWith("out_time=")) {
            return parseClock(line.substring("out_time=".length()));
        }
        int idx = line.indexOf("time=");
        if (idx >= 0 && (idx < 4 || !line.startsWith("out_", idx - 4))) {
            String rest = line.substring(idx + "time=".length()).trim();
            int end = 0;
            while (end < rest.length() && !Character.isWhitespace(rest.charAt(end))) {
                end++;
            }
            return parseClock(rest.substring(0, end));
        }
        return null;
    }

    static Long parseClock(String value) {
        if (value == null || value.isBlank() || "N/A".equalsIgnoreCase(value.trim())) {
            return null;
        }
        String clock = value.trim();
        int colon = clock.lastIndexOf(':');
        if (colon < 0) {
            return null;
        }
        int first = clock.indexOf(':');
        try {
            long hours;
            long minutes;
            double seconds;
            if (first == colon) {
                hours = 0;
                minutes = Long.parseLong(clock.substring(0, colon));
                seconds = Double.parseDouble(clock.substring(colon + 1));
            } else {
                hours = Long.parseLong(clock.substring(0, first));
                minutes = Long.parseLong(clock.substring(first + 1, colon));
                seconds = Double.parseDouble(clock.substring(colon + 1));
            }
            return Math.round(((hours * 3600d) + (minutes * 60d) + seconds) * 1_000_000d);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long parseLong(String raw) {
        try {
            long value = Long.parseLong(raw.trim());
            return value < 0 ? 0L : value;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

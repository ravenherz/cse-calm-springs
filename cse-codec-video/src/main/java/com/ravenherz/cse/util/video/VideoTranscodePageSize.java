package com.ravenherz.cse.util.video;

public final class VideoTranscodePageSize {

    public static final int DEFAULT = 20;

    private VideoTranscodePageSize() {
    }

    public static int normalize(int size) {
        if (size == 50 || size == 100) {
            return size;
        }
        return DEFAULT;
    }

    public static int normalize(String raw) {
        return normalize(parse(raw, DEFAULT));
    }

    public static int page(String raw) {
        int page = parse(raw, 1);
        return page < 1 ? 1 : page;
    }

    private static int parse(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}

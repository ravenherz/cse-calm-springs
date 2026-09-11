package com.ravenherz.cse.util;

import java.util.Locale;

public enum UrlTemplateSize {
    XS,
    S,
    M,
    L;

    public static UrlTemplateSize parse(String raw, UrlTemplateSize fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "xs" -> XS;
            case "s" -> S;
            case "m" -> M;
            case "l" -> L;
            default -> fallback;
        };
    }

    public String css() {
        return name().toLowerCase(Locale.ROOT);
    }
}

package com.ravenherz.cse.store;

import java.util.Locale;

public enum AppStoreAccess {
    OWNER,
    PUBLIC_READ,
    MEMBER_WRITE,
    ADMIN;

    public String wire() {
        return switch (this) {
            case OWNER -> "owner";
            case PUBLIC_READ -> "public-read";
            case MEMBER_WRITE -> "member-write";
            case ADMIN -> "admin";
        };
    }

    public static AppStoreAccess parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return OWNER;
        }
        String key = raw.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        return switch (key) {
            case "owner" -> OWNER;
            case "public-read" -> PUBLIC_READ;
            case "member-write" -> MEMBER_WRITE;
            case "admin" -> ADMIN;
            default -> throw new AppStoreException(400, "Unknown table access: " + raw);
        };
    }

    public static AppStoreAccess parseOrDefault(String raw) {
        try {
            return parse(raw);
        } catch (AppStoreException ex) {
            return OWNER;
        }
    }
}

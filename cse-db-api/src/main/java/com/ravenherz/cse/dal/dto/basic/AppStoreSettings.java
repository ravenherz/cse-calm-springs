package com.ravenherz.cse.dal.dto.basic;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-install data-store grant and quotas. Lives on {@code cse-apps} / {@link AppData}
 * so a re-upload of the same slug keeps operator choices.
 */
public final class AppStoreSettings {

    public static final int DEFAULT_MAX_DATA_BYTES = 256 * 1024;
    public static final int DEFAULT_MAX_DOCS = 10_000;
    public static final long DEFAULT_MAX_BYTES = 32L * 1024 * 1024;

    public static final int MIN_MAX_DATA_BYTES = 4 * 1024;
    public static final int ABSOLUTE_MAX_DATA_BYTES = 2 * 1024 * 1024;
    public static final int MIN_MAX_DOCS = 1;
    public static final int ABSOLUTE_MAX_DOCS = 1_000_000;
    public static final long MIN_MAX_BYTES = 64L * 1024;
    public static final long ABSOLUTE_MAX_BYTES = 2L * 1024 * 1024 * 1024;

    private boolean enabled;
    private boolean schemaOpen;
    private int maxDataBytes = DEFAULT_MAX_DATA_BYTES;
    private int maxDocs = DEFAULT_MAX_DOCS;
    private long maxBytes = DEFAULT_MAX_BYTES;

    public AppStoreSettings() {
    }

    public static AppStoreSettings defaults() {
        return new AppStoreSettings();
    }

    public static AppStoreSettings fromLegacy(boolean enabled, boolean schemaOpen) {
        AppStoreSettings settings = new AppStoreSettings();
        settings.enabled = enabled;
        settings.schemaOpen = schemaOpen;
        return settings;
    }

    public static AppStoreSettings copyOf(AppStoreSettings source) {
        if (source == null) {
            return defaults();
        }
        AppStoreSettings copy = new AppStoreSettings();
        copy.enabled = source.enabled;
        copy.schemaOpen = source.schemaOpen;
        copy.maxDataBytes = source.maxDataBytes;
        copy.maxDocs = source.maxDocs;
        copy.maxBytes = source.maxBytes;
        copy.normalize();
        return copy;
    }

    public void normalize() {
        maxDataBytes = clamp(maxDataBytes, MIN_MAX_DATA_BYTES, ABSOLUTE_MAX_DATA_BYTES, DEFAULT_MAX_DATA_BYTES);
        maxDocs = clamp(maxDocs, MIN_MAX_DOCS, ABSOLUTE_MAX_DOCS, DEFAULT_MAX_DOCS);
        maxBytes = clamp(maxBytes, MIN_MAX_BYTES, ABSOLUTE_MAX_BYTES, DEFAULT_MAX_BYTES);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSchemaOpen() {
        return schemaOpen;
    }

    public void setSchemaOpen(boolean schemaOpen) {
        this.schemaOpen = schemaOpen;
    }

    public int getMaxDataBytes() {
        return maxDataBytes;
    }

    public void setMaxDataBytes(int maxDataBytes) {
        this.maxDataBytes = maxDataBytes;
    }

    public int getMaxDocs() {
        return maxDocs;
    }

    public void setMaxDocs(int maxDocs) {
        this.maxDocs = maxDocs;
    }

    public long getMaxBytes() {
        return maxBytes;
    }

    public void setMaxBytes(long maxBytes) {
        this.maxBytes = maxBytes;
    }

    public int resolvedMaxDataBytes() {
        return clamp(maxDataBytes, MIN_MAX_DATA_BYTES, ABSOLUTE_MAX_DATA_BYTES, DEFAULT_MAX_DATA_BYTES);
    }

    public int resolvedMaxDocs() {
        return clamp(maxDocs, MIN_MAX_DOCS, ABSOLUTE_MAX_DOCS, DEFAULT_MAX_DOCS);
    }

    public long resolvedMaxBytes() {
        return clamp(maxBytes, MIN_MAX_BYTES, ABSOLUTE_MAX_BYTES, DEFAULT_MAX_BYTES);
    }

    public int maxDataKb() {
        return Math.max(1, resolvedMaxDataBytes() / 1024);
    }

    public int maxBytesMb() {
        return Math.max(1, (int) (resolvedMaxBytes() / (1024L * 1024L)));
    }

    public Map<String, Object> toMap() {
        normalize();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", enabled);
        out.put("schemaOpen", schemaOpen);
        out.put("maxDataBytes", maxDataBytes);
        out.put("maxDocs", maxDocs);
        out.put("maxBytes", maxBytes);
        return out;
    }

    public static AppStoreSettings fromMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        AppStoreSettings settings = new AppStoreSettings();
        settings.enabled = bool(map.get("enabled"), false);
        settings.schemaOpen = bool(map.get("schemaOpen"), false);
        settings.maxDataBytes = integer(map.get("maxDataBytes"), DEFAULT_MAX_DATA_BYTES);
        settings.maxDocs = integer(map.get("maxDocs"), DEFAULT_MAX_DOCS);
        settings.maxBytes = longValue(map.get("maxBytes"), DEFAULT_MAX_BYTES);
        settings.normalize();
        return settings;
    }

    private static int clamp(int value, int min, int max, int fallback) {
        if (value < min || value > max) {
            return fallback;
        }
        return value;
    }

    private static long clamp(long value, long min, long max, long fallback) {
        if (value < min || value > max) {
            return fallback;
        }
        return value;
    }

    private static boolean bool(Object value, boolean fallback) {
        if (value instanceof Boolean flag) {
            return flag;
        }
        if (value == null) {
            return fallback;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private static int integer(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static long longValue(Object value, long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}

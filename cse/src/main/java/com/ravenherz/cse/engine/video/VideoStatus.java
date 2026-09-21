package com.ravenherz.cse.engine.video;

import com.ravenherz.cse.dal.DalStrings;
import com.ravenherz.cse.dal.dto.basic.ResourceData;
import com.ravenherz.cse.dal.dto.basic.enums.ResourceType;

import java.util.Map;

public final class VideoStatus {

    public static final String KEY = "transcode";
    public static final String ERROR_KEY = "transcodeError";
    public static final String SOURCE_EXT_KEY = "sourceExtension";
    public static final String SOURCE_SIZE_KEY = "sourceSize";
    public static final String PROCESSING = "processing";
    public static final String READY = "ready";
    public static final String FAILED = "failed";

    private VideoStatus() {
    }

    public static String of(ResourceData data) {
        if (data == null || data.getMetadata() == null) {
            return "";
        }
        String status = data.getMetadata().get(KEY);
        return status == null ? "" : status.trim();
    }

    public static boolean processing(ResourceData data) {
        return PROCESSING.equalsIgnoreCase(of(data));
    }

    public static boolean failed(ResourceData data) {
        return FAILED.equalsIgnoreCase(of(data));
    }

    public static boolean ready(ResourceData data) {
        if (data == null || data.getType() != ResourceType.VIDEO) {
            return false;
        }
        String status = of(data);
        return status.isEmpty() || READY.equalsIgnoreCase(status);
    }

    public static void set(ResourceData data, String status) {
        if (data == null || status == null || status.isBlank()) {
            return;
        }
        data.addMetadata(KEY, status);
        if (!FAILED.equals(status)) {
            Map<String, String> metadata = data.getMetadata();
            if (metadata != null) {
                metadata.remove(ERROR_KEY);
            }
        }
    }

    public static void fail(ResourceData data, String message) {
        if (data == null) {
            return;
        }
        data.addMetadata(KEY, FAILED);
        if (message != null && !message.isBlank()) {
            String trimmed = message.trim();
            if (trimmed.length() > 240) {
                trimmed = trimmed.substring(0, 240).trim();
            }
            data.addMetadata(ERROR_KEY, trimmed);
        }
    }

    public static void rememberSourceSize(ResourceData data) {
        if (data == null || sourceSizeBytes(data) != null || data.getSizeInBytes() <= 0) {
            return;
        }
        data.addMetadata(SOURCE_SIZE_KEY, Long.toString(data.getSizeInBytes()));
    }

    public static Long sourceSizeBytes(ResourceData data) {
        if (data == null || data.getMetadata() == null) {
            return null;
        }
        String raw = data.getMetadata().get(SOURCE_SIZE_KEY);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            long value = Long.parseLong(raw.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String sizeLabel(Long bytes) {
        if (bytes == null || bytes <= 0) {
            return null;
        }
        return DalStrings.formatByteSize(bytes);
    }
}

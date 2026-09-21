package com.ravenherz.cse.util;

import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.dal.ConfigSource;
import com.ravenherz.cse.dal.dto.basic.enums.ResourceType;

public final class ResourceUploadLimits {

    public static final int DEFAULT_IMAGE_MB = 50;
    public static final int DEFAULT_VIDEO_MB = 200;
    public static final int DEFAULT_AUDIO_MB = 50;
    public static final int DEFAULT_BINARY_MB = 50;
    public static final int MAX_MB = -1;
    private static final int MIN_MB = 1;

    /**
     * Tomcat {@code maxPostSize} / {@code maxSwallowSize}: {@code -1} is unlimited.
     */
    public static int servletCeilingBytes() {
        if (MAX_MB < 0) {
            return -1;
        }
        long bytes = (long) MAX_MB * 1024L * 1024L;
        return (int) Math.min(bytes, Integer.MAX_VALUE);
    }

    private final int imageMb;
    private final int videoMb;
    private final int audioMb;
    private final int binaryMb;

    private ResourceUploadLimits(int imageMb, int videoMb, int audioMb, int binaryMb) {
        this.imageMb = imageMb;
        this.videoMb = videoMb;
        this.audioMb = audioMb;
        this.binaryMb = binaryMb;
    }

    public static ResourceUploadLimits defaults() {
        return new ResourceUploadLimits(DEFAULT_IMAGE_MB, DEFAULT_VIDEO_MB, DEFAULT_AUDIO_MB, DEFAULT_BINARY_MB);
    }

    public static ResourceUploadLimits from(ConfigSource settings) {
        if (settings == null) {
            return defaults();
        }
        return new ResourceUploadLimits(
                parseMb(settings.getValue(SettingKeys.CONTEXT_DATASOURCE_UPLOAD_LIMITS,
                        SettingKeys.KEY_IMAGE_UPLOAD_MAX_MB), DEFAULT_IMAGE_MB),
                parseMb(settings.getValue(SettingKeys.CONTEXT_DATASOURCE_UPLOAD_LIMITS,
                        SettingKeys.KEY_VIDEO_UPLOAD_MAX_MB), DEFAULT_VIDEO_MB),
                parseMb(settings.getValue(SettingKeys.CONTEXT_DATASOURCE_UPLOAD_LIMITS,
                        SettingKeys.KEY_AUDIO_UPLOAD_MAX_MB), DEFAULT_AUDIO_MB),
                parseMb(settings.getValue(SettingKeys.CONTEXT_DATASOURCE_UPLOAD_LIMITS,
                        SettingKeys.KEY_BINARY_UPLOAD_MAX_MB), DEFAULT_BINARY_MB));
    }

    public int maxMb(ResourceType type) {
        if (type == ResourceType.VIDEO) {
            return videoMb;
        }
        if (type == ResourceType.AUDIO) {
            return audioMb;
        }
        if (type == ResourceType.IMAGE) {
            return imageMb;
        }
        return binaryMb;
    }

    public long maxBytes(ResourceType type) {
        return maxMb(type) * 1024L * 1024L;
    }

    public String tooLargeMessage(ResourceType type) {
        return "File is too large (" + maxMb(type) + " MB max)";
    }

    public int imageMb() {
        return imageMb;
    }

    public int videoMb() {
        return videoMb;
    }

    public int audioMb() {
        return audioMb;
    }

    public int binaryMb() {
        return binaryMb;
    }

    private static int parseMb(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < MIN_MB) {
                return MIN_MB;
            }
            if (MAX_MB > 0 && value > MAX_MB) {
                return MAX_MB;
            }
            return value;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}

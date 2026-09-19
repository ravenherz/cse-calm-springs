package com.ravenherz.cse.util.video;

import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.util.Settings;
import com.ravenherz.cse.util.imaging.JpegImages;

public final class VideoUploadOptions {

    public static final int DEFAULT_MAX_WIDTH = 1280;
    public static final int DEFAULT_MAX_HEIGHT = 720;
    public static final float DEFAULT_QUALITY_FACTOR = 0.95f;
    public static final String DEFAULT_AUDIO_BITRATE = "128k";

    private final int maxWidth;
    private final int maxHeight;
    private final float qualityFactor;
    private final String audioBitrate;

    private VideoUploadOptions(int maxWidth, int maxHeight, float qualityFactor, String audioBitrate) {
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.qualityFactor = qualityFactor;
        this.audioBitrate = audioBitrate;
    }

    public static VideoUploadOptions defaults() {
        return new VideoUploadOptions(DEFAULT_MAX_WIDTH, DEFAULT_MAX_HEIGHT, DEFAULT_QUALITY_FACTOR,
                DEFAULT_AUDIO_BITRATE);
    }

    public static VideoUploadOptions from(Settings settings) {
        if (settings == null) {
            return defaults();
        }
        int width = even(clamp(parseInt(settings.getValue(SettingKeys.CONTEXT_DATASOURCE_VIDEO_UPLOAD,
                SettingKeys.KEY_MAX_WIDTH), DEFAULT_MAX_WIDTH), 16, 7680));
        int height = even(clamp(parseInt(settings.getValue(SettingKeys.CONTEXT_DATASOURCE_VIDEO_UPLOAD,
                SettingKeys.KEY_MAX_HEIGHT), DEFAULT_MAX_HEIGHT), 16, 4320));
        float quality = JpegImages.clampQuality(parseFloat(settings.getValue(
                SettingKeys.CONTEXT_DATASOURCE_VIDEO_UPLOAD, SettingKeys.KEY_QUALITY_FACTOR),
                DEFAULT_QUALITY_FACTOR));
        String audio = audioBitrate(settings.getValue(SettingKeys.CONTEXT_DATASOURCE_VIDEO_UPLOAD,
                SettingKeys.KEY_AUDIO_BITRATE));
        return new VideoUploadOptions(width, height, quality, audio);
    }

    public int maxWidth() {
        return maxWidth;
    }

    public int maxHeight() {
        return maxHeight;
    }

    public float qualityFactor() {
        return qualityFactor;
    }

    public String audioBitrate() {
        return audioBitrate;
    }

    public String videoBitrate() {
        int kbps = Math.round(400 + qualityFactor * 3600);
        return kbps + "k";
    }

    public String scaleFilter() {
        return "scale='min(iw," + maxWidth + ")':'min(ih," + maxHeight
                + ")':force_original_aspect_ratio=decrease:force_divisible_by=2";
    }

    private static String audioBitrate(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_AUDIO_BITRATE;
        }
        String value = raw.trim().toLowerCase();
        if (value.matches("[0-9]+")) {
            return value + "k";
        }
        if (value.matches("[0-9]+k")) {
            return value;
        }
        return DEFAULT_AUDIO_BITRATE;
    }

    private static int parseInt(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static float parseFloat(String raw, float fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Float.parseFloat(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int even(int value) {
        return value - (value % 2);
    }
}

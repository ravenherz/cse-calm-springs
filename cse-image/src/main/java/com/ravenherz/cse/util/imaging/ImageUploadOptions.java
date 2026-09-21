package com.ravenherz.cse.util.imaging;

import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.dal.ConfigSource;

public final class ImageUploadOptions {

    private static final int DEFAULT_PREVIEW_MAX_WIDTH = 700;
    private static final float DEFAULT_QUALITY_FACTOR = 0.95f;

    private final int previewMaxWidth;
    private final float qualityFactor;

    private ImageUploadOptions(int previewMaxWidth, float qualityFactor) {
        this.previewMaxWidth = previewMaxWidth;
        this.qualityFactor = qualityFactor;
    }

    public static ImageUploadOptions from(ConfigSource settings) {
        int maxWidth = DEFAULT_PREVIEW_MAX_WIDTH;
        float quality = DEFAULT_QUALITY_FACTOR;
        if (settings != null) {
            maxWidth = parseInt(settings.getValue(SettingKeys.CONTEXT_DATASOURCE_IMAGE_UPLOAD,
                    SettingKeys.KEY_PREVIEW_MAX_WIDTH), DEFAULT_PREVIEW_MAX_WIDTH);
            quality = parseQuality(settings.getValue(SettingKeys.CONTEXT_DATASOURCE_IMAGE_UPLOAD,
                    SettingKeys.KEY_QUALITY_FACTOR), DEFAULT_QUALITY_FACTOR);
        }
        return new ImageUploadOptions(Math.max(0, maxWidth), JpegImages.clampQuality(quality));
    }

    public int previewMaxWidth() {
        return previewMaxWidth;
    }

    public float qualityFactor() {
        return qualityFactor;
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

    private static float parseQuality(String raw, float fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Float.parseFloat(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}

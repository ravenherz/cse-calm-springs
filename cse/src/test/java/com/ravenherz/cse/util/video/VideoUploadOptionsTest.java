package com.ravenherz.cse.util.video;

import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.util.Settings;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VideoUploadOptionsTest {

    @Test
    void defaultsMatch720pAndHighQuality() {
        VideoUploadOptions options = VideoUploadOptions.defaults();
        assertEquals(1280, options.maxWidth());
        assertEquals(720, options.maxHeight());
        assertEquals(0.95f, options.qualityFactor());
        assertEquals("128k", options.audioBitrate());
        assertEquals("3820k", options.videoBitrate());
        assertTrue(options.scaleFilter().contains("1280"));
        assertTrue(options.scaleFilter().contains("720"));
    }

    @Test
    void fromSettingsReadsVideoUploadContext() {
        Settings settings = settings(
                SettingKeys.KEY_MAX_WIDTH, "1920",
                SettingKeys.KEY_MAX_HEIGHT, "1081",
                SettingKeys.KEY_QUALITY_FACTOR, "0.5",
                SettingKeys.KEY_AUDIO_BITRATE, "96");
        VideoUploadOptions options = VideoUploadOptions.from(settings);
        assertEquals(1920, options.maxWidth());
        assertEquals(1080, options.maxHeight());
        assertEquals(0.5f, options.qualityFactor());
        assertEquals("96k", options.audioBitrate());
        assertEquals("2200k", options.videoBitrate());
    }

    @Test
    void nullSettingsUseDefaults() {
        assertEquals(1280, VideoUploadOptions.from(null).maxWidth());
    }

    private static Settings settings(String... pairs) {
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            values.put(pairs[i], pairs[i + 1]);
        }
        return new Settings() {
            @Override
            public String getValue(String context, String key) {
                if (!SettingKeys.CONTEXT_DATASOURCE_VIDEO_UPLOAD.equals(context)) {
                    return null;
                }
                return values.get(key);
            }
        };
    }
}

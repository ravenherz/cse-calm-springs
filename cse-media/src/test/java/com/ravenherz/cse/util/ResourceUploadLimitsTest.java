package com.ravenherz.cse.util;

import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.dal.ConfigSource;
import com.ravenherz.cse.dal.dto.basic.enums.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceUploadLimitsTest {

    @Test
    void defaultsMatchPreviousHardLimits() {
        ResourceUploadLimits limits = ResourceUploadLimits.defaults();
        assertEquals(50, limits.maxMb(ResourceType.IMAGE));
        assertEquals(200, limits.maxMb(ResourceType.VIDEO));
        assertEquals(50, limits.maxMb(ResourceType.AUDIO));
        assertEquals(50, limits.maxMb(ResourceType.BINARY));
        assertEquals(50, limits.binaryMb());
        assertEquals(50L * 1024 * 1024, limits.maxBytes(ResourceType.IMAGE));
        assertEquals(200L * 1024 * 1024, limits.maxBytes(ResourceType.VIDEO));
        assertEquals("File is too large (200 MB max)", limits.tooLargeMessage(ResourceType.VIDEO));
        assertEquals(-1, ResourceUploadLimits.MAX_MB);
        assertEquals(-1, ResourceUploadLimits.servletCeilingBytes());
    }

    @Test
    void fromSettingsReadsUploadLimitsContext() {
        ConfigSource settings = settings(
                SettingKeys.KEY_IMAGE_UPLOAD_MAX_MB, "12",
                SettingKeys.KEY_VIDEO_UPLOAD_MAX_MB, "80",
                SettingKeys.KEY_AUDIO_UPLOAD_MAX_MB, "25",
                SettingKeys.KEY_BINARY_UPLOAD_MAX_MB, "40");
        ResourceUploadLimits limits = ResourceUploadLimits.from(settings);
        assertEquals(12, limits.imageMb());
        assertEquals(80, limits.videoMb());
        assertEquals(25, limits.audioMb());
        assertEquals(40, limits.binaryMb());
        assertEquals(40, limits.maxMb(ResourceType.INVALID));
    }

    @Test
    void clampsAndFallsBack() {
        ConfigSource settings = settings(
                SettingKeys.KEY_IMAGE_UPLOAD_MAX_MB, "0",
                SettingKeys.KEY_VIDEO_UPLOAD_MAX_MB, "99999",
                SettingKeys.KEY_AUDIO_UPLOAD_MAX_MB, "nope");
        ResourceUploadLimits limits = ResourceUploadLimits.from(settings);
        assertEquals(1, limits.imageMb());
        assertEquals(99999, limits.videoMb());
        assertEquals(50, limits.audioMb());
        assertEquals(50, ResourceUploadLimits.from(null).imageMb());
    }

    private static ConfigSource settings(String... pairs) {
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            values.put(pairs[i], pairs[i + 1]);
        }
        return (context, key) -> {
            if (!SettingKeys.CONTEXT_DATASOURCE_UPLOAD_LIMITS.equals(context)) {
                return null;
            }
            return values.get(key);
        };
    }
}

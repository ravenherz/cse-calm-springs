package com.ravenherz.cse.util.video;

import org.junit.jupiter.api.Test;

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
    void fromRawValuesReadsWidthHeightQualityAndAudio() {
        VideoUploadOptions options = VideoUploadOptions.from("1920", "1081", "0.5", "96");
        assertEquals(1920, options.maxWidth());
        assertEquals(1080, options.maxHeight());
        assertEquals(0.5f, options.qualityFactor());
        assertEquals("96k", options.audioBitrate());
        assertEquals("2200k", options.videoBitrate());
    }

    @Test
    void blankValuesUseDefaults() {
        assertEquals(1280, VideoUploadOptions.from(null, null, null, null).maxWidth());
    }
}

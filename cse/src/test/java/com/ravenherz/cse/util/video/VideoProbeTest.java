package com.ravenherz.cse.util.video;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VideoProbeTest {

    @Test
    void readsVideoStreamAndDuration() throws Exception {
        VideoProbe.Info info = VideoProbe.parse("""
                {
                  "streams": [
                    {"codec_type": "audio", "duration": "3.0"},
                    {"codec_type": "video", "width": 1920, "height": 1080, "duration": "12.4"}
                  ],
                  "format": {"duration": "12.41"}
                }
                """);
        assertEquals(1920, info.width());
        assertEquals(1080, info.height());
        assertEquals(12.4, info.durationSeconds());
        assertFalse(info.fits720p());
    }

    @Test
    void treats720pBoxAsFit() throws Exception {
        VideoProbe.Info info = VideoProbe.parse("""
                {"streams":[{"codec_type":"video","width":1280,"height":720}]}
                """);
        assertTrue(info.fits720p());
    }

    @Test
    void rejectsMissingVideoStream() {
        assertThrows(IOException.class, () -> VideoProbe.parse("""
                {"streams":[{"codec_type":"audio"}]}
                """));
    }
}

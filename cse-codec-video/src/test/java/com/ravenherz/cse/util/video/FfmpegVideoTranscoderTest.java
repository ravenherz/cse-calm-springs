package com.ravenherz.cse.util.video;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FfmpegVideoTranscoderTest {

    @Test
    void encodeTimeoutScalesWithDuration() {
        assertEquals(Duration.ofHours(2), FfmpegVideoTranscoder.encodeTimeout(null));
        assertEquals(Duration.ofMinutes(45), FfmpegVideoTranscoder.encodeTimeout(10.0));
        assertEquals(Duration.ofMinutes(175), FfmpegVideoTranscoder.encodeTimeout(20 * 60.0));
        assertEquals(Duration.ofMinutes(240), FfmpegVideoTranscoder.encodeTimeout(10_000.0));
    }

    @Test
    void quietEnvironmentDropsDisplayAndDisablesPulse() {
        ProcessBuilder builder = new ProcessBuilder("ffmpeg");
        builder.environment().put("DISPLAY", ":0");
        builder.environment().put("WAYLAND_DISPLAY", "wayland-0");
        FfmpegProcess.quietEnvironment(builder);
        assertNull(builder.environment().get("DISPLAY"));
        assertNull(builder.environment().get("WAYLAND_DISPLAY"));
        assertEquals("none", builder.environment().get("PULSE_SERVER"));
        assertEquals("dummy", builder.environment().get("SDL_VIDEODRIVER"));
    }

    @Test
    void linuxStubIsElf64SharedObject() {
        byte[] elf = LinuxSharedStubs.elf("libxcb-shm.so.0");
        assertEquals(0x7f, elf[0] & 0xff);
        assertEquals('E', elf[1]);
        assertEquals('L', elf[2]);
        assertEquals('F', elf[3]);
        assertEquals(2, elf[4]);
        assertTrue(new String(elf, java.nio.charset.StandardCharsets.US_ASCII).contains("libxcb-shm.so.0"));
    }

    @Test
    void platformMatchesBytedecoLayout() {
        String platform = FfmpegBinaries.platform();
        assertTrue(platform.contains("-"));
        assertTrue(platform.startsWith("windows") || platform.startsWith("linux") || platform.startsWith("macosx"));
    }
}

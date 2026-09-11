package com.ravenherz.cse.util.video;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FfmpegProgressTest {

    @Test
    void percentFromOutTimeAndEnd() {
        assertEquals(50, FfmpegProgress.percent("out_time=00:00:05.000000", 10.0, 0));
        assertEquals(50, FfmpegProgress.percent("out_time_us=5000000", 10.0, 40));
        assertEquals(100, FfmpegProgress.percent("progress=end", 10.0, 90));
    }

    @Test
    void outTimeMsIsMicroseconds() {
        assertEquals(5, FfmpegProgress.percent("out_time_ms=500000", 10.0, 0));
        assertEquals(50, FfmpegProgress.percent("out_time_ms=5000000", 10.0, 0));
        assertEquals(4, FfmpegProgress.percent("out_time_ms=2500000", 60.0, 0));
    }

    @Test
    void ffmpegProgressBlockDoesNotJumpOnOutTimeMs() {
        FfmpegProgress progress = new FfmpegProgress(60.0);
        assertEquals(4, progress.accept("out_time_us=2500000"));
        assertEquals(4, progress.accept("out_time_ms=2500000"));
        assertEquals(4, progress.accept("out_time=00:00:02.500000"));
        assertEquals(4, progress.accept("progress=continue"));
    }

    @Test
    void doesNotJumpBackwardOrPast99UntilEnd() {
        assertEquals(40, FfmpegProgress.percent("out_time=00:00:01.000000", 10.0, 40));
        assertEquals(99, FfmpegProgress.percent("out_time=00:00:12.000000", 10.0, 0));
    }

    @Test
    void percentFromStatsTime() {
        assertEquals(25, FfmpegProgress.percent(
                "frame=  12 fps= 30 q=28.0 size=     256KiB time=00:00:02.50 bitrate= 800.0kbits/s",
                10.0, 0));
        assertEquals(50, FfmpegProgress.percent("time=00:00:05.00", 10.0, 0));
    }

    @Test
    void durationFromBanner() {
        assertEquals(83.45, FfmpegProgress.durationFromBanner(
                "  Duration: 00:01:23.45, start: 0.000000, bitrate: 800 kb/s"), 0.01);
    }

    @Test
    void acceptUsesBannerWhenDurationUnknown() {
        FfmpegProgress progress = new FfmpegProgress(null);
        progress.accept("  Duration: 00:00:10.00, start: 0.000000, bitrate: N/A");
        assertEquals(50, progress.accept("out_time=00:00:05.000000"));
        assertEquals(50, progress.accept("frame=  12 fps=  0"));
    }

    @Test
    void missingDurationKeepsCurrent() {
        assertEquals(7, FfmpegProgress.percent("out_time=00:00:05.000000", null, 7));
        assertNull(FfmpegProgress.micros("frame=12"));
    }
}

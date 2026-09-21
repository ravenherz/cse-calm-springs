package com.ravenherz.cse.util.video;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FfmpegVideoTranscoderNativeTest {

    @TempDir
    Path temp;

    @Test
    @EnabledIf("ffmpegLoaded")
    void recodesTinyClipToMp4() throws Exception {
        Path source = temp.resolve("src.mp4");
        FfmpegProcess.run(List.of(
                FfmpegBinaries.ffmpeg(),
                "-y",
                "-nostdin",
                "-f", "lavfi",
                "-i", "color=c=red:s=320x240:d=2",
                "-pix_fmt", "yuv420p",
                source.toAbsolutePath().toString()), Duration.ofSeconds(20));
        Path out = temp.resolve("out.mp4");
        java.util.concurrent.atomic.AtomicInteger mid = new java.util.concurrent.atomic.AtomicInteger();
        new FfmpegVideoTranscoder().recode(source, out, 2.0, percent -> {
            if (percent > 0 && percent < 100) {
                mid.accumulateAndGet(percent, Math::max);
            }
        });
        assertTrue(Files.size(out) > 0);
        assertTrue(mid.get() >= 5, "encode progress should move, last mid=" + mid.get());
        VideoProbe.Info info = VideoProbe.probe(out);
        assertTrue(info.width() <= 1280);
        assertTrue(info.height() <= 720);
        assertTrue(info.width() <= 320);
        assertTrue(info.height() <= 240);
    }

    @Test
    @EnabledIf("ffmpegLoaded")
    void progressLinesArriveOnStderrPipe() throws Exception {
        Path out = temp.resolve("prog.mp4");
        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        FfmpegProcess.run(List.of(
                FfmpegBinaries.ffmpeg(),
                "-hide_banner",
                "-y",
                "-nostdin",
                "-f", "lavfi",
                "-i", "color=c=red:s=320x240:d=2",
                "-c:v", "libopenh264",
                "-pix_fmt", "yuv420p",
                "-an",
                "-progress", "pipe:2",
                out.toAbsolutePath().toString()), Duration.ofSeconds(30), lines::add);
        assertTrue(Files.size(out) > 0);
        assertTrue(lines.stream().anyMatch(line -> line.startsWith("out_time") || line.startsWith("progress=")),
                String.join("\n", lines));
    }

    static boolean ffmpegLoaded() {
        return FfmpegBinaries.available();
    }
}

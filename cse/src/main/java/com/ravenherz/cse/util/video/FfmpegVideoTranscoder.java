package com.ravenherz.cse.util.video;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public final class FfmpegVideoTranscoder implements VideoTranscoder {

    @Override
    public void recode(Path input, Path output) throws IOException {
        recode(input, output, null, null, VideoUploadOptions.defaults());
    }

    @Override
    public void recode(Path input, Path output, Double durationSeconds, IntConsumer percent,
            VideoUploadOptions options) throws IOException {
        if (input == null || !Files.isRegularFile(input)) {
            throw new IOException("Video source is missing");
        }
        if (output == null) {
            throw new IOException("Video output is missing");
        }
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        FfmpegProgress progress = new FfmpegProgress(durationSeconds);
        IntConsumer sink = percent == null ? value -> {
        } : percent;
        VideoUploadOptions tuned = options == null ? VideoUploadOptions.defaults() : options;
        List<String> command = new ArrayList<>();
        command.add(FfmpegBinaries.ffmpeg());
        command.add("-hide_banner");
        command.add("-y");
        command.add("-nostdin");
        command.add("-hwaccel");
        command.add("none");
        command.add("-i");
        command.add(input.toAbsolutePath().toString());
        command.add("-vf");
        command.add(tuned.scaleFilter());
        command.add("-c:v");
        command.add("libopenh264");
        command.add("-b:v");
        command.add(tuned.videoBitrate());
        command.add("-pix_fmt");
        command.add("yuv420p");
        command.add("-map");
        command.add("0:v:0");
        command.add("-map");
        command.add("0:a:0?");
        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add(tuned.audioBitrate());
        command.add("-movflags");
        command.add("+faststart");
        command.add("-stats_period");
        command.add("0.5");
        command.add("-progress");
        command.add("pipe:2");
        command.add(output.toAbsolutePath().toString());
        int[] last = { -1 };
        FfmpegProcess.run(command, encodeTimeout(durationSeconds), line -> {
            int value = progress.accept(line);
            if (value != last[0]) {
                last[0] = value;
                sink.accept(value);
            }
        });
        if (!Files.isRegularFile(output) || Files.size(output) <= 0) {
            throw new IOException("FFmpeg produced an empty MP4");
        }
        sink.accept(100);
    }

    @Override
    public void poster(Path input, Path output) throws IOException {
        if (input == null || !Files.isRegularFile(input)) {
            throw new IOException("Video source is missing");
        }
        if (output == null) {
            throw new IOException("Poster output is missing");
        }
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        try {
            grab(input, output, "0.5");
        } catch (IOException first) {
            grab(input, output, "0");
        }
        if (!Files.isRegularFile(output) || Files.size(output) <= 0) {
            throw new IOException("FFmpeg produced an empty poster");
        }
    }

    private static void grab(Path input, Path output, String seconds) throws IOException {
        FfmpegProcess.run(List.of(
                FfmpegBinaries.ffmpeg(),
                "-hide_banner",
                "-y",
                "-nostdin",
                "-hwaccel", "none",
                "-ss", seconds,
                "-i", input.toAbsolutePath().toString(),
                "-frames:v", "1",
                "-q:v", "3",
                output.toAbsolutePath().toString()), Duration.ofSeconds(30));
    }

    static Duration encodeTimeout(Double durationSeconds) {
        if (durationSeconds == null || !Double.isFinite(durationSeconds) || durationSeconds <= 0) {
            return Duration.ofHours(2);
        }
        long minutes = Math.round(durationSeconds / 60.0d * 8.0d) + 15;
        minutes = Math.max(45, Math.min(240, minutes));
        return Duration.ofMinutes(minutes);
    }
}

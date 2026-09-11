package com.ravenherz.cse.util.video;

import com.fasterxml.jackson.databind.JsonNode;
import com.ravenherz.cse.util.Json;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public final class VideoProbe {

    public record Info(int width, int height, Double durationSeconds) {
        public boolean fits720p() {
            return width > 0 && height > 0 && width <= 1280 && height <= 720;
        }
    }

    private VideoProbe() {
    }

    public static Info probe(Path file) throws IOException {
        if (file == null) {
            throw new IOException("Video file is missing");
        }
        String json = FfmpegProcess.stdout(List.of(
                FfmpegBinaries.ffprobe(),
                "-v", "error",
                "-hide_banner",
                "-print_format", "json",
                "-show_streams",
                "-show_format",
                file.toAbsolutePath().toString()), Duration.ofSeconds(45));
        return parse(json);
    }

    public static Info parse(String json) throws IOException {
        if (json == null || json.isBlank()) {
            throw new IOException("ffprobe returned no JSON");
        }
        JsonNode root = Json.MAPPER.readTree(json);
        int width = 0;
        int height = 0;
        Double duration = null;
        JsonNode streams = root.get("streams");
        if (streams != null && streams.isArray()) {
            for (JsonNode stream : streams) {
                if (stream == null || !"video".equals(text(stream, "codec_type"))) {
                    continue;
                }
                width = intValue(stream, "width");
                height = intValue(stream, "height");
                duration = firstDuration(duration, number(stream, "duration"));
                if (width > 0 && height > 0) {
                    break;
                }
            }
        }
        JsonNode format = root.get("format");
        if (format != null) {
            duration = firstDuration(duration, number(format, "duration"));
        }
        if (width <= 0 || height <= 0) {
            throw new IOException("ffprobe found no video stream");
        }
        return new Info(width, height, duration);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }

    private static int intValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || !value.canConvertToInt() ? 0 : value.asInt(0);
    }

    private static Double number(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.asDouble();
        }
        try {
            return Double.parseDouble(value.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double firstDuration(Double current, Double next) {
        if (current != null && current > 0) {
            return current;
        }
        return next != null && next > 0 ? next : current;
    }
}

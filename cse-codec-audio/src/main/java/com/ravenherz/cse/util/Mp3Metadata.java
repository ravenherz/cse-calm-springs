package com.ravenherz.cse.util;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Mp3Metadata {

    private static final Logger LOGGER = LoggerFactory.getLogger(Mp3Metadata.class);
    private static final Pattern TRACK_NUMBER = Pattern.compile("(\\d+)");
    private static final List<String> FIELD_ORDER = List.of(
            "title", "artist", "album", "albumArtist", "trackNumber", "trackTotal",
            "discNo", "discTotal", "year", "originalYear", "genre", "composer",
            "lyricist", "lyrics", "comment", "grouping", "bpm", "key", "isrc",
            "language", "copyright", "publisher", "encoder");

    public record Parsed(Map<String, String> fields, Integer durationSeconds, Integer bitrateKbps,
            byte[] artworkBytes) {
        public Parsed {
            fields = fields == null || fields.isEmpty()
                    ? Map.of()
                    : Collections.unmodifiableMap(new LinkedHashMap<>(fields));
        }
    }

    private Mp3Metadata() {
    }

    public static Parsed parse(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return empty();
        }
        Path temp = null;
        try {
            temp = Files.createTempFile("cse-mp3-", ".mp3");
            Files.write(temp, bytes);
            AudioFile audioFile = AudioFileIO.read(temp.toFile());
            Tag tag = audioFile.getTag();
            AudioHeader header = audioFile.getAudioHeader();
            Integer duration = null;
            Integer bitrate = null;
            if (header != null) {
                if (header.getTrackLength() > 0) {
                    duration = header.getTrackLength();
                }
                long kbps = header.getBitRateAsNumber();
                if (kbps > 0) {
                    bitrate = (int) kbps;
                }
            }
            return new Parsed(readFields(tag), duration, bitrate, artworkBytes(tag));
        } catch (Exception e) {
            LOGGER.warn("Could not read MP3 tags: {}", e.getMessage());
            return empty();
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (Exception e) {
                    LOGGER.debug("Could not delete temp MP3: {}", e.getMessage());
                }
            }
        }
    }

    public static Map<String, String> merge(Map<String, String> existing, Parsed parsed) {
        Map<String, String> fields = existing == null ? new java.util.LinkedHashMap<>() : new java.util.LinkedHashMap<>(existing);
        if (parsed == null) {
            return fields;
        }
        for (Map.Entry<String, String> entry : parsed.fields().entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                fields.put(entry.getKey(), entry.getValue());
            }
        }
        if (parsed.durationSeconds() != null && parsed.durationSeconds() > 0) {
            fields.put("duration", String.valueOf(parsed.durationSeconds()));
        }
        if (parsed.bitrateKbps() != null && parsed.bitrateKbps() > 0) {
            fields.put("bitrate", String.valueOf(parsed.bitrateKbps()));
        }
        return fields;
    }

    public static String metadataKey(FieldKey key) {
        if (key == null) {
            return null;
        }
        if (key == FieldKey.TRACK) {
            return "trackNumber";
        }
        String[] parts = key.name().split("_");
        StringBuilder keyName = new StringBuilder(parts[0].toLowerCase());
        for (int i = 1; i < parts.length; i++) {
            keyName.append(Character.toUpperCase(parts[i].charAt(0)));
            if (parts[i].length() > 1) {
                keyName.append(parts[i].substring(1).toLowerCase());
            }
        }
        return keyName.toString();
    }

    public static String albumLine(Map<String, String> metadata) {
        String album = value(metadata, "album");
        String year = value(metadata, "year");
        String trackNumber = value(metadata, "trackNumber");
        if (album == null && year == null && trackNumber == null) {
            return null;
        }
        StringBuilder line = new StringBuilder();
        if (album != null) {
            line.append(album);
        }
        if (year != null) {
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(year);
        }
        if (trackNumber != null) {
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append('#').append(trackNumber);
        }
        return line.toString();
    }

    public static String parseTrackNumber(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Matcher matcher = TRACK_NUMBER.matcher(raw.trim());
        if (!matcher.find()) {
            return null;
        }
        try {
            return String.valueOf(Integer.parseInt(matcher.group(1)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String value(Map<String, String> metadata, String key) {
        if (metadata == null || key == null) {
            return null;
        }
        String value = metadata.get(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static Parsed empty() {
        return new Parsed(Map.of(), null, null, null);
    }

    private static Map<String, String> readFields(Tag tag) {
        Map<String, String> raw = new LinkedHashMap<>();
        if (tag == null) {
            return raw;
        }
        for (FieldKey key : FieldKey.values()) {
            if (key == FieldKey.COVER_ART) {
                continue;
            }
            String name = metadataKey(key);
            if (name == null || raw.containsKey(name)) {
                continue;
            }
            String value = key == FieldKey.TRACK
                    ? parseTrackNumber(first(tag, key))
                    : joined(tag, key);
            if (value != null) {
                raw.put(name, value);
            }
        }
        Map<String, String> ordered = new LinkedHashMap<>();
        for (String name : FIELD_ORDER) {
            String value = raw.remove(name);
            if (value != null) {
                ordered.put(name, value);
            }
        }
        List<String> rest = new ArrayList<>(raw.keySet());
        Collections.sort(rest);
        for (String name : rest) {
            ordered.put(name, raw.get(name));
        }
        return ordered;
    }

    private static String joined(Tag tag, FieldKey key) {
        try {
            List<String> values = tag.getAll(key);
            if (values == null || values.isEmpty()) {
                return null;
            }
            String joined = values.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .distinct()
                    .collect(Collectors.joining("; "));
            return joined.isEmpty() ? null : joined;
        } catch (Exception e) {
            return first(tag, key);
        }
    }

    private static byte[] artworkBytes(Tag tag) {
        if (tag == null) {
            return null;
        }
        try {
            Artwork artwork = tag.getFirstArtwork();
            if (artwork == null) {
                return null;
            }
            byte[] data = artwork.getBinaryData();
            if (data == null || data.length == 0) {
                return null;
            }
            return data;
        } catch (Exception e) {
            LOGGER.debug("Could not read MP3 artwork: {}", e.getMessage());
            return null;
        }
    }

    private static String first(Tag tag, FieldKey key) {
        if (tag == null) {
            return null;
        }
        try {
            String value = tag.getFirst(key);
            if (value == null || value.isBlank()) {
                return null;
            }
            return value.trim();
        } catch (Exception e) {
            return null;
        }
    }
}

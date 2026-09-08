package com.ravenherz.cse.util;

import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.basic.PlaylistTrack;
import com.ravenherz.cse.dal.dto.basic.ResourceData;

import java.util.Map;

public final class PlaylistTracks {

    private PlaylistTracks() {
    }

    public static String title(PlaylistTrack track) {
        if (track == null) {
            return "Unknown";
        }
        if (track.getTitle() != null && !track.getTitle().isBlank()) {
            return track.getTitle().trim();
        }
        String fromMeta = metadata(track, "title");
        if (fromMeta != null) {
            return fromMeta;
        }
        String fileName = fileName(track);
        return fileName.isBlank() ? "Unknown" : fileName;
    }

    public static String artist(PlaylistTrack track) {
        if (track == null) {
            return "";
        }
        if (track.getArtist() != null && !track.getArtist().isBlank()) {
            return track.getArtist().trim();
        }
        String fromMeta = metadata(track, "artist");
        return fromMeta == null ? "" : fromMeta;
    }

    public static String trackNumber(PlaylistTrack track, int index) {
        String fromMeta = metadata(track, "trackNumber");
        if (fromMeta != null) {
            return fromMeta;
        }
        return String.valueOf(index + 1);
    }

    public static String durationLabel(PlaylistTrack track) {
        return formatDuration(metadata(track, "duration"));
    }

    public static String src(PlaylistTrack track) {
        ResourceData data = resourceData(track);
        if (data == null || data.getPathPublic() == null || data.getPathPublic().isBlank()) {
            return "";
        }
        return "./content-protected" + data.getPathPublic();
    }

    public static String waveformBase64(PlaylistTrack track) {
        ResourceData data = resourceData(track);
        if (data == null) {
            return "";
        }
        String encoded = data.getWaveformBase64();
        return encoded == null ? "" : encoded;
    }

    public static String formatDuration(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String value = raw.trim();
        if (value.contains(":")) {
            return value;
        }
        try {
            int seconds = Integer.parseInt(value);
            if (seconds < 0) {
                return "";
            }
            return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    public static String metadata(ResourceEntity resource, String key) {
        if (resource == null || resource.getResourceData() == null || key == null) {
            return null;
        }
        Map<String, String> metadata = resource.getResourceData().getMetadata();
        if (metadata == null) {
            return null;
        }
        String value = metadata.get(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String metadata(PlaylistTrack track, String key) {
        return metadata(track == null ? null : track.getRefResource(), key);
    }

    private static ResourceData resourceData(PlaylistTrack track) {
        if (track == null || track.getRefResource() == null) {
            return null;
        }
        return track.getRefResource().getResourceData();
    }

    private static String fileName(PlaylistTrack track) {
        ResourceData data = resourceData(track);
        return data == null ? "" : data.getFileName();
    }
}

package com.ravenherz.cse.util;

import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.basic.PlaylistTrack;
import com.ravenherz.cse.dal.dto.basic.ResourceData;

import java.util.Map;

public final class PlaylistTracks {

    private PlaylistTracks() {
    }

    public static String title(PlaylistTrack track, ResourceEntity resource) {
        if (track == null && resource == null) {
            return "Unknown";
        }
        if (track != null && track.getTitle() != null && !track.getTitle().isBlank()) {
            return track.getTitle().trim();
        }
        String fromMeta = metadata(resource, "title");
        if (fromMeta != null) {
            return fromMeta;
        }
        String fileName = fileName(resource);
        return fileName.isBlank() ? "Unknown" : fileName;
    }

    public static String artist(PlaylistTrack track, ResourceEntity resource) {
        if (track != null && track.getArtist() != null && !track.getArtist().isBlank()) {
            return track.getArtist().trim();
        }
        String fromMeta = metadata(resource, "artist");
        return fromMeta == null ? "" : fromMeta;
    }

    public static String trackNumber(PlaylistTrack track, ResourceEntity resource, int index) {
        String fromMeta = metadata(resource, "trackNumber");
        if (fromMeta != null) {
            return fromMeta;
        }
        return String.valueOf(index + 1);
    }

    public static String durationLabel(PlaylistTrack track, ResourceEntity resource) {
        return formatDuration(metadata(resource, "duration"));
    }

    public static String src(PlaylistTrack track, ResourceEntity resource) {
        ResourceData data = resource == null ? null : resource.getResourceData();
        if (data == null || data.getPathPublic() == null || data.getPathPublic().isBlank()) {
            return "";
        }
        return "./content-protected" + data.getPathPublic();
    }

    public static String waveformBase64(PlaylistTrack track, ResourceEntity resource) {
        ResourceData data = resource == null ? null : resource.getResourceData();
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

    private static String fileName(ResourceEntity resource) {
        ResourceData data = resource == null ? null : resource.getResourceData();
        return data == null ? "" : data.getFileName();
    }
}

package com.ravenherz.cse.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class PlaylistIds {

    public static final Pattern PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]*$");

    private PlaylistIds() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isValid(String playlistId) {
        return playlistId != null && !playlistId.isBlank() && PATTERN.matcher(playlistId).matches();
    }
}

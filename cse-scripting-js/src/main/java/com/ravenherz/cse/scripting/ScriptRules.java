package com.ravenherz.cse.scripting;

import java.util.Locale;
import java.util.regex.Pattern;

public final class ScriptRules {

    public static final int MAX_SOURCE = 200_000;

    private static final Pattern ID = Pattern.compile("^[a-z][a-z0-9-]{0,62}$");
    private static final Pattern SEGMENT = Pattern.compile("^[a-z0-9][a-z0-9-]{0,62}$");

    private ScriptRules() {
    }

    public static String idError(String raw) {
        String id = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (!ID.matcher(id).matches()) {
            return "Use a lowercase id that starts with a letter";
        }
        return null;
    }

    public static String normalizeId(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    public static String folderError(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        if (normalizeFolder(raw) == null) {
            return "Use a folder like hooks/pages";
        }
        return null;
    }

    /**
     * @return {@code ""} when blank, the cleaned path when valid, or {@code null} when the path is rejected
     */
    public static String normalizeFolder(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String value = raw.trim().toLowerCase(Locale.ROOT).replace('\\', '/');
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.isEmpty()) {
            return "";
        }
        if (value.contains("//") || value.contains("..") || value.indexOf(' ') >= 0 || value.indexOf('*') >= 0) {
            return null;
        }
        String[] parts = value.split("/");
        if (parts.length > 8) {
            return null;
        }
        for (String part : parts) {
            if (!SEGMENT.matcher(part).matches()) {
                return null;
            }
        }
        return value;
    }

    public static String sourceError(String source) {
        if (source != null && source.length() > MAX_SOURCE) {
            return "Script is too long";
        }
        return null;
    }
}

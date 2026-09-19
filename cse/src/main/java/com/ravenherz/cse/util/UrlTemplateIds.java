package com.ravenherz.cse.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class UrlTemplateIds {

    public static final Pattern PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]*$");

    private UrlTemplateIds() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isValid(String urlTemplateId) {
        return urlTemplateId != null && !urlTemplateId.isBlank() && PATTERN.matcher(urlTemplateId).matches();
    }
}

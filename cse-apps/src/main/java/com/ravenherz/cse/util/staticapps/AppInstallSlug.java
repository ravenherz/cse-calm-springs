package com.ravenherz.cse.util.staticapps;

import java.util.Locale;

/**
 * Install identity is the pack's slug prefix ({@code version.manifest}), not the zip filename.
 * An optional form slug overrides the manifest so an operator can park a pack under a different prefix.
 */
public final class AppInstallSlug {

    private AppInstallSlug() {
    }

    public static String resolve(String requested, AppManifest manifest) {
        String fromForm = normalize(requested);
        if (!fromForm.isEmpty()) {
            return fromForm;
        }
        if (manifest != null && manifest.getSlug() != null) {
            return normalize(manifest.getSlug());
        }
        return "";
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}

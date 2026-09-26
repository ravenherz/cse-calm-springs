package com.ravenherz.cse.core.route;

import java.util.List;

/**
 * Context-relative prefixes the public redirect lookup must not claim.
 * Save checks use the same list so a stored row cannot cover a real controller.
 */
public final class ReservedPaths {

    public static final List<String> PREFIXES = List.of(
            "/editor",
            "/apps",
            "/install",
            "/account",
            "/rest",
            "/app-data",
            "/content-private",
            "/content-public",
            "/error",
            "/robots.txt");

    private ReservedPaths() {
    }

    public static boolean isReserved(String contextRelativePath) {
        if (contextRelativePath == null || contextRelativePath.isBlank()) {
            return false;
        }
        for (String prefix : PREFIXES) {
            if (contextRelativePath.equals(prefix) || contextRelativePath.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }
}

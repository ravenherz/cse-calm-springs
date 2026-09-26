package com.ravenherz.cse.redirect;

import java.util.Optional;

final class RedirectPaths {

    private RedirectPaths() {
    }

    static Optional<String> normalize(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String value = raw.trim();
        if (value.isEmpty() || !value.startsWith("/") || value.startsWith("//")
                || value.indexOf('\\') >= 0 || value.contains("..") || value.contains("://")) {
            return Optional.empty();
        }
        int query = value.indexOf('?');
        String path = query < 0 ? value : value.substring(0, query);
        String rest = query < 0 ? "" : value.substring(query);
        if (path.length() > 1 && path.endsWith("/") && !path.endsWith("/*/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.contains("*") && !prefixWildcard(path)) {
            return Optional.empty();
        }
        if (path.isEmpty() || !path.startsWith("/")) {
            return Optional.empty();
        }
        return Optional.of(path + rest);
    }

    static boolean prefixWildcard(String path) {
        return path != null && (path.endsWith("/*/") || path.endsWith("/*"));
    }

    static String prefixBase(String path) {
        if (path.endsWith("/*/")) {
            return path.substring(0, path.length() - 3);
        }
        if (path.endsWith("/*")) {
            return path.substring(0, path.length() - 2);
        }
        return path;
    }
}

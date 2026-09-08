package com.ravenherz.cse.util.themes;

import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Serves exploded theme packs from {@code themes/{id}/} only. Hidden work
 * folders, reserved engine names, and path escapes stay dark.
 */
public final class ThemeDiskResources extends PathResourceResolver {

    private static final Pattern THEME_ID = Pattern.compile("^[a-z0-9][a-z0-9-]{0,62}$");
    private static final Set<String> BLOCKED_ROOTS = Set.of(
            "admin", "fragments", "index", "editor"
    );

    public static ThemeDiskResources isolated() {
        return new ThemeDiskResources();
    }

    public static boolean isIsolatedThemePath(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return false;
        }
        String normalized = resourcePath.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isEmpty() || normalized.contains("..")) {
            return false;
        }
        int slash = normalized.indexOf('/');
        String id = slash < 0 ? normalized : normalized.substring(0, slash);
        if (id.startsWith(".") || !THEME_ID.matcher(id).matches()) {
            return false;
        }
        return !BLOCKED_ROOTS.contains(id.toLowerCase(Locale.ROOT));
    }

    @Override
    protected Resource getResource(String resourcePath, Resource location) throws IOException {
        if (!isIsolatedThemePath(resourcePath)) {
            return null;
        }
        return super.getResource(resourcePath, location);
    }
}

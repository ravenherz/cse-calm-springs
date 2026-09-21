package com.ravenherz.cse.util.frontend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Frontend packs the WAR ships under {@code classpath:install/}.
 */
public final class ShippedPackCatalog {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShippedPackCatalog.class);
    private static final PathMatchingResourcePatternResolver RESOLVER =
            new PathMatchingResourcePatternResolver();

    public static final String THEMES_DIR = "install/themes";
    public static final String APPS_DIR = "install";

    private ShippedPackCatalog() {
    }

    public static List<Pack> themes() {
        return list(THEMES_DIR, "*.csetheme");
    }

    public static List<Pack> apps() {
        return list(APPS_DIR, "*.cseapp");
    }

    public static boolean isShippedTheme(String id) {
        return containsStem(themes(), id);
    }

    public static boolean isShippedApp(String slug) {
        return containsStem(apps(), slug);
    }

    private static boolean containsStem(List<Pack> packs, String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        String needle = id.trim().toLowerCase(Locale.ROOT);
        for (Pack pack : packs) {
            if (needle.equals(pack.stem())) {
                return true;
            }
        }
        return false;
    }

    private static List<Pack> list(String directory, String filePattern) {
        List<Pack> out = new ArrayList<>();
        String location = directory == null ? "" : directory.replace('\\', '/');
        while (location.startsWith("/")) {
            location = location.substring(1);
        }
        if (!location.isEmpty() && !location.endsWith("/")) {
            location = location + "/";
        }
        String pattern = filePattern == null || filePattern.isBlank() ? "*" : filePattern;
        try {
            for (Resource resource : RESOLVER.getResources("classpath*:" + location + pattern)) {
                if (resource == null || !resource.exists() || !resource.isReadable()) {
                    continue;
                }
                String filename = resource.getFilename();
                if (filename == null || filename.isBlank()) {
                    continue;
                }
                try (InputStream in = resource.getInputStream()) {
                    out.add(new Pack(filename, in.readAllBytes()));
                }
            }
        } catch (IOException ex) {
            LOGGER.warn("Cannot list classpath {}/{}", directory, pattern, ex);
        }
        out.sort(Comparator.comparing(Pack::filename, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    public static final class Pack {
        private final String filename;
        private final byte[] bytes;

        public Pack(String filename, byte[] bytes) {
            this.filename = filename;
            this.bytes = bytes == null ? new byte[0] : bytes;
        }

        public String filename() {
            return filename;
        }

        public byte[] bytes() {
            return bytes;
        }

        public long size() {
            return bytes.length;
        }

        public String stem() {
            String name = filename.replace('\\', '/');
            int slash = name.lastIndexOf('/');
            if (slash >= 0) {
                name = name.substring(slash + 1);
            }
            int dot = name.lastIndexOf('.');
            return (dot > 0 ? name.substring(0, dot) : name).toLowerCase(Locale.ROOT);
        }
    }
}

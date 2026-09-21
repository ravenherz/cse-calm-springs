package com.ravenherz.cse.util.io;

import jakarta.servlet.ServletContext;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Writable CSE disk. {@code CSE_DISK_ROOT} / {@code cse.disk.root} (default {@code /var/cse})
 * is the <em>machine</em> root (certs, shared connector files). Instance data lives under
 * {@code {machine}/{tomcat-context}/}.
 */
public final class CseDisk {

    private static final Logger LOGGER = LoggerFactory.getLogger(CseDisk.class);
    public static final String DEFAULT_ROOT = "/var/cse";
    public static final String ROOT_INSTANCE_SLUG = "ROOT";
    static final String SITE_READY_MARKER = ".site-ready";
    private static final String CONTENT_CACHE = "content-cache";
    private static final String STATIC_PAGES = "apps";
    private static final String THEMES = "themes";
    private static final String CONTENT_PRIVATE = "content-private";
    private static final String CONFIGURATION = "configuration";

    private static volatile String instanceSlug;
    private static volatile File instanceRoot;
    private static volatile File staticPagesDir;
    private static volatile File themesDir;

    private CseDisk() {
    }

    public static void bindInstance(ServletContext servletContext) {
        bindInstanceSlug(slugFromContextPath(
                servletContext == null ? null : servletContext.getContextPath()));
    }

    public static synchronized void bindInstanceSlug(String slug) {
        instanceSlug = slugFromContextPath(slug);
        instanceRoot = new File(machineRoot(), instanceSlug);
        staticPagesDir = null;
        themesDir = null;
    }

    public static String slugFromContextPath(String contextPath) {
        if (contextPath == null || contextPath.isBlank()) {
            return ROOT_INSTANCE_SLUG;
        }
        String path = contextPath.trim().replace('\\', '/');
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.isEmpty() || ".".equals(path) || path.contains("..") || path.contains("/")) {
            return ROOT_INSTANCE_SLUG;
        }
        String cleaned = path.replaceAll("[^A-Za-z0-9._-]", "-");
        return cleaned.isBlank() ? ROOT_INSTANCE_SLUG : cleaned;
    }

    public static String instanceSlug() {
        String slug = instanceSlug;
        return slug == null ? ROOT_INSTANCE_SLUG : slug;
    }

    /**
     * Tomcat / VPS root. Certs stay here, not under {@link #cseRoot()}.
     */
    public static File machineRoot() {
        return new File(resolvedRoot());
    }

    /**
     * Instance root {@code {machine}/{context}}. All CSE data for this WAR.
     */
    public static File cseRoot() {
        File inst = instanceRoot;
        if (inst != null) {
            return inst;
        }
        return new File(machineRoot(), ROOT_INSTANCE_SLUG);
    }

    public static File contentCacheDir() {
        return new File(cseRoot(), CONTENT_CACHE);
    }

    public static File contentPrivateDir() {
        return new File(cseRoot(), CONTENT_PRIVATE);
    }

    public static File instanceConfigurationDir() {
        return new File(contentPrivateDir(), CONFIGURATION);
    }

    public static File secretFile(String filename) {
        return new File(instanceConfigurationDir(), filename);
    }

    public static File siteReadyMarker() {
        return new File(contentPrivateDir(), SITE_READY_MARKER);
    }

    public static boolean siteReadyMarkerPresent() {
        File marker = siteReadyMarker();
        return marker.isFile();
    }

    public static boolean writeSiteReadyMarker() {
        try {
            File dir = contentPrivateDir();
            if (!ensureWritable(dir)) {
                return false;
            }
            restrictOwnerOnly(dir);
            File marker = siteReadyMarker();
            FileUtils.writeByteArrayToFile(marker,
                    Instant.now().toString().getBytes(StandardCharsets.UTF_8));
            restrictOwnerOnly(marker);
            return true;
        } catch (IOException ex) {
            LOGGER.warn("Cannot write site-ready marker: {}", ex.getMessage());
            return false;
        }
    }

    public static boolean deleteSiteReadyMarker() {
        File marker = siteReadyMarker();
        if (!marker.isFile()) {
            return true;
        }
        if (marker.delete()) {
            return true;
        }
        LOGGER.warn("Cannot delete site-ready marker {}", marker.getAbsolutePath());
        return false;
    }

    public static boolean ensureInstanceConfigurationWritable() {
        File dir = instanceConfigurationDir();
        if (!ensureWritable(dir)) {
            return false;
        }
        restrictOwnerOnly(contentPrivateDir());
        return true;
    }

    public static File cachedMedia(String protectedPath) {
        String relative = protectedPath == null ? "" : protectedPath.replace('\\', '/');
        while (relative.startsWith("/")) {
            relative = relative.substring(1);
        }
        return new File(contentCacheDir(), relative);
    }

    public static File staticPagesDir() throws IOException {
        File cached = staticPagesDir;
        if (cached != null) {
            return cached;
        }
        synchronized (CseDisk.class) {
            if (staticPagesDir != null) {
                return staticPagesDir;
            }
            File preferred = new File(contentCacheDir(), STATIC_PAGES);
            if (ensureWritable(preferred)) {
                staticPagesDir = preferred;
                return preferred;
            }
            File fallback = new File(System.getProperty("java.io.tmpdir"),
                    "cse-apps-" + instanceSlug());
            if (ensureWritable(fallback)) {
                LOGGER.warn("Cannot write static apps under {} ; using {}", preferred.getAbsolutePath(),
                        fallback.getAbsolutePath());
                staticPagesDir = fallback;
                return fallback;
            }
            throw new IOException("Cannot write static apps to " + preferred.getAbsolutePath()
                    + " or " + fallback.getAbsolutePath());
        }
    }

    public static File themesDir() throws IOException {
        File cached = themesDir;
        if (cached != null) {
            return cached;
        }
        synchronized (CseDisk.class) {
            if (themesDir != null) {
                return themesDir;
            }
            File preferred = new File(contentCacheDir(), THEMES);
            if (ensureWritable(preferred)) {
                themesDir = preferred;
                return preferred;
            }
            File fallback = new File(System.getProperty("java.io.tmpdir"),
                    "cse-themes-" + instanceSlug());
            if (ensureWritable(fallback)) {
                LOGGER.warn("Cannot write theme packs under {} ; using {}", preferred.getAbsolutePath(),
                        fallback.getAbsolutePath());
                themesDir = fallback;
                return fallback;
            }
            throw new IOException("Cannot write theme packs to " + preferred.getAbsolutePath()
                    + " or " + fallback.getAbsolutePath());
        }
    }

    public static synchronized void resetForTests() {
        instanceSlug = null;
        instanceRoot = null;
        staticPagesDir = null;
        themesDir = null;
    }

    private static String resolvedRoot() {
        String override = System.getProperty("cse.disk.root");
        if (override == null || override.isBlank()) {
            override = System.getenv("CSE_DISK_ROOT");
        }
        if (override == null || override.isBlank()) {
            return DEFAULT_ROOT;
        }
        return override.trim();
    }

    static boolean ensureWritable(File dir) {
        try {
            if (!dir.exists() && !dir.mkdirs()) {
                return false;
            }
            if (!dir.isDirectory()) {
                return false;
            }
            File probe = new File(dir, ".cse-write-probe");
            FileUtils.writeByteArrayToFile(probe, new byte[] { 1 });
            if (!probe.delete()) {
                probe.deleteOnExit();
            }
            return true;
        } catch (IOException ex) {
            LOGGER.info("Directory is not writable: {} ({})", dir.getAbsolutePath(), ex.getMessage());
            return false;
        }
    }

    public static void restrictOwnerOnly(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        file.setReadable(false, false);
        file.setWritable(false, false);
        file.setExecutable(false, false);
        file.setReadable(true, true);
        file.setWritable(true, true);
        if (file.isDirectory()) {
            file.setExecutable(true, true);
        }
    }
}

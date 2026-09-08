package com.ravenherz.cse.util.themes;

import com.ravenherz.cse.util.io.CseDisk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Installs {@code .csetheme} zips. Overlay packs ({@code shell} modern/2000s) are
 * CSS plus optional shared {@code js/*.js}. Own-shell packs add a zip-root
 * {@code index.html}. JS shells also require {@code js/theme.js}.
 * Same extension; not {@code .cseapp}.
 */
@Component
public class ThemePackDeployer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThemePackDeployer.class);

    @Autowired(required = false)
    private SpringTemplateEngine templateEngine;

    public static final String THEME_PACKAGE_EXT = ".csetheme";
    private static final Pattern ID = Pattern.compile("^[a-z0-9][a-z0-9-]{0,62}$");
    private static final Set<String> RESERVED = Set.of(
            "admin", "fragments", "modern", "index", "editor"
    );
    private static final Set<String> ALLOWED_EXT = Set.of(
            ".css", ".woff", ".woff2", ".ttf", ".otf", ".png", ".jpg", ".jpeg",
            ".gif", ".svg", ".webp", ".ico"
    );
    private static final int MAX_ENTRIES = 500;
    private static final long MAX_UNCOMPRESSED = 20L * 1024 * 1024;
    private static final long MAX_ENTRY = 2L * 1024 * 1024;
    private static final int MAX_MANIFEST = 64 * 1024;

    public static boolean isReservedId(String id) {
        return id != null && RESERVED.contains(id.toLowerCase(Locale.ROOT));
    }

    public Path themesRoot() throws IOException {
        return CseDisk.themesDir().getAbsoluteFile().toPath().normalize();
    }

    public void validateId(String id) {
        if (id == null || !ID.matcher(id).matches() || isReservedId(id)) {
            throw new IllegalArgumentException("Invalid theme id");
        }
    }

    public void validateZip(byte[] zipBytes) throws IOException {
        inspectAndExtract(zipBytes, null);
        ThemeManifest manifest = readManifest(zipBytes);
        validateManifestShell(manifest);
        boolean customShell = ThemeCatalog.isCustomShell(manifest.getShell());
        if (customShell && !zipHasRootIndex(zipBytes)) {
            throw new IOException("Own-shell and JS theme packs must include index.html at the package root");
        }
        if (ThemeCatalog.isJsShell(manifest.getShell()) && !zipHasThemeScript(zipBytes)) {
            throw new IOException("JS theme pack must include js/theme.js");
        }
    }

    public ThemeManifest readManifest(byte[] zipBytes) {
        byte[] data = readManifestBytes(zipBytes);
        if (data == null) {
            return null;
        }
        return ThemeManifest.parse(new String(data, StandardCharsets.UTF_8));
    }

    public void deploy(String id, byte[] zipBytes) throws IOException {
        validateId(id);
        deployEngineTheme(id, zipBytes);
    }

    /**
     * Explode a packed theme even when the id is reserved for uploads (WAR-shipped packs).
     */
    public void deployEngineTheme(String id, byte[] zipBytes) throws IOException {
        if (id == null || !ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid theme id");
        }
        validateZip(zipBytes);
        Path root = themesRoot();
        Path dest = childDir(root, id);
        Path tmp = childDir(root, "." + id + ".tmp");
        Path bak = childDir(root, "." + id + ".bak");
        deleteRecursively(tmp);
        mkdir(tmp.toFile());
        try {
            inspectAndExtract(zipBytes, tmp);
            requireOverlayLayout(tmp);
            ThemeManifest manifest = readDiskManifest(tmp);
            validateManifestShell(manifest);
            if (manifest.getId() != null && !id.equalsIgnoreCase(manifest.getId())) {
                throw new IOException("theme.json id must match the install id");
            }
            if (ThemeCatalog.isCustomShell(manifest.getShell())) {
                requireOwnShellLayout(tmp, id);
            }
            if (ThemeCatalog.isJsShell(manifest.getShell())) {
                requireJsShellLayout(tmp);
            }
            deleteRecursively(bak);
            if (Files.exists(dest)) {
                Files.move(dest, bak);
            }
            try {
                Files.move(tmp, dest);
                deleteRecursively(bak);
            } catch (IOException moveFailed) {
                if (Files.exists(bak) && !Files.exists(dest)) {
                    Files.move(bak, dest);
                }
                throw moveFailed;
            }
            LOGGER.info("Deployed theme pack '{}' to {}", id, dest);
            clearTemplateCache();
        } catch (IOException | RuntimeException ex) {
            deleteRecursively(tmp);
            throw ex;
        }
    }

    public static List<String> schemasInZip(byte[] zipBytes) {
        List<String> names = new ArrayList<>();
        if (zipBytes == null || zipBytes.length < 4) {
            return names;
        }
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String lower = entry.getName().replace('\\', '/').toLowerCase(Locale.ROOT);
                int marker = lower.lastIndexOf("css/color-schemas/");
                if (marker < 0) {
                    continue;
                }
                String file = lower.substring(marker + "css/color-schemas/".length());
                if (file.contains("/") || !file.endsWith(".css") || file.length() <= 4) {
                    continue;
                }
                names.add(file.substring(0, file.length() - 4));
            }
        } catch (IOException ex) {
            LOGGER.warn("Could not list color schemas in theme zip", ex);
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public static boolean zipHasPreview(byte[] zipBytes) {
        if (zipBytes == null || zipBytes.length < 4) {
            return false;
        }
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName().replace('\\', '/');
                int slash = name.lastIndexOf('/');
                String file = (slash < 0 ? name : name.substring(slash + 1)).toLowerCase(Locale.ROOT);
                int depth = slash < 0 ? 0 : slashCount(name);
                if (depth > 1) {
                    continue;
                }
                if ("preview.jpg".equals(file) || "preview.jpeg".equals(file) || "preview.png".equals(file)) {
                    return true;
                }
            }
        } catch (IOException ex) {
            return false;
        }
        return false;
    }

    public void undeploy(String id) throws IOException {
        validateId(id);
        removeExploded(id);
    }

    /**
     * Drop exploded trees that are not in {@code keep} (current WAR packs + operator uploads).
     * Also removes leftover reserved shells that this WAR no longer ships.
     */
    public void retainOnly(Set<String> keep) throws IOException {
        Set<String> retain = new HashSet<>();
        if (keep != null) {
            for (String id : keep) {
                if (id != null && !id.isBlank()) {
                    retain.add(id.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        Path root = themesRoot();
        if (!Files.isDirectory(root)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path dir : stream) {
                if (!Files.isDirectory(dir)) {
                    continue;
                }
                String name = dir.getFileName().toString();
                if (name.startsWith(".")) {
                    continue;
                }
                if (retain.contains(name.toLowerCase(Locale.ROOT))) {
                    continue;
                }
                if (!ID.matcher(name).matches()) {
                    continue;
                }
                removeExploded(name);
            }
        }
    }

    public void removeExploded(String id) throws IOException {
        if (id == null || !ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid theme id");
        }
        deleteRecursively(childDir(themesRoot(), id));
        deleteRecursively(childDir(themesRoot(), "." + id + ".tmp"));
        deleteRecursively(childDir(themesRoot(), "." + id + ".bak"));
        LOGGER.info("Removed theme pack '{}'", id);
        clearTemplateCache();
    }

    private void validateManifestShell(ThemeManifest manifest) throws IOException {
        if (manifest == null || manifest.getShell() == null
                || !ThemeCatalog.isAllowedShell(manifest.getShell())) {
            throw new IOException("theme.json must set shell to modern, 2000s, own, or js");
        }
    }

    private void requireOwnShellLayout(Path dest, String id) throws IOException {
        Path index = dest.resolve("index.html");
        if (!Files.isRegularFile(index)) {
            throw new IOException("Own-shell theme pack must include index.html at the package root");
        }
        String html = Files.readString(index, StandardCharsets.UTF_8);
        if (!html.contains("id=\"page-body\"") && !html.contains("id='page-body'")) {
            LOGGER.warn("Own-shell '{}' index.html is missing id=\"page-body\"", id);
        }
    }

    private void requireJsShellLayout(Path dest) throws IOException {
        Path script = dest.resolve("js").resolve("theme.js");
        if (!Files.isRegularFile(script)) {
            throw new IOException("JS theme pack must include js/theme.js");
        }
    }

    private void clearTemplateCache() {
        if (templateEngine != null) {
            templateEngine.clearTemplateCache();
        }
    }

    private void inspectAndExtract(byte[] zipBytes, Path dest) throws IOException {
        if (zipBytes == null || zipBytes.length < 4
                || zipBytes[0] != 0x50 || zipBytes[1] != 0x4B) {
            throw new IOException("Not a zip file");
        }
        List<String> names = new ArrayList<>();
        long uncompressed = 0;
        int entries = 0;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (name.startsWith("/") || name.contains("..")) {
                    throw new IOException("Zip entry escapes the theme root: " + name);
                }
                if (name.isEmpty() || name.equals("/") || isJunkEntry(name)) {
                    continue;
                }
                if (isReservedFolder(name)) {
                    throw new IOException("Theme packs cannot include admin/ or fragments/: " + name);
                }
                if (isForbiddenEntry(name)) {
                    throw new IOException("Theme packs cannot include HTML or scripts: " + name);
                }
                entries++;
                if (entries > MAX_ENTRIES) {
                    throw new IOException("Zip has too many entries");
                }
                names.add(name);
                if (entry.isDirectory()) {
                    if (dest != null) {
                        mkdir(safeResolve(dest, name).toFile());
                    }
                    continue;
                }
                if (!isAllowedFile(name)) {
                    throw new IOException("Unsupported theme file: " + name);
                }
                long size = copyOrCount(zis, dest == null ? null : safeResolve(dest, name), entry.getSize());
                uncompressed += size;
                if (uncompressed > MAX_UNCOMPRESSED) {
                    throw new IOException("Zip expands beyond the size limit");
                }
            }
        }
        if (names.isEmpty()) {
            throw new IOException("Zip is empty");
        }
        String prefix = wrapperPrefix(names);
        if (dest != null && prefix != null) {
            unwrap(dest, prefix);
        }
        if (dest == null) {
            requireOverlayNames(names, prefix);
        }
    }

    private void requireOverlayLayout(Path dest) throws IOException {
        if (!Files.isRegularFile(dest.resolve(ThemeManifest.FILE_NAME))) {
            throw new IOException("Zip must contain theme.json at the package root");
        }
        if (!Files.isRegularFile(dest.resolve("css").resolve("styles.css"))) {
            throw new IOException("Theme pack must include css/styles.css");
        }
        if (!Files.isRegularFile(dest.resolve("css").resolve("cse-player.css"))) {
            throw new IOException("Theme pack must include css/cse-player.css");
        }
        Path schemas = dest.resolve("css").resolve("color-schemas");
        if (!Files.isDirectory(schemas) || !hasCssFile(schemas)) {
            throw new IOException("Theme pack must include at least one css/color-schemas/*.css file");
        }
    }

    private boolean hasCssFile(Path dir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.css")) {
            return stream.iterator().hasNext();
        }
    }

    private ThemeManifest readDiskManifest(Path dest) throws IOException {
        Path file = dest.resolve(ThemeManifest.FILE_NAME);
        if (!Files.isRegularFile(file)) {
            return null;
        }
        return ThemeManifest.parse(Files.readString(file, StandardCharsets.UTF_8));
    }

    private byte[] readManifestBytes(byte[] zipBytes) {
        if (zipBytes == null || zipBytes.length < 4) {
            return null;
        }
        byte[] root = null;
        byte[] wrapped = null;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName().replace('\\', '/');
                if (isJunkEntry(name) || !isManifestEntry(name)) {
                    continue;
                }
                byte[] data;
                try {
                    data = readLimited(zis, MAX_MANIFEST);
                } catch (IOException ex) {
                    LOGGER.warn("Skipping oversized {}", ThemeManifest.FILE_NAME);
                    continue;
                }
                int depth = slashCount(name);
                if (depth == 0) {
                    root = data;
                    break;
                }
                if (depth == 1 && wrapped == null) {
                    wrapped = data;
                }
            }
        } catch (IOException ex) {
            LOGGER.warn("Could not read {}", ThemeManifest.FILE_NAME, ex);
            return null;
        }
        return root != null ? root : wrapped;
    }

    private long copyOrCount(InputStream in, Path outFile, long declaredSize) throws IOException {
        if (declaredSize > MAX_ENTRY) {
            throw new IOException("Zip entry is too large");
        }
        if (outFile != null) {
            File parent = outFile.toFile().getParentFile();
            if (parent != null) {
                mkdir(parent);
            }
        }
        long written = 0;
        byte[] buf = new byte[8192];
        try (OutputStream out = outFile == null
                ? OutputStream.nullOutputStream()
                : new FileOutputStream(outFile.toFile())) {
            int n;
            while ((n = in.read(buf)) >= 0) {
                written += n;
                if (written > MAX_ENTRY) {
                    throw new IOException("Zip entry is too large");
                }
                if (outFile != null) {
                    out.write(buf, 0, n);
                }
            }
        }
        return written;
    }

    private boolean isManifestEntry(String name) {
        int slash = name.lastIndexOf('/');
        String file = slash < 0 ? name : name.substring(slash + 1);
        return ThemeManifest.FILE_NAME.equalsIgnoreCase(file);
    }

    private boolean hasManifest(List<String> names, String prefix) {
        String expected = prefix == null ? ThemeManifest.FILE_NAME : prefix + ThemeManifest.FILE_NAME;
        return names.stream().anyMatch(n -> n.equalsIgnoreCase(expected));
    }

    private void requireOverlayNames(List<String> names, String prefix) throws IOException {
        if (!hasManifest(names, prefix)) {
            throw new IOException("Zip must contain theme.json at the package root");
        }
        String base = prefix == null ? "" : prefix;
        boolean styles = false;
        boolean player = false;
        boolean schema = false;
        for (String name : names) {
            String relative = name;
            if (!base.isEmpty()) {
                if (!name.startsWith(base)) {
                    continue;
                }
                relative = name.substring(base.length());
            }
            String lower = relative.toLowerCase(Locale.ROOT);
            if ("css/styles.css".equals(lower)) {
                styles = true;
            } else if ("css/cse-player.css".equals(lower)) {
                player = true;
            } else if (lower.startsWith("css/color-schemas/") && lower.endsWith(".css")
                    && lower.length() > "css/color-schemas/".length()) {
                schema = true;
            }
        }
        if (!styles) {
            throw new IOException("Theme pack must include css/styles.css");
        }
        if (!player) {
            throw new IOException("Theme pack must include css/cse-player.css");
        }
        if (!schema) {
            throw new IOException("Theme pack must include at least one css/color-schemas/*.css file");
        }
    }

    private boolean isRootIndexHtml(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith("/")) {
            return false;
        }
        int slash = lower.lastIndexOf('/');
        String file = slash < 0 ? lower : lower.substring(slash + 1);
        if (!"index.html".equals(file) && !"index.htm".equals(file)) {
            return false;
        }
        int slashes = slashCount(name);
        return slashes == 0 || slashes == 1;
    }

    private boolean zipHasThemeScript(byte[] zipBytes) {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory() || isJunkEntry(entry.getName())) {
                    continue;
                }
                String name = entry.getName().replace('\\', '/').toLowerCase(Locale.ROOT);
                if (name.endsWith("/js/theme.js") || name.equals("js/theme.js")) {
                    return true;
                }
            }
        } catch (IOException ex) {
            return false;
        }
        return false;
    }

    private boolean isReservedFolder(String name) {
        for (String part : name.replace('\\', '/').split("/")) {
            if ("admin".equalsIgnoreCase(part) || "fragments".equalsIgnoreCase(part)) {
                return true;
            }
        }
        return false;
    }

    private boolean zipHasRootIndex(byte[] zipBytes) {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory() || isJunkEntry(entry.getName())) {
                    continue;
                }
                if (isRootIndexHtml(entry.getName().replace('\\', '/'))) {
                    return true;
                }
            }
        } catch (IOException ex) {
            return false;
        }
        return false;
    }

    /** Extra HTML is rejected. Flat {@code js/*.js} is allowed on every pack. */
    private boolean isForbiddenEntry(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            return !isRootIndexHtml(name);
        }
        if (lower.endsWith(".js")) {
            return !isThemeScript(name);
        }
        return lower.endsWith(".mjs") || lower.endsWith(".class") || lower.endsWith(".jar")
                || lower.endsWith(".war") || lower.endsWith(".sh") || lower.endsWith(".php")
                || lower.endsWith(".jsp");
    }

    private boolean isThemeScript(String name) {
        String lower = name.replace('\\', '/').toLowerCase(Locale.ROOT);
        int marker = lower.lastIndexOf("js/");
        if (marker < 0) {
            return false;
        }
        String file = lower.substring(marker + 3);
        return !file.contains("/") && file.endsWith(".js") && file.length() > 3;
    }

    private boolean isAllowedFile(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        int slash = lower.lastIndexOf('/');
        String file = slash < 0 ? lower : lower.substring(slash + 1);
        if (isRootIndexHtml(name)) {
            return true;
        }
        if (isThemeScript(name)) {
            return true;
        }
        if (ThemeManifest.FILE_NAME.equals(file)) {
            return slash < 0 || slashCount(name) == 1;
        }
        int dot = file.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        return ALLOWED_EXT.contains(file.substring(dot));
    }

    private boolean isJunkEntry(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return name.startsWith("__MACOSX/") || name.equals("__MACOSX")
                || lower.endsWith("/.ds_store") || lower.equals(".ds_store")
                || lower.endsWith("/thumbs.db") || lower.equals("thumbs.db");
    }

    private static int slashCount(String name) {
        int count = 0;
        for (int i = 0; i < name.length(); i++) {
            if (name.charAt(i) == '/') {
                count++;
            }
        }
        return count;
    }

    private byte[] readLimited(InputStream in, int max) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        int total = 0;
        while ((n = in.read(buf)) >= 0) {
            total += n;
            if (total > max) {
                throw new IOException("Entry exceeds limit");
            }
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private void mkdir(File dir) throws IOException {
        if (dir == null) {
            return;
        }
        if (!dir.exists() && !dir.mkdirs() && !dir.isDirectory()) {
            throw new IOException("Directory '" + dir.getAbsolutePath() + "' could not be created");
        }
        if (dir.exists() && !dir.isDirectory()) {
            throw new IOException("Not a directory: " + dir.getAbsolutePath());
        }
    }

    private Path childDir(Path root, String name) throws IOException {
        Path dest = root.resolve(name).normalize();
        if (!dest.startsWith(root) || dest.equals(root)) {
            throw new IOException("Refusing to use path outside themes root");
        }
        return dest;
    }

    private Path safeResolve(Path dest, String name) throws IOException {
        Path resolved = dest.resolve(name).normalize();
        if (!resolved.startsWith(dest)) {
            throw new IOException("Zip entry escapes the theme root: " + name);
        }
        return resolved;
    }

    private String wrapperPrefix(List<String> names) {
        Set<String> tops = new HashSet<>();
        for (String name : names) {
            int slash = name.indexOf('/');
            String top = slash < 0 ? name : name.substring(0, slash);
            if (!top.isEmpty() && !top.startsWith(".") && !top.equals("__MACOSX")) {
                tops.add(top);
            }
        }
        if (tops.size() != 1) {
            return null;
        }
        String folder = tops.iterator().next();
        boolean folderLooksLikeFile = folder.contains(".");
        boolean hasNested = names.stream().anyMatch(n -> n.startsWith(folder + "/"));
        if (folderLooksLikeFile && !hasNested) {
            return null;
        }
        return folder + "/";
    }

    private void unwrap(Path dest, String prefix) throws IOException {
        Path inner = dest.resolve(prefix.substring(0, prefix.length() - 1));
        if (!Files.isDirectory(inner)) {
            return;
        }
        Path tmpUnwrap = dest.resolveSibling(dest.getFileName() + ".unwrap");
        deleteRecursively(tmpUnwrap);
        Files.move(inner, tmpUnwrap);
        try (var stream = Files.list(dest)) {
            for (Path leftover : stream.toList()) {
                deleteRecursively(leftover);
            }
        }
        try (var stream = Files.list(tmpUnwrap)) {
            for (Path child : stream.toList()) {
                Files.move(child, dest.resolve(child.getFileName()));
            }
        }
        deleteRecursively(tmpUnwrap);
    }

    private void deleteRecursively(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (var walk = Files.walk(path)) {
            walk.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException io) {
                throw io;
            }
            throw e;
        }
    }
}

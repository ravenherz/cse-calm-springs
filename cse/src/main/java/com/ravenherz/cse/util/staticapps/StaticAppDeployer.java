package com.ravenherz.cse.util.staticapps;

import com.ravenherz.cse.util.io.CseDisk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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

@Component
public class StaticAppDeployer {

    private static final Logger LOGGER = LoggerFactory.getLogger(StaticAppDeployer.class);

    public static final String PRODUCT_LOGO = "product-logo.jpg";
    public static final int PRODUCT_LOGO_PX = 512;
    public static final String APP_PACKAGE_EXT = ".cseapp";
    public static final String INSTALLER_SLUG = "setup";
    public static final String INSTALLER_CLASSPATH = "install/setup.cseapp";
    public static final String ADMIN_SLUG = "admin";
    public static final String ADMIN_CLASSPATH = "install/admin.cseapp";
    private static final Pattern SLUG = Pattern.compile("^[a-z0-9][a-z0-9-]{0,62}$");
    private static final Set<String> RESERVED = Set.of(
            "index", "editor", "admin", "apps", "rest", "account", "error",
            "content-public", "content-private", "content-cache", "content-protected",
            "static-pages", INSTALLER_SLUG, "install"
    );
    private static final int MAX_ENTRIES = 4000;
    private static final long MAX_UNCOMPRESSED = 200L * 1024 * 1024;
    private static final long MAX_ENTRY = 50L * 1024 * 1024;
    private static final int MAX_MANIFEST = 64 * 1024;

    public Path pagesRoot() throws IOException {
        return CseDisk.staticPagesDir().getAbsoluteFile().toPath().normalize();
    }

    public static boolean isReservedSlug(String slug) {
        return slug != null && RESERVED.contains(slug.toLowerCase(Locale.ROOT));
    }

    public void validateSlug(String slug) {
        if (slug == null || !SLUG.matcher(slug).matches() || isReservedSlug(slug)) {
            throw new IllegalArgumentException("Invalid app slug");
        }
    }

    public void validateZip(byte[] zipBytes) throws IOException {
        if (zipBytes == null || zipBytes.length < 4) {
            throw new IOException("Not a zip file");
        }
        boolean pk = zipBytes[0] == 0x50 && zipBytes[1] == 0x4B;
        if (!pk) {
            throw new IOException("Not a zip file");
        }
        inspectAndExtract(zipBytes, null);
    }

    public AppManifest readManifest(byte[] zipBytes) {
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
                    LOGGER.warn("Skipping oversized {}", AppManifest.FILE_NAME);
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
            LOGGER.warn("Could not read {}", AppManifest.FILE_NAME, ex);
            return null;
        }
        byte[] chosen = root != null ? root : wrapped;
        if (chosen == null) {
            return null;
        }
        return AppManifest.parse(new String(chosen, StandardCharsets.UTF_8));
    }

    public void deploy(String slug, byte[] zipBytes) throws IOException {
        validateSlug(slug);
        deployEngineApp(slug, zipBytes);
    }

    /**
     * Explode a packed app even when the slug is reserved for uploads (first-boot installer).
     */
    public void deployEngineApp(String slug, byte[] zipBytes) throws IOException {
        if (slug == null || !SLUG.matcher(slug).matches()) {
            throw new IllegalArgumentException("Invalid app slug");
        }
        validateZip(zipBytes);
        Path root = pagesRoot();
        Path dest = childDir(root, slug);
        Path tmp = childDir(root, "." + slug + ".tmp");
        Path bak = childDir(root, "." + slug + ".bak");
        deleteRecursively(tmp);
        mkdir(tmp.toFile());
        try {
            inspectAndExtract(zipBytes, tmp);
            requireIndexHtml(tmp);
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
            LOGGER.info("Deployed static app '{}' to {}", slug, dest);
        } catch (IOException | RuntimeException ex) {
            deleteRecursively(tmp);
            throw ex;
        }
    }

    public void undeploy(String slug) throws IOException {
        validateSlug(slug);
        undeployEngineApp(slug);
    }

    public void undeployEngineApp(String slug) throws IOException {
        if (slug == null || !SLUG.matcher(slug).matches()) {
            throw new IllegalArgumentException("Invalid app slug");
        }
        deleteRecursively(childDir(pagesRoot(), slug));
        deleteRecursively(childDir(pagesRoot(), "." + slug + ".tmp"));
        deleteRecursively(childDir(pagesRoot(), "." + slug + ".bak"));
        LOGGER.info("Removed static app tree '{}'", slug);
    }

    public Path resolvePublicFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank() || relativePath.contains("..")) {
            return null;
        }
        try {
            Path root = pagesRoot();
            Path file = root.resolve(relativePath).normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                return null;
            }
            return file;
        } catch (IOException ex) {
            return null;
        }
    }

    public boolean hasValidProductLogo(String slug) {
        Path file = resolvePublicFile(slug + "/" + PRODUCT_LOGO);
        if (file == null) {
            return false;
        }
        try {
            try (InputStream in = Files.newInputStream(file)) {
                byte[] magic = in.readNBytes(3);
                if (magic.length < 3
                        || (magic[0] & 0xFF) != 0xFF
                        || (magic[1] & 0xFF) != 0xD8
                        || (magic[2] & 0xFF) != 0xFF) {
                    return false;
                }
            }
            try (ImageInputStream iis = ImageIO.createImageInputStream(file.toFile())) {
                if (iis == null) {
                    return false;
                }
                var readers = ImageIO.getImageReadersByFormatName("JPEG");
                if (!readers.hasNext()) {
                    return false;
                }
                ImageReader reader = readers.next();
                try {
                    reader.setInput(iis);
                    return reader.getWidth(0) == PRODUCT_LOGO_PX
                            && reader.getHeight(0) == PRODUCT_LOGO_PX;
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException ex) {
            LOGGER.warn("Could not read {} for slug '{}'", PRODUCT_LOGO, slug, ex);
            return false;
        }
    }

    private Path childDir(Path root, String name) throws IOException {
        Path dest = root.resolve(name).normalize();
        if (!dest.startsWith(root) || dest.equals(root)) {
            throw new IOException("Refusing to use path outside static pages root");
        }
        return dest;
    }

    private void inspectAndExtract(byte[] zipBytes, Path dest) throws IOException {
        List<String> names = new ArrayList<>();
        long uncompressed = 0;
        int entries = 0;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (name.startsWith("/") || name.contains("..")) {
                    throw new IOException("Zip entry escapes the app root: " + name);
                }
                if (name.isEmpty() || name.equals("/") || isJunkEntry(name)) {
                    continue;
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
        if (dest == null && !hasIndexHtml(names, wrapperPrefix(names))) {
            throw new IOException("Zip must contain index.html at the app root");
        }
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
        return AppManifest.FILE_NAME.equalsIgnoreCase(file);
    }

    private int slashCount(String name) {
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

    private boolean isJunkEntry(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return name.startsWith("__MACOSX/") || name.equals("__MACOSX")
                || lower.endsWith("/.ds_store") || lower.equals(".ds_store")
                || lower.endsWith("/thumbs.db") || lower.equals("thumbs.db");
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

    private Path safeResolve(Path dest, String name) throws IOException {
        Path resolved = dest.resolve(name).normalize();
        if (!resolved.startsWith(dest)) {
            throw new IOException("Zip entry escapes the app root: " + name);
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

    private boolean hasIndexHtml(List<String> names, String prefix) {
        String index = prefix == null ? "index.html" : prefix + "index.html";
        String indexLower = index.toLowerCase(Locale.ROOT);
        return names.stream().anyMatch(n -> n.equalsIgnoreCase(index) || n.toLowerCase(Locale.ROOT).equals(indexLower));
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

    private void requireIndexHtml(Path dest) throws IOException {
        Path index = dest.resolve("index.html");
        if (!Files.isRegularFile(index)) {
            throw new IOException("Zip must contain index.html at the app root");
        }
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

package com.ravenherz.cse.util.video;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Bundled FFmpeg/ffprobe executables. Do not {@code Loader.load} the bytedeco
 * ffmpeg classes: on Linux that dlopens Pulse, X11, and VA-API into the JVM and
 * can block forever on a headless host.
 */
public final class FfmpegBinaries {

    private static final Logger LOGGER = LoggerFactory.getLogger(FfmpegBinaries.class);
    private static final Object LOCK = new Object();

    private static volatile Path dir;
    private static volatile String ffmpeg;
    private static volatile String ffprobe;

    private FfmpegBinaries() {
    }

    public static String ffmpeg() throws IOException {
        extract();
        return ffmpeg;
    }

    public static String ffprobe() throws IOException {
        extract();
        return ffprobe;
    }

    public static Path libraryDir() throws IOException {
        extract();
        return dir;
    }

    public static boolean available() {
        try {
            ffmpeg();
            ffprobe();
            return true;
        } catch (Exception e) {
            LOGGER.warn("Bundled FFmpeg is not available: {}", e.getMessage());
            return false;
        }
    }

    static String platform() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        String family;
        if (os.startsWith("windows")) {
            family = "windows";
        } else if (os.startsWith("mac os") || os.startsWith("darwin")) {
            family = "macosx";
        } else {
            int space = os.indexOf(' ');
            family = space > 0 ? os.substring(0, space) : os;
        }
        if ("amd64".equals(arch) || "x86-64".equals(arch) || "x64".equals(arch)) {
            arch = "x86_64";
        } else if (arch.startsWith("aarch64") || arch.startsWith("arm64")) {
            arch = "arm64";
        }
        return family + "-" + arch;
    }

    private static void extract() throws IOException {
        if (ffmpeg != null && ffprobe != null && dir != null) {
            return;
        }
        synchronized (LOCK) {
            if (ffmpeg != null && ffprobe != null && dir != null) {
                return;
            }
            String platform = platform();
            String folder = windows() ? platform : platform + "-headless";
            Path dest = VideoWork.directory().resolve("native").resolve(folder);
            Files.createDirectories(dest);
            Path ffmpegPath = dest.resolve(toolName("ffmpeg"));
            Path ffprobePath = dest.resolve(toolName("ffprobe"));
            Path marker = dest.resolve(".ok");
            if (!Files.isRegularFile(marker) || !Files.isRegularFile(ffmpegPath) || !Files.isRegularFile(ffprobePath)) {
                unpack(platform, dest);
                if (!windows()) {
                    alias(dest, "libva.so", "libva.so.2");
                    alias(dest, "libva-drm.so", "libva-drm.so.2");
                    LinuxSharedStubs.install(dest);
                }
                if (!Files.isRegularFile(ffmpegPath) || !Files.isRegularFile(ffprobePath)) {
                    throw new IOException("Bundled FFmpeg natives missing for " + platform);
                }
                ffmpegPath.toFile().setExecutable(true, false);
                ffprobePath.toFile().setExecutable(true, false);
                Files.writeString(marker, folder);
                LOGGER.info("Extracted bundled FFmpeg to {}", dest.toAbsolutePath());
            }
            dir = dest;
            ffmpeg = ffmpegPath.toAbsolutePath().toString();
            ffprobe = ffprobePath.toAbsolutePath().toString();
        }
    }

    private static void unpack(String platform, Path dest) throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:org/bytedeco/ffmpeg/" + platform + "/*");
        if (resources.length == 0) {
            throw new IOException("No bundled FFmpeg files for " + platform);
        }
        int copied = 0;
        for (Resource resource : resources) {
            String name = resource.getFilename();
            if (name == null || name.isBlank() || skip(name) || !resource.isReadable()) {
                continue;
            }
            Path target = dest.resolve(name);
            try (InputStream in = resource.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            copied++;
        }
        if (copied == 0) {
            throw new IOException("Could not unpack FFmpeg files for " + platform);
        }
    }

    private static void alias(Path dir, String from, String to) throws IOException {
        Path source = dir.resolve(from);
        Path target = dir.resolve(to);
        if (Files.isRegularFile(source) && !Files.isRegularFile(target)) {
            Files.copy(source, target);
        }
    }

    private static boolean skip(String filename) {
        String name = filename.toLowerCase(Locale.ROOT);
        return name.startsWith("libjni") || name.startsWith("jni");
    }

    private static String toolName(String name) {
        return windows() ? name + ".exe" : name;
    }

    static boolean windows() {
        return platform().startsWith("windows");
    }
}

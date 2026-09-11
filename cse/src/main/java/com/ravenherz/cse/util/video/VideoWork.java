package com.ravenherz.cse.util.video;

import com.ravenherz.cse.util.io.CseDisk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class VideoWork {

    private VideoWork() {
    }

    public static Path directory() throws IOException {
        Path preferred = CseDisk.cseRoot().toPath().resolve("work").resolve("video");
        try {
            Files.createDirectories(preferred);
            Path probe = Files.createTempFile(preferred, ".probe-", ".tmp");
            Files.deleteIfExists(probe);
            return preferred;
        } catch (IOException e) {
            Path fallback = Path.of(System.getProperty("java.io.tmpdir"), "cse-video");
            Files.createDirectories(fallback);
            return fallback;
        }
    }

    public static Path temp(String prefix, String suffix) throws IOException {
        return Files.createTempFile(directory(), prefix, suffix == null ? "" : suffix);
    }

    public static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}

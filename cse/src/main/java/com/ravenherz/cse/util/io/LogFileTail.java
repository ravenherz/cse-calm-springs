package com.ravenherz.cse.util.io;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class LogFileTail {

    public static final int WINDOW_BYTES = 256 * 1024;
    public static final String WINDOW_LABEL = "256 KB";
    public static final String LOG_FILE_NAME = "cse.log";

    private final List<Path> candidates;

    public LogFileTail() {
        this.candidates = List.copyOf(buildCandidates());
    }

    public Snapshot read() {
        List<String> triedLabels = new ArrayList<>();
        for (Path path : candidates) {
            triedLabels.add(path.toString());
            if (!Files.isRegularFile(path)) {
                continue;
            }
            try {
                return readWindow(path, triedLabels);
            } catch (IOException ex) {
                return Snapshot.missing(triedLabels, "Cannot read log: " + ex.getMessage());
            }
        }
        return Snapshot.missing(triedLabels, null);
    }

    private static List<Path> buildCandidates() {
        Set<Path> unique = new LinkedHashSet<>();
        unique.add(resolveConfigured("./logs/" + LOG_FILE_NAME));
        String catalinaBase = System.getProperty("catalina.base");
        if (catalinaBase != null && !catalinaBase.isBlank()) {
            unique.add(Paths.get(catalinaBase, "logs", LOG_FILE_NAME).toAbsolutePath().normalize());
        }
        unique.add(new File(CseDisk.cseRoot(), "logs/" + LOG_FILE_NAME).toPath().toAbsolutePath().normalize());
        return new ArrayList<>(unique);
    }

    private static Path resolveConfigured(String configured) {
        Path path = Paths.get(configured);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir", ".")).resolve(path);
        }
        return path.toAbsolutePath().normalize();
    }

    private static Snapshot readWindow(Path path, List<String> tried) throws IOException {
        File file = path.toFile();
        long size = file.length();
        if (size <= 0) {
            return new Snapshot(path.toString(), "", 0, false, tried, null);
        }
        long start = Math.max(0, size - WINDOW_BYTES);
        boolean truncated = start > 0;
        byte[] raw;
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(start);
            int length = (int) (size - start);
            raw = new byte[length];
            raf.readFully(raw);
        }
        String text = new String(raw, StandardCharsets.UTF_8);
        if (truncated) {
            int newline = text.indexOf('\n');
            if (newline >= 0 && newline + 1 < text.length()) {
                text = text.substring(newline + 1);
            }
        }
        return new Snapshot(path.toString(), text, size, truncated, tried, null);
    }

    public static final class Snapshot {
        private final String path;
        private final String text;
        private final long fileBytes;
        private final boolean truncated;
        private final List<String> tried;
        private final String error;

        Snapshot(String path, String text, long fileBytes, boolean truncated,
                List<String> tried, String error) {
            this.path = path;
            this.text = text;
            this.fileBytes = fileBytes;
            this.truncated = truncated;
            this.tried = tried;
            this.error = error;
        }

        static Snapshot missing(List<String> tried, String error) {
            return new Snapshot(null, "", 0, false, tried, error);
        }

        public String getPath() {
            return path;
        }

        public String getText() {
            return text;
        }

        public long getFileBytes() {
            return fileBytes;
        }

        public boolean isTruncated() {
            return truncated;
        }

        public boolean isFound() {
            return path != null && error == null;
        }

        public List<String> getTried() {
            return tried;
        }

        public String getError() {
            return error;
        }
    }
}

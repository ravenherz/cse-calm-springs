package com.ravenherz.cse.controller;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the parts of one catalog file until every part has arrived, then writes them in order.
 */
final class ParallelUpload {

    private final Path root;
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    ParallelUpload() throws IOException {
        this.root = Files.createTempDirectory("cse-upload-");
    }

    Path accept(String resourceId, int partIndex, int partCount, long totalSize, MultipartFile part)
            throws IOException {
        Session session = sessions.computeIfAbsent(resourceId, id -> new Session(root.resolve(id), partCount, totalSize));
        synchronized (session) {
            if (session.finished || session.failed) {
                return null;
            }
            if (session.partCount != partCount || session.totalSize != totalSize) {
                session.failed = true;
                throw new IOException("Upload parts do not match");
            }
            if (partIndex < 0 || partIndex >= partCount) {
                session.failed = true;
                throw new IOException("Upload part is out of range");
            }
            Path dest = session.dir.resolve(Integer.toString(partIndex));
            Files.createDirectories(session.dir);
            try (InputStream in = part.getInputStream()) {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }
            session.received.add(partIndex);
            if (session.received.size() < partCount) {
                return null;
            }
            session.finished = true;
            return assemble(session);
        }
    }

    void cancel(String resourceId) {
        Session session = sessions.remove(resourceId);
        if (session == null) {
            return;
        }
        synchronized (session) {
            deleteTree(session.dir);
        }
    }

    private Path assemble(Session session) throws IOException {
        Path assembled = session.dir.resolve("file");
        try (OutputStream out = Files.newOutputStream(assembled)) {
            for (int i = 0; i < session.partCount; i++) {
                Path part = session.dir.resolve(Integer.toString(i));
                if (!Files.isRegularFile(part)) {
                    throw new IOException("Upload part " + i + " is missing");
                }
                Files.copy(part, out);
            }
        }
        if (Files.size(assembled) != session.totalSize) {
            throw new IOException("Upload parts do not match the file size");
        }
        return assembled;
    }

    private static void deleteTree(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (var paths = Files.walk(dir)) {
            paths.sorted((left, right) -> right.getNameCount() - left.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    private static final class Session {
        private final Path dir;
        private final int partCount;
        private final long totalSize;
        private final Set<Integer> received = new HashSet<>();
        private boolean finished;
        private boolean failed;

        private Session(Path dir, int partCount, long totalSize) {
            this.dir = dir;
            this.partCount = partCount;
            this.totalSize = totalSize;
        }
    }
}

package com.ravenherz.optideployer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadService.class);
    private static final Pattern UPLOAD_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,63}");

    private final OptiConfig config;
    private final ManagerClient manager;
    private final Object lock = new Object();
    private String activeId;
    private long totalSize;
    private boolean deploying;

    public UploadService(OptiConfig config, ManagerClient manager) {
        this.config = config;
        this.manager = manager;
    }

    public UploadResult accept(String secret, String uploadId, int partIndex, long totalSize, InputStream body)
            throws IOException {
        if (!config.matches(secret)) {
            return UploadResult.denied();
        }
        if (!validUploadId(uploadId, partIndex)) {
            return UploadResult.bad("uploadId");
        }
        if (partIndex < 0 || partIndex >= PartPlan.STREAMS) {
            return UploadResult.bad("partIndex");
        }
        if (totalSize < 0) {
            return UploadResult.bad("totalSize");
        }

        Path incoming = Files.createTempFile("opti-", ".part");
        boolean moved = false;
        boolean deployNow = false;
        try {
            Files.copy(body, incoming, StandardCopyOption.REPLACE_EXISTING);
            synchronized (lock) {
                if (deploying) {
                    if (uploadId.equals(activeId)) {
                        return UploadResult.stored();
                    }
                    return UploadResult.conflict("deploy in progress");
                }
                if (!uploadId.equals(activeId)) {
                    deleteParts();
                    activeId = uploadId;
                    this.totalSize = totalSize;
                } else if (this.totalSize != totalSize) {
                    return UploadResult.bad("totalSize mismatch");
                }
                Path dest = partFile(uploadId, partIndex);
                Files.createDirectories(dest.getParent());
                Files.move(incoming, dest, StandardCopyOption.REPLACE_EXISTING);
                moved = true;
                if (ready(uploadId)) {
                    deploying = true;
                    deployNow = true;
                }
            }
            if (!deployNow) {
                log.info("stored part {} of {}", partIndex, uploadId);
                return UploadResult.stored();
            }
            return finish(uploadId, totalSize);
        } finally {
            if (!moved) {
                Files.deleteIfExists(incoming);
            }
        }
    }

    private UploadResult finish(String uploadId, long totalSize) {
        Path partial = null;
        try {
            String problem = lengthProblem(uploadId, totalSize);
            if (problem != null) {
                return UploadResult.bad(problem);
            }
            Path war = Path.of(config.warPath());
            if (war.getParent() != null) {
                Files.createDirectories(war.getParent());
            }
            partial = war.resolveSibling(war.getFileName().toString() + ".partial");
            try (OutputStream out = Files.newOutputStream(partial)) {
                byte[] buffer = new byte[1024 * 1024];
                for (int i = 0; i < PartPlan.STREAMS; i++) {
                    try (InputStream in = Files.newInputStream(partFile(uploadId, i))) {
                        int read;
                        while ((read = in.read(buffer)) >= 0) {
                            out.write(buffer, 0, read);
                        }
                    }
                }
            }
            moveIntoPlace(partial, war);
            partial = null;
            log.info("deploying {} ({} bytes) at {}", war, totalSize, config.context());
            String text = manager.deploy(war);
            log.info("{}", text);
            return UploadResult.ok(text);
        } catch (Exception e) {
            log.warn("deploy failed: {}", e.getMessage());
            String message = e.getMessage() == null ? "deploy failed" : e.getMessage();
            return UploadResult.failed(message);
        } finally {
            if (partial != null) {
                try {
                    Files.deleteIfExists(partial);
                } catch (IOException e) {
                    log.warn("could not delete {}", partial);
                }
            }
            synchronized (lock) {
                deploying = false;
                activeId = null;
                try {
                    deleteParts();
                } catch (IOException e) {
                    log.warn("cleanup failed: {}", e.getMessage());
                }
            }
        }
    }

    private static void moveIntoPlace(Path partial, Path war) throws IOException {
        try {
            Files.move(partial, war, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(partial, war, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String lengthProblem(String uploadId, long totalSize) throws IOException {
        long[] expected = PartPlan.lengths(totalSize);
        for (int i = 0; i < PartPlan.STREAMS; i++) {
            long actual = Files.size(partFile(uploadId, i));
            if (actual != expected[i]) {
                return "part " + i + " is " + actual + " bytes, expected " + expected[i];
            }
        }
        return null;
    }

    private boolean ready(String uploadId) {
        for (int i = 0; i < PartPlan.STREAMS; i++) {
            if (Files.notExists(partFile(uploadId, i))) {
                return false;
            }
        }
        return true;
    }

    private Path partsRoot() {
        Path war = Path.of(config.warPath());
        Path parent = war.getParent() == null ? Path.of(".") : war.getParent();
        return parent.resolve(war.getFileName().toString() + ".parts");
    }

    private boolean validUploadId(String uploadId, int partIndex) {
        if (uploadId == null || !UPLOAD_ID.matcher(uploadId).matches()) {
            return false;
        }
        if (partIndex < 0 || partIndex >= PartPlan.STREAMS) {
            return true;
        }
        Path root = partsRoot().toAbsolutePath().normalize();
        Path dest = root.resolve(uploadId).resolve(partIndex + ".part").normalize();
        return dest.startsWith(root);
    }

    private Path partFile(String uploadId, int index) {
        Path root = partsRoot().toAbsolutePath().normalize();
        Path dest = root.resolve(uploadId).resolve(index + ".part").normalize();
        if (!dest.startsWith(root)) {
            throw new IllegalArgumentException("uploadId");
        }
        return dest;
    }

    private void deleteParts() throws IOException {
        Path root = partsRoot();
        if (Files.notExists(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

}

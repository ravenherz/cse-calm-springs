package com.ravenherz.cse.util.video;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

final class FfmpegProcess {

    private static final int MAX_CAPTURE = 64 * 1024;

    private FfmpegProcess() {
    }

    static String stdout(List<String> command, Duration timeout) throws IOException {
        return execute(command, timeout, false, null);
    }

    static void run(List<String> command, Duration timeout) throws IOException {
        execute(command, timeout, true, null);
    }

    static void run(List<String> command, Duration timeout, Consumer<String> line) throws IOException {
        execute(command, timeout, false, line);
    }

    private static String execute(List<String> command, Duration timeout, boolean mergeError,
            Consumer<String> line) throws IOException {
        if (command == null || command.isEmpty()) {
            throw new IOException("FFmpeg command is empty");
        }
        boolean merge = mergeError && line == null;
        ProcessBuilder builder = new ProcessBuilder(command);
        quietEnvironment(builder);
        builder.redirectErrorStream(merge);
        Process process = builder.start();
        try {
            process.getOutputStream().close();
        } catch (IOException ignored) {
        }
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        StringBuilder errTail = new StringBuilder();
        Thread outThread = line == null
                ? drain(process.getInputStream(), stdout)
                : drainLines(process.getInputStream(), line);
        Thread errThread = merge ? null : drainLines(process.getErrorStream(), stderrLine -> {
            if (errTail.length() < MAX_CAPTURE) {
                errTail.append(stderrLine).append('\n');
                if (errTail.length() > MAX_CAPTURE) {
                    errTail.setLength(MAX_CAPTURE);
                }
            }
            if (line != null) {
                line.accept(stderrLine);
            }
        });
        outThread.start();
        if (errThread != null) {
            errThread.start();
        }
        long millis = timeout == null ? TimeUnit.HOURS.toMillis(2) : Math.max(1L, timeout.toMillis());
        try {
            if (!process.waitFor(millis, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new IOException("Timed out: " + command.get(0));
            }
            outThread.join(1000);
            if (errThread != null) {
                errThread.join(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("Interrupted", e);
        }
        if (process.exitValue() != 0) {
            String detail = merge ? stdout.toString(StandardCharsets.UTF_8) : errTail.toString();
            throw new IOException(command.get(0) + " exited " + process.exitValue()
                    + (detail.isBlank() ? "" : ": " + trim(detail)));
        }
        return stdout.toString(StandardCharsets.UTF_8);
    }

    static void quietEnvironment(ProcessBuilder builder) {
        Map<String, String> env = builder.environment();
        env.remove("DISPLAY");
        env.remove("WAYLAND_DISPLAY");
        env.put("PULSE_SERVER", "none");
        env.put("SDL_AUDIODRIVER", "dummy");
        env.put("SDL_VIDEODRIVER", "dummy");
        env.put("LIBVA_DRIVER_NAME", "off");
        env.put("LIBVA_DRIVERS_PATH", nonexistent());
        try {
            Path lib = FfmpegBinaries.libraryDir();
            String path = lib.toAbsolutePath().toString();
            if (FfmpegBinaries.windows()) {
                prepend(env, "PATH", path);
            } else {
                prepend(env, "LD_LIBRARY_PATH", path);
            }
        } catch (IOException ignored) {
        }
    }

    private static void prepend(Map<String, String> env, String key, String value) {
        String current = env.get(key);
        if (current == null || current.isBlank()) {
            env.put(key, value);
            return;
        }
        String sep = FfmpegBinaries.windows() ? ";" : ":";
        if (current.startsWith(value + sep) || current.equals(value)) {
            return;
        }
        env.put(key, value + sep + current);
    }

    private static String nonexistent() {
        return FfmpegBinaries.windows() ? "NUL" : "/var/empty/cse-no-va";
    }

    private static Thread drain(InputStream in, ByteArrayOutputStream into) {
        Thread thread = new Thread(() -> {
            try {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) >= 0) {
                    int room = MAX_CAPTURE - into.size();
                    if (room <= 0) {
                        continue;
                    }
                    into.write(buf, 0, Math.min(n, room));
                }
            } catch (IOException ignored) {
            }
        }, "cse-ffmpeg-io");
        thread.setDaemon(true);
        return thread;
    }

    private static Thread drainLines(InputStream in, Consumer<String> lines) {
        Thread thread = new Thread(() -> {
            try {
                byte[] buf = new byte[4096];
                StringBuilder acc = new StringBuilder();
                int n;
                while ((n = in.read(buf)) >= 0) {
                    for (int i = 0; i < n; i++) {
                        char c = (char) (buf[i] & 0xff);
                        if (c == '\n' || c == '\r') {
                            if (acc.length() > 0) {
                                lines.accept(acc.toString().trim());
                                acc.setLength(0);
                            }
                        } else {
                            acc.append(c);
                        }
                    }
                }
                if (acc.length() > 0) {
                    lines.accept(acc.toString().trim());
                }
            } catch (IOException ignored) {
            }
        }, "cse-ffmpeg-progress");
        thread.setDaemon(true);
        return thread;
    }

    private static String trim(String raw) {
        String text = raw.replaceAll("\\s+", " ").trim();
        return text.length() > 400 ? text.substring(0, 400) : text;
    }
}

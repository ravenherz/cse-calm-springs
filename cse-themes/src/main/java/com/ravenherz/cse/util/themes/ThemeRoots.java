package com.ravenherz.cse.util.themes;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Exploded theme packs and installed app pages. The WAR binds this to the instance disk
 * after the context root is known.
 */
public final class ThemeRoots {

    public interface Paths {
        Path themesDir() throws IOException;

        Path appsDir() throws IOException;
    }

    private static volatile Paths paths;

    private ThemeRoots() {
    }

    public static void bind(Paths paths) {
        ThemeRoots.paths = paths;
    }

    public static void resetForTests() {
        paths = null;
    }

    public static Path themesDir() throws IOException {
        return current().themesDir();
    }

    public static Path appsDir() throws IOException {
        return current().appsDir();
    }

    private static Paths current() throws IOException {
        Paths current = paths;
        if (current == null) {
            throw new IOException("Theme roots are not bound");
        }
        return current;
    }
}

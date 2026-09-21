package com.ravenherz.cse.util.staticapps;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Exploded app packs. The WAR binds this to the instance disk after the context root is known.
 */
public final class AppRoots {

    public interface Paths {
        Path appsDir() throws IOException;
    }

    private static volatile Paths paths;

    private AppRoots() {
    }

    public static void bind(Paths paths) {
        AppRoots.paths = paths;
    }

    public static void resetForTests() {
        paths = null;
    }

    public static Path appsDir() throws IOException {
        Paths current = paths;
        if (current == null) {
            throw new IOException("App roots are not bound");
        }
        return current.appsDir();
    }
}

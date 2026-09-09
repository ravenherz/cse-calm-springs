package com.ravenherz.cse.util.staticapps;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppInstallSlugTest {

    @Test
    void manifestSlugIsTheInstallPrefix() {
        AppManifest manifest = new AppManifest("Fretboard Lab", "1.0", "fretlab");
        assertEquals("fretlab", AppInstallSlug.resolve(null, manifest));
        assertEquals("fretlab", AppInstallSlug.resolve("  ", manifest));
    }

    @Test
    void formSlugOverridesManifest() {
        AppManifest manifest = new AppManifest("Fretboard Lab", "1.0", "fretlab");
        assertEquals("lab-copy", AppInstallSlug.resolve("Lab-Copy", manifest));
    }

    @Test
    void filenameIsNotConsulted() {
        assertEquals("", AppInstallSlug.resolve(null, null));
        assertEquals("", AppInstallSlug.resolve("", new AppManifest("Hello", "1.0", null)));
    }
}

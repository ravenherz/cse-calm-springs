package com.ravenherz.cse.util.themes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThemeDiskResourcesTest {

    @Test
    void acceptsExplodedThemeAssets() {
        assertTrue(ThemeDiskResources.isIsolatedThemePath("modern/js/jquery.min.js"));
        assertTrue(ThemeDiskResources.isIsolatedThemePath("client/css/styles.css"));
        assertTrue(ThemeDiskResources.isIsolatedThemePath("2000s/fonts/inter-latin-wght-normal.woff2"));
    }

    @Test
    void rejectsEscapesAndEngineFolders() {
        assertFalse(ThemeDiskResources.isIsolatedThemePath("../evil.css"));
        assertFalse(ThemeDiskResources.isIsolatedThemePath(".modern.tmp/js/jquery.min.js"));
        assertFalse(ThemeDiskResources.isIsolatedThemePath("admin/secret.css"));
        assertFalse(ThemeDiskResources.isIsolatedThemePath("fragments/shared.html"));
        assertFalse(ThemeDiskResources.isIsolatedThemePath(""));
    }
}

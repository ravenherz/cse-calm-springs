package com.ravenherz.cse.util.staticapps;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AppManifestTest {

    @Test
    void parsesIdentityAndPublisherFields() {
        AppManifest manifest = AppManifest.parse("""
                {
                  "slug": "admin",
                  "name": "Calm Springs admin",
                  "version": "0.6.0",
                  "author": "A. Kolesnikov",
                  "company": "ravenherz",
                  "description": "Editor for pages, media, themes, and site settings"
                }
                """);
        assertNotNull(manifest);
        assertEquals("admin", manifest.getSlug());
        assertEquals("Calm Springs admin", manifest.getName());
        assertEquals("0.6.0", manifest.getVersion());
        assertEquals("A. Kolesnikov", manifest.getAuthor());
        assertEquals("ravenherz", manifest.getCompany());
        assertEquals("Editor for pages, media, themes, and site settings", manifest.getDescription());
    }

    @Test
    void olderManifestsStayValidWithoutPublisherFields() {
        AppManifest manifest = AppManifest.parse("""
                { "slug": "setup", "name": "Setup", "version": "1.0.0" }
                """);
        assertNotNull(manifest);
        assertEquals("setup", manifest.getSlug());
        assertNull(manifest.getAuthor());
        assertNull(manifest.getCompany());
        assertNull(manifest.getDescription());
    }
}

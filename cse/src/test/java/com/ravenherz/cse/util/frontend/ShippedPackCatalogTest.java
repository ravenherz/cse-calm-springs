package com.ravenherz.cse.util.frontend;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShippedPackCatalogTest {

    @Test
    void warShipsFrontendPacks() {
        Set<String> themes = ShippedPackCatalog.themes().stream()
                .map(ShippedPackCatalog.Pack::stem)
                .collect(Collectors.toSet());
        assertTrue(themes.contains("modern"), themes.toString());
        assertFalse(themes.contains("paper"), themes.toString());
        assertFalse(themes.contains("client"), themes.toString());
        assertFalse(themes.contains("hall"), themes.toString());
        assertFalse(themes.contains("2000s"), themes.toString());

        Set<String> apps = ShippedPackCatalog.apps().stream()
                .map(ShippedPackCatalog.Pack::stem)
                .collect(Collectors.toSet());
        assertTrue(apps.contains("setup"), apps.toString());
        assertTrue(apps.contains("admin"), apps.toString());
        assertTrue(apps.contains("login"), apps.toString());
        assertFalse(apps.contains("projects"), apps.toString());
    }

    @Test
    void shippedModernPackIncludesIsolatedRuntime() throws IOException {
        byte[] bytes = ShippedPackCatalog.themes().stream()
                .filter(pack -> "modern".equals(pack.stem()))
                .findFirst()
                .orElseThrow()
                .bytes();
        Set<String> names = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                names.add(entry.getName().replace('\\', '/'));
            }
        }
        assertTrue(names.contains("js/jquery.min.js"), names.toString());
        assertTrue(names.contains("js/cse-lp.js"), names.toString());
        assertTrue(names.contains("css/fonts.css"), names.toString());
        assertTrue(names.contains("fonts/outfit-latin-wght-normal.woff2"), names.toString());
        assertFalse(names.stream().anyMatch(name -> name.startsWith("js/dist/")), names.toString());
    }
}

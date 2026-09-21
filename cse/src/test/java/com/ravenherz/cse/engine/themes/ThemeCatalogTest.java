package com.ravenherz.cse.engine.themes;

import com.ravenherz.cse.util.themes.ShippedThemes;
import com.ravenherz.cse.util.themes.ThemeCatalog;
import com.ravenherz.cse.util.themes.ThemeInfo;
import com.ravenherz.cse.util.themes.ThemeRoots;
import com.ravenherz.cse.util.themes.ThemeSelection;

import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.dal.dao.ThemeService;
import com.ravenherz.cse.dal.dto.ThemeEntity;
import com.ravenherz.cse.dal.dto.basic.ThemeData;
import com.ravenherz.cse.engine.util.Settings;
import com.ravenherz.cse.util.frontend.ShippedPackCatalog;
import com.ravenherz.cse.util.io.CseDisk;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ObjectProvider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ThemeCatalogTest {

    private Path temp;
    private String previousRoot;
    private Settings settings;
    private ThemeCatalog catalog;

    @BeforeEach
    void bindTempMachineRoot() throws Exception {
        previousRoot = System.getProperty("cse.disk.root");
        temp = Files.createTempDirectory("cse-themes-catalog");
        System.setProperty("cse.disk.root", temp.toString());
        CseDisk.resetForTests();
        CseDisk.bindInstanceSlug("theme-test");
        ThemeRoots.bind(new ThemeRoots.Paths() {
            @Override
            public Path themesDir() throws IOException {
                return CseDisk.themesDir().toPath();
            }

            @Override
            public Path appsDir() throws IOException {
                return CseDisk.staticPagesDir().toPath();
            }
        });
        settings = mock(Settings.class);
        when(settings.getValue(any(), any())).thenReturn(null);
        catalog = new ThemeCatalog(settings, null, shippedPacks());
    }

    @AfterEach
    void restore() {
        ThemeRoots.resetForTests();
        CseDisk.resetForTests();
        if (previousRoot == null) {
            System.clearProperty("cse.disk.root");
        } else {
            System.setProperty("cse.disk.root", previousRoot);
        }
    }

    @Test
    void emptyDiskCatalogIsExactlyTheShippedPacks() {
        Set<String> shipped = ShippedPackCatalog.themes().stream()
                .map(ShippedPackCatalog.Pack::stem)
                .collect(Collectors.toSet());
        Set<String> listed = catalog.list().stream()
                .map(ThemeInfo::getId)
                .map(id -> id.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        assertEquals(shipped, listed);
        assertTrue(shipped.contains("modern"), shipped.toString());
        assertFalse(listed.contains("paper"));
        assertFalse(listed.contains("client"));
        assertFalse(listed.contains("hall"));
        ThemeInfo modern = catalog.find("modern");
        assertNotNull(modern);
        assertTrue(modern.isBuiltin());
        assertEquals("modern", modern.getShell());
        assertTrue(modern.getSchemas().contains("linen"));
        assertEquals("thymeleaf", modern.getShellKind());
    }

    @Test
    void unknownSelectionFallsBackToModernLinen() {
        ThemeSelection selection = catalog.resolve();
        assertEquals("modern", selection.getCssId());
        assertEquals("linen", selection.getSchemaId());
        assertEquals("modern", selection.getShellId());
    }

    @Test
    void diskOverlayIsListedAndResolved() throws Exception {
        Path dir = CseDisk.themesDir().toPath().resolve("parchment");
        Files.createDirectories(dir.resolve("css").resolve("color-schemas"));
        Files.writeString(dir.resolve("theme.json"), """
                {
                  "id": "parchment",
                  "title": "Parchment",
                  "shell": "modern",
                  "defaultSchema": "ivory"
                }
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("css").resolve("styles.css"), "body{}", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("css").resolve("color-schemas").resolve("ivory.css"),
                ":root{}", StandardCharsets.UTF_8);

        assertTrue(catalog.isKnownTheme("parchment"));
        ThemeInfo parchment = catalog.find("parchment");
        assertNotNull(parchment);
        assertFalse(parchment.isBuiltin());
        assertEquals("modern", parchment.getShell());
        assertTrue(parchment.getSchemas().contains("ivory"));
        assertTrue(catalog.isKnownSchema("parchment", "ivory"));

        when(settings.getValue(eq(SettingKeys.CONTEXT_DATASOURCE_VIEW),
                eq(SettingKeys.KEY_STYLES_THEME))).thenReturn("parchment");
        when(settings.getValue(eq(SettingKeys.CONTEXT_DATASOURCE_VIEW),
                eq(SettingKeys.KEY_STYLES_SCHEMA))).thenReturn("ivory");
        ThemeSelection selection = catalog.resolve();
        assertEquals("parchment", selection.getCssId());
        assertEquals("ivory", selection.getSchemaId());
        assertEquals("modern", selection.getShellId());
    }

    @Test
    void diskOwnShellUsesThemeIdAsShell() throws Exception {
        Path dir = CseDisk.themesDir().toPath().resolve("folio");
        Files.createDirectories(dir.resolve("css").resolve("color-schemas"));
        Files.writeString(dir.resolve("theme.json"), """
                {
                  "id": "folio",
                  "title": "Folio",
                  "shell": "own",
                  "defaultSchema": "ivory"
                }
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("css").resolve("styles.css"), "body{}", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("css").resolve("color-schemas").resolve("ivory.css"),
                ":root{}", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("index.html"), "<body id=\"page-body\"></body>", StandardCharsets.UTF_8);

        assertTrue(catalog.isKnownTheme("folio"));
        ThemeInfo folio = catalog.find("folio");
        assertNotNull(folio);
        assertTrue(folio.isOwnShell());
        assertEquals("thymeleaf", folio.getShellKind());
        when(settings.getValue(eq(SettingKeys.CONTEXT_DATASOURCE_VIEW),
                eq(SettingKeys.KEY_STYLES_THEME))).thenReturn("folio");
        when(settings.getValue(eq(SettingKeys.CONTEXT_DATASOURCE_VIEW),
                eq(SettingKeys.KEY_STYLES_SCHEMA))).thenReturn("ivory");
        ThemeSelection selection = catalog.resolve();
        assertEquals("folio", selection.getCssId());
        assertEquals("ivory", selection.getSchemaId());
        assertEquals("folio", selection.getShellId());
    }

    @Test
    void diskJsShellUsesThemeIdAsShell() throws Exception {
        Path dir = CseDisk.themesDir().toPath().resolve("plainjs");
        Files.createDirectories(dir.resolve("css").resolve("color-schemas"));
        Files.createDirectories(dir.resolve("js"));
        Files.writeString(dir.resolve("theme.json"), """
                {
                  "id": "plainjs",
                  "title": "Plain JS",
                  "shell": "js",
                  "defaultSchema": "ink"
                }
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("css").resolve("styles.css"), "body{}", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("css").resolve("color-schemas").resolve("ink.css"),
                ":root{}", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("index.html"), "<body id=\"page-body\"></body>", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("js").resolve("theme.js"), "void 0;", StandardCharsets.UTF_8);

        assertTrue(catalog.isKnownTheme("plainjs"));
        ThemeInfo client = catalog.find("plainjs");
        assertNotNull(client);
        assertTrue(ThemeCatalog.isJsShell(client.getShell()));
        assertEquals("js", client.getShellKind());
        when(settings.getValue(eq(SettingKeys.CONTEXT_DATASOURCE_VIEW),
                eq(SettingKeys.KEY_STYLES_THEME))).thenReturn("plainjs");
        when(settings.getValue(eq(SettingKeys.CONTEXT_DATASOURCE_VIEW),
                eq(SettingKeys.KEY_STYLES_SCHEMA))).thenReturn("ink");
        ThemeSelection selection = catalog.resolve();
        assertEquals("plainjs", selection.getCssId());
        assertEquals("ink", selection.getSchemaId());
        assertEquals("plainjs", selection.getShellId());
    }

    @Test
    void leftoverDiskThemeIsHiddenWhenNotInMongo() throws Exception {
        writeDiskOverlay("hall", "js");
        assertTrue(catalog.isKnownTheme("hall"));
        ThemeService service = mock(ThemeService.class);
        when(service.getByThemeId(any())).thenReturn(null);
        @SuppressWarnings("unchecked")
        ObjectProvider<ThemeService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(service);
        ThemeCatalog gated = new ThemeCatalog(settings, provider, shippedPacks());
        assertFalse(gated.isKnownTheme("hall"));
        assertTrue(gated.isKnownTheme("modern"));
    }

    @Test
    void mongoBackedDiskThemeStaysListed() throws Exception {
        writeDiskOverlay("parchment", "modern");
        ThemeData data = new ThemeData();
        data.setThemeId("parchment");
        ThemeEntity entity = mock(ThemeEntity.class);
        when(entity.getThemeData()).thenReturn(data);
        ThemeService service = mock(ThemeService.class);
        when(service.getByThemeId("parchment")).thenReturn(entity);
        @SuppressWarnings("unchecked")
        ObjectProvider<ThemeService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(service);
        ThemeCatalog gated = new ThemeCatalog(settings, provider, shippedPacks());
        assertTrue(gated.isKnownTheme("parchment"));
    }

    private static ShippedThemes shippedPacks() {
        return () -> {
            List<ShippedThemes.Pack> packs = new ArrayList<>();
            for (ShippedPackCatalog.Pack pack : ShippedPackCatalog.themes()) {
                packs.add(new ShippedThemes.Pack(pack.stem(), pack.bytes()));
            }
            return packs;
        };
    }

    private static void writeDiskOverlay(String id, String shell) throws Exception {
        Path dir = CseDisk.themesDir().toPath().resolve(id);
        Files.createDirectories(dir.resolve("css").resolve("color-schemas"));
        Files.writeString(dir.resolve("theme.json"), """
                {
                  "id": "%s",
                  "title": "%s",
                  "shell": "%s",
                  "defaultSchema": "ivory"
                }
                """.formatted(id, id, shell), StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("css").resolve("styles.css"), "body{}", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("css").resolve("color-schemas").resolve("ivory.css"),
                ":root{}", StandardCharsets.UTF_8);
        if ("js".equals(shell)) {
            Files.createDirectories(dir.resolve("js"));
            Files.writeString(dir.resolve("index.html"), "<body id=\"page-body\"></body>", StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("js").resolve("theme.js"), "void 0;", StandardCharsets.UTF_8);
        }
    }
}

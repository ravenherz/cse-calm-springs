package com.ravenherz.cse.util.themes;

import com.ravenherz.cse.util.io.CseDisk;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThemePackDeployerTest {

    private Path temp;
    private String previousRoot;
    private final ThemePackDeployer deployer = new ThemePackDeployer();

    @BeforeEach
    void bindTempMachineRoot() throws Exception {
        previousRoot = System.getProperty("cse.disk.root");
        temp = Files.createTempDirectory("cse-themes-pack");
        System.setProperty("cse.disk.root", temp.toString());
        CseDisk.resetForTests();
        CseDisk.bindInstanceSlug("theme-pack");
    }

    @AfterEach
    void restore() {
        CseDisk.resetForTests();
        if (previousRoot == null) {
            System.clearProperty("cse.disk.root");
        } else {
            System.setProperty("cse.disk.root", previousRoot);
        }
    }

    @Test
    void reservedIdsCannotBeInstalled() {
        assertThrows(IllegalArgumentException.class, () -> deployer.validateId("modern"));
        assertThrows(IllegalArgumentException.class, () -> deployer.validateId("admin"));
        assertDoesNotThrow(() -> deployer.validateId("paper"));
    }

    @Test
    void engineThemeCanDeployReservedId() throws Exception {
        byte[] zip = zip(validFiles("modern"));
        assertThrows(IllegalArgumentException.class, () -> deployer.deploy("modern", zip));
        deployer.deployEngineTheme("modern", zip);
        Path dest = CseDisk.themesDir().toPath().resolve("modern");
        assertTrue(Files.isRegularFile(dest.resolve("theme.json")));
        assertTrue(Files.isRegularFile(dest.resolve("css").resolve("styles.css")));
    }

    @Test
    void extraHtmlAndScriptsAreRejected() throws Exception {
        Map<String, String> files = validFiles("paper");
        files.put("extra.html", "<html></html>");
        byte[] zip = zip(files);
        IOException ex = assertThrows(IOException.class, () -> deployer.validateZip(zip));
        assertTrue(ex.getMessage().toLowerCase().contains("html")
                || ex.getMessage().toLowerCase().contains("script"), ex.getMessage());
    }

    @Test
    void ownShellRequiresRootIndex() throws Exception {
        Map<String, String> files = validFiles("folio");
        files.put("theme.json", """
                {
                  "id": "folio",
                  "title": "Folio",
                  "shell": "own",
                  "defaultSchema": "ivory"
                }
                """);
        IOException missing = assertThrows(IOException.class, () -> deployer.validateZip(zip(files)));
        assertTrue(missing.getMessage().toLowerCase().contains("index.html"), missing.getMessage());

        files.put("index.html", "<body id=\"page-body\"></body>");
        deployer.validateZip(zip(files));
        deployer.deploy("folio", zip(files));
        Path dest = CseDisk.themesDir().toPath().resolve("folio");
        assertTrue(Files.isRegularFile(dest.resolve("index.html")));
    }

    @Test
    void adminAndFragmentsFoldersAreRejected() throws Exception {
        Map<String, String> files = validFiles("paper");
        files.put("admin/secret.css", "body{}");
        assertThrows(IOException.class, () -> deployer.validateZip(zip(files)));
    }

    @Test
    void missingStylesAreRejected() throws Exception {
        Map<String, String> files = validFiles("paper");
        files.remove("css/styles.css");
        byte[] zip = zip(files);
        IOException ex = assertThrows(IOException.class, () -> deployer.validateZip(zip));
        assertTrue(ex.getMessage().contains("styles.css"), ex.getMessage());
    }

    @Test
    void zipSlipIsRejected() throws Exception {
        Map<String, String> files = validFiles("paper");
        files.put("../evil.css", "body{}");
        byte[] zip = zip(files);
        assertThrows(IOException.class, () -> deployer.validateZip(zip));
    }

    @Test
    void validPackDeploysAndUnwraps() throws Exception {
        byte[] zip = zip(validFiles("paper"));
        deployer.validateZip(zip);
        deployer.deploy("paper", zip);
        Path dest = CseDisk.themesDir().toPath().resolve("paper");
        assertTrue(Files.isRegularFile(dest.resolve("theme.json")));
        assertTrue(Files.isRegularFile(dest.resolve("css").resolve("styles.css")));
        assertTrue(Files.isRegularFile(dest.resolve("css").resolve("cse-player.css")));
        assertTrue(Files.isRegularFile(dest.resolve("css").resolve("color-schemas").resolve("ivory.css")));

        Map<String, String> wrapped = new LinkedHashMap<>();
        validFiles("paper").forEach((name, value) -> wrapped.put("paper/" + name, value));
        deployer.deploy("paper", zip(wrapped));
        assertTrue(Files.isRegularFile(dest.resolve("theme.json")));

        deployer.undeploy("paper");
        assertFalse(Files.exists(dest));
    }

    @Test
    void retainOnlyRemovesExplodedTreesNotKept() throws Exception {
        deployer.deploy("paper", zip(validFiles("paper")));
        Path paper = CseDisk.themesDir().toPath().resolve("paper");
        assertTrue(Files.isDirectory(paper));
        deployer.retainOnly(Set.of("modern"));
        assertFalse(Files.exists(paper));
    }

    @Test
    void jsShellAllowsThemeScript() throws Exception {
        Map<String, String> files = validFiles("js");
        files.put("theme.json", """
                {
                  "id": "js",
                  "title": "JS",
                  "shell": "js",
                  "defaultSchema": "ink"
                }
                """);
        files.put("index.html", "<body id=\"page-body\"></body>");
        files.put("js/theme.js", "console.log('ok');");
        deployer.validateZip(zip(files));
        deployer.deploy("js", zip(files));
        Path dest = CseDisk.themesDir().toPath().resolve("js");
        assertTrue(Files.isRegularFile(dest.resolve("js").resolve("theme.js")));
    }

    @Test
    void jsShellRequiresThemeScript() throws Exception {
        Map<String, String> files = validFiles("plainjs");
        files.put("theme.json", """
                {
                  "id": "plainjs",
                  "title": "JS",
                  "shell": "js",
                  "defaultSchema": "ink"
                }
                """);
        files.put("index.html", "<body id=\"page-body\"></body>");
        IOException ex = assertThrows(IOException.class, () -> deployer.validateZip(zip(files)));
        assertTrue(ex.getMessage().contains("js/theme.js"), ex.getMessage());
    }

    @Test
    void overlayPackMayIncludeSharedScripts() throws Exception {
        Map<String, String> files = validFiles("paper");
        files.put("js/jquery.min.js", "window.jQuery=function(){};");
        deployer.validateZip(zip(files));
        deployer.deploy("paper", zip(files));
        Path dest = CseDisk.themesDir().toPath().resolve("paper");
        assertTrue(Files.isRegularFile(dest.resolve("js").resolve("jquery.min.js")));
    }

    @Test
    void thymeleafShellMayIncludeSharedScripts() throws Exception {
        Map<String, String> files = validFiles("modern");
        files.put("js/jquery.min.js", "window.jQuery=function(){};");
        deployer.deployEngineTheme("modern", zip(files));
        Path dest = CseDisk.themesDir().toPath().resolve("modern");
        assertTrue(Files.isRegularFile(dest.resolve("js").resolve("jquery.min.js")));
    }

    @Test
    void nestedScriptsAreRejected() throws Exception {
        Map<String, String> files = validFiles("paper");
        files.put("js/dist/jquery.min.js", "window.jQuery=function(){};");
        IOException ex = assertThrows(IOException.class, () -> deployer.validateZip(zip(files)));
        assertTrue(ex.getMessage().toLowerCase().contains("js"), ex.getMessage());
    }

    private static Map<String, String> validFiles(String id) {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("theme.json", """
                {
                  "id": "%s",
                  "title": "Paper",
                  "shell": "modern",
                  "defaultSchema": "ivory"
                }
                """.formatted(id));
        files.put("css/styles.css", "body{}");
        files.put("css/cse-player.css", ".cse-player{}");
        files.put("css/color-schemas/ivory.css", ":root{}");
        return files;
    }

    private static byte[] zip(Map<String, String> files) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
            for (Map.Entry<String, String> entry : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return buffer.toByteArray();
    }
}

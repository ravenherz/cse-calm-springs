package com.ravenherz.cse.util.themes;

import com.ravenherz.cse.util.io.CseDisk;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiskThemeTemplateResolverTest {

    private Path temp;
    private String previousRoot;
    private final DiskThemeTemplateResolver resolver = new DiskThemeTemplateResolver();

    @BeforeEach
    void bindTempMachineRoot() throws Exception {
        previousRoot = System.getProperty("cse.disk.root");
        temp = Files.createTempDirectory("cse-disk-templates");
        System.setProperty("cse.disk.root", temp.toString());
        CseDisk.resetForTests();
        CseDisk.bindInstanceSlug("disk-templates");
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
    void themeShellComesFromExplodedPack() throws Exception {
        Path index = CseDisk.themesDir().toPath().resolve("modern").resolve("index.html");
        Files.createDirectories(index.getParent());
        Files.writeString(index, "<html></html>", StandardCharsets.UTF_8);

        Path file = resolver.fileFor("/modern/index");
        assertNotNull(file);
        assertTrue(Files.isSameFile(index, file));
        assertNull(resolver.fileFor("modern/styles"));
    }

    @Test
    void editorTemplatesComeFromExplodedAdminApp() throws Exception {
        Path admin = CseDisk.staticPagesDir().toPath().resolve("admin");
        Path page = admin.resolve("admin").resolve("editor-pages-list.html");
        Path fragment = admin.resolve("fragments").resolve("shared.html");
        Files.createDirectories(page.getParent());
        Files.createDirectories(fragment.getParent());
        Files.writeString(page, "<html></html>", StandardCharsets.UTF_8);
        Files.writeString(fragment, "<div></div>", StandardCharsets.UTF_8);

        Path foundPage = resolver.fileFor("/admin/editor-pages-list");
        Path foundFragment = resolver.fileFor("fragments/shared");
        assertNotNull(foundPage);
        assertNotNull(foundFragment);
        assertTrue(Files.isSameFile(page, foundPage));
        assertTrue(Files.isSameFile(fragment, foundFragment));
        assertNull(resolver.fileFor("admin/missing"));
        assertNull(resolver.fileFor("../admin/editor-pages-list"));
    }
}

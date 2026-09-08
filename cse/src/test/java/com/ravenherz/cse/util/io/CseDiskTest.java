package com.ravenherz.cse.util.io;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CseDiskTest {

    private Path temp;
    private String previousRoot;

    @BeforeEach
    void bindTempMachineRoot() throws Exception {
        previousRoot = System.getProperty("cse.disk.root");
        temp = Files.createTempDirectory("cse-disk");
        System.setProperty("cse.disk.root", temp.toString());
        CseDisk.resetForTests();
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
    void contextPathBecomesInstanceDir() {
        assertEquals("rhz-we", CseDisk.slugFromContextPath("/rhz-we"));
        assertEquals("wiztest", CseDisk.slugFromContextPath("/wiztest/"));
        assertEquals("ROOT", CseDisk.slugFromContextPath(""));
        assertEquals("ROOT", CseDisk.slugFromContextPath("/"));
        CseDisk.bindInstanceSlug(CseDisk.slugFromContextPath("/rhz-we"));
        assertEquals(temp.resolve("rhz-we").toFile().getAbsoluteFile(),
                CseDisk.cseRoot().getAbsoluteFile());
        assertEquals(temp.toFile().getAbsoluteFile(), CseDisk.machineRoot().getAbsoluteFile());
        assertTrue(CseDisk.contentCacheDir().getAbsolutePath().replace('\\', '/')
                .endsWith("rhz-we/content-cache"));
        assertTrue(CseDisk.instanceConfigurationDir().getAbsolutePath().replace('\\', '/')
                .endsWith("rhz-we/content-private/configuration"));
    }

    @Test
    void themesDirLivesUnderContentCache() throws Exception {
        CseDisk.bindInstanceSlug(CseDisk.slugFromContextPath("/rhz-we"));
        String path = CseDisk.themesDir().getAbsolutePath().replace('\\', '/');
        assertTrue(path.endsWith("rhz-we/content-cache/themes"), path);
    }
}

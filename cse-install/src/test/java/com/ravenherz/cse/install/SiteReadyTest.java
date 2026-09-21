package com.ravenherz.cse.install;

import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.dal.DataProvider;
import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.dao.AccountService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SiteReadyTest {

    private Path temp;
    private TempDisk disk;
    private DataProvider dataProvider;
    private AccountService accountService;
    private SiteReady siteReady;

    @BeforeEach
    void diskAndMocks() throws Exception {
        temp = Files.createTempDirectory("cse-site-ready");
        disk = new TempDisk(temp);
        dataProvider = mock(DataProvider.class);
        accountService = mock(AccountService.class);
        ServiceProvider serviceProvider = mock(ServiceProvider.class);
        when(serviceProvider.getAccountService()).thenReturn(accountService);
        siteReady = new SiteReady(dataProvider, serviceProvider, disk);
    }

    @AfterEach
    void restore() {
        System.clearProperty("cse.force.setup");
    }

    @Test
    void noBindingIsNotConfigured() {
        assertFalse(siteReady.hasDbmsBinding());
        assertFalse(siteReady.isConfigured());
        assertFalse(siteReady.evaluateAtBoot());
    }

    @Test
    void secretFilesCountAsBinding() throws Exception {
        writeSecrets();
        assertTrue(siteReady.hasDbmsBinding());
        assertFalse(siteReady.isConfigured());
    }

    @Test
    void installerHeldOpenStaysUnconfiguredAfterOwner() throws Exception {
        writeSecrets();
        when(dataProvider.ping()).thenReturn(true);
        when(accountService.getAllAccounts()).thenReturn(List.of(new AccountEntity()));
        siteReady.holdInstallerOpen();
        assertFalse(siteReady.isConfigured());
        siteReady.markFinished();
        assertTrue(siteReady.isConfigured());
        assertTrue(disk.siteReadyMarkerPresent());
    }

    @Test
    void bootWithOwnerWritesMarkerAndIsReady() throws Exception {
        writeSecrets();
        when(dataProvider.ping()).thenReturn(true);
        when(accountService.getAllAccounts()).thenReturn(List.of(new AccountEntity()));
        assertTrue(siteReady.evaluateAtBoot());
        assertTrue(siteReady.isConfigured());
        assertTrue(disk.siteReadyMarkerPresent());
    }

    @Test
    void markerPlusBindingIsReadyWithoutPing() throws Exception {
        writeSecrets();
        assertTrue(disk.writeSiteReadyMarker());
        when(dataProvider.ping()).thenReturn(false);
        assertTrue(siteReady.evaluateAtBoot());
        assertTrue(siteReady.isConfigured());
        assertTrue(disk.siteReadyMarkerPresent());
    }

    @Test
    void markerWithReachableEmptyMongoReopensSetup() throws Exception {
        writeSecrets();
        assertTrue(disk.writeSiteReadyMarker());
        when(dataProvider.ping()).thenReturn(true);
        when(accountService.getAllAccounts()).thenReturn(List.of());
        assertFalse(siteReady.evaluateAtBoot());
        assertFalse(disk.siteReadyMarkerPresent());
        assertFalse(siteReady.isConfigured());
    }

    @Test
    void markerStaysWhenAccountListFails() throws Exception {
        writeSecrets();
        assertTrue(disk.writeSiteReadyMarker());
        when(dataProvider.ping()).thenReturn(true);
        when(accountService.getAllAccounts()).thenThrow(new RuntimeException("mongo blip"));
        assertTrue(siteReady.evaluateAtBoot());
        assertTrue(disk.siteReadyMarkerPresent());
        assertTrue(siteReady.isConfigured());
    }

    @Test
    void forceSetupReopensEvenWithOwner() throws Exception {
        writeSecrets();
        assertTrue(disk.writeSiteReadyMarker());
        when(dataProvider.ping()).thenReturn(true);
        when(accountService.getAllAccounts()).thenReturn(List.of(new AccountEntity()));
        System.setProperty("cse.force.setup", "true");
        assertFalse(siteReady.evaluateAtBoot());
        siteReady.holdInstallerOpen();
        assertFalse(siteReady.isConfigured());
        siteReady.markFinished();
        assertTrue(siteReady.isConfigured());
    }

    @Test
    void refreshAfterDataChangeClearsMarkerWhenAccountsGone() throws Exception {
        writeSecrets();
        when(dataProvider.ping()).thenReturn(true);
        when(accountService.getAllAccounts()).thenReturn(List.of());
        assertTrue(disk.writeSiteReadyMarker());
        siteReady.markFinished();
        siteReady.refreshAfterDataChange();
        assertFalse(disk.siteReadyMarkerPresent());
        assertFalse(siteReady.isConfigured());
    }

    @Test
    void refreshAfterDataChangeKeepsReadyWhenOwnerExists() throws Exception {
        writeSecrets();
        when(dataProvider.ping()).thenReturn(true);
        when(accountService.getAllAccounts()).thenReturn(List.of(new AccountEntity()));
        siteReady.refreshAfterDataChange();
        assertTrue(disk.siteReadyMarkerPresent());
        assertTrue(siteReady.isConfigured());
    }

    @Test
    void refreshAfterDataChangeLeavesReadyWhenAccountListFails() throws Exception {
        writeSecrets();
        assertTrue(disk.writeSiteReadyMarker());
        siteReady.markFinished();
        when(accountService.getAllAccounts()).thenThrow(new RuntimeException("blip"));
        siteReady.refreshAfterDataChange();
        assertTrue(disk.siteReadyMarkerPresent());
        assertTrue(siteReady.isConfigured());
    }

    private void writeSecrets() throws Exception {
        File instance = disk.secretFile(SettingKeys.CONTEXT_DATASOURCE_DBMS_INSTANCE + ".json");
        File access = disk.secretFile(SettingKeys.CONTEXT_DATASOURCE_DBMS_ACCESS + ".json");
        Files.createDirectories(instance.toPath().getParent());
        Files.writeString(instance.toPath(), """
                {"%s":"localhost","%s":"rhz-we-site"}
                """.formatted(SettingKeys.KEY_DBMS_ADDRESS, SettingKeys.KEY_DBMS_DBNAME),
                StandardCharsets.UTF_8);
        Files.writeString(access.toPath(), """
                {"%s":"app"}
                """.formatted(SettingKeys.KEY_DBMS_ACCESS_USER), StandardCharsets.UTF_8);
    }

    private static final class TempDisk implements InstallDisk {
        private final Path root;

        private TempDisk(Path root) {
            this.root = root;
        }

        @Override
        public boolean siteReadyMarkerPresent() {
            return marker().isFile();
        }

        @Override
        public boolean writeSiteReadyMarker() {
            try {
                Files.writeString(marker().toPath(), "ready", StandardCharsets.UTF_8);
                return true;
            } catch (Exception ex) {
                return false;
            }
        }

        @Override
        public boolean deleteSiteReadyMarker() {
            File marker = marker();
            return !marker.isFile() || marker.delete();
        }

        @Override
        public File secretFile(String filename) {
            return root.resolve("configuration").resolve(filename).toFile();
        }

        private File marker() {
            return root.resolve(".site-ready").toFile();
        }
    }
}

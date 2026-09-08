package com.ravenherz.cse.install;

import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.dal.DataProvider;
import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.dao.AccountService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.util.Json;
import com.ravenherz.cse.util.io.CseDisk;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    private String previousRoot;
    private DataProvider dataProvider;
    private AccountService accountService;
    private SiteReady siteReady;

    @BeforeEach
    void diskAndMocks() throws Exception {
        previousRoot = System.getProperty("cse.disk.root");
        temp = Files.createTempDirectory("cse-site-ready");
        System.setProperty("cse.disk.root", temp.toString());
        CseDisk.resetForTests();
        CseDisk.bindInstanceSlug("wiztest");
        dataProvider = mock(DataProvider.class);
        accountService = mock(AccountService.class);
        ServiceProvider serviceProvider = mock(ServiceProvider.class);
        when(serviceProvider.getAccountService()).thenReturn(accountService);
        siteReady = new SiteReady(dataProvider, serviceProvider);
    }

    @AfterEach
    void restore() {
        System.clearProperty("cse.force.setup");
        CseDisk.resetForTests();
        if (previousRoot == null) {
            System.clearProperty("cse.disk.root");
        } else {
            System.setProperty("cse.disk.root", previousRoot);
        }
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
        assertTrue(CseDisk.siteReadyMarkerPresent());
    }

    @Test
    void bootWithOwnerWritesMarkerAndIsReady() throws Exception {
        writeSecrets();
        when(dataProvider.ping()).thenReturn(true);
        when(accountService.getAllAccounts()).thenReturn(List.of(new AccountEntity()));
        assertTrue(siteReady.evaluateAtBoot());
        assertTrue(siteReady.isConfigured());
        assertTrue(CseDisk.siteReadyMarkerPresent());
    }

    @Test
    void markerPlusBindingIsReadyWithoutPing() throws Exception {
        writeSecrets();
        assertTrue(CseDisk.writeSiteReadyMarker());
        when(dataProvider.ping()).thenReturn(false);
        assertTrue(siteReady.evaluateAtBoot());
        assertTrue(siteReady.isConfigured());
        assertTrue(CseDisk.siteReadyMarkerPresent());
    }

    @Test
    void markerWithReachableEmptyMongoReopensSetup() throws Exception {
        writeSecrets();
        assertTrue(CseDisk.writeSiteReadyMarker());
        when(dataProvider.ping()).thenReturn(true);
        when(accountService.getAllAccounts()).thenReturn(List.of());
        assertFalse(siteReady.evaluateAtBoot());
        assertFalse(CseDisk.siteReadyMarkerPresent());
        assertFalse(siteReady.isConfigured());
    }

    @Test
    void markerStaysWhenAccountListFails() throws Exception {
        writeSecrets();
        assertTrue(CseDisk.writeSiteReadyMarker());
        when(dataProvider.ping()).thenReturn(true);
        when(accountService.getAllAccounts()).thenThrow(new RuntimeException("mongo blip"));
        assertTrue(siteReady.evaluateAtBoot());
        assertTrue(CseDisk.siteReadyMarkerPresent());
        assertTrue(siteReady.isConfigured());
    }

    @Test
    void forceSetupReopensEvenWithOwner() throws Exception {
        writeSecrets();
        assertTrue(CseDisk.writeSiteReadyMarker());
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
        assertTrue(CseDisk.writeSiteReadyMarker());
        siteReady.markFinished();
        siteReady.refreshAfterDataChange();
        assertFalse(CseDisk.siteReadyMarkerPresent());
        assertFalse(siteReady.isConfigured());
    }

    @Test
    void refreshAfterDataChangeKeepsReadyWhenOwnerExists() throws Exception {
        writeSecrets();
        when(dataProvider.ping()).thenReturn(true);
        when(accountService.getAllAccounts()).thenReturn(List.of(new AccountEntity()));
        siteReady.refreshAfterDataChange();
        assertTrue(CseDisk.siteReadyMarkerPresent());
        assertTrue(siteReady.isConfigured());
    }

    @Test
    void refreshAfterDataChangeLeavesReadyWhenAccountListFails() throws Exception {
        writeSecrets();
        assertTrue(CseDisk.writeSiteReadyMarker());
        siteReady.markFinished();
        when(accountService.getAllAccounts()).thenThrow(new RuntimeException("blip"));
        siteReady.refreshAfterDataChange();
        assertTrue(CseDisk.siteReadyMarkerPresent());
        assertTrue(siteReady.isConfigured());
    }

    private void writeSecrets() throws Exception {
        assertTrue(CseDisk.ensureInstanceConfigurationWritable());
        Json.MAPPER.writeValue(CseDisk.secretFile(SettingKeys.CONTEXT_DATASOURCE_DBMS_INSTANCE + ".json"),
                Map.of(SettingKeys.KEY_DBMS_ADDRESS, "localhost",
                        SettingKeys.KEY_DBMS_DBNAME, "rhz-we-site"));
        Json.MAPPER.writeValue(CseDisk.secretFile(SettingKeys.CONTEXT_DATASOURCE_DBMS_ACCESS + ".json"),
                Map.of(SettingKeys.KEY_DBMS_ACCESS_USER, "app"));
    }
}

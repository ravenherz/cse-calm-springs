package com.ravenherz.cse.install;

import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.dal.DataProvider;
import com.ravenherz.cse.dal.dao.AccountService;
import com.ravenherz.cse.dal.dao.CategoryService;
import com.ravenherz.cse.dal.dao.RoleMatrixService;
import com.ravenherz.cse.dal.dao.RoleService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.CategoryEntity;
import com.ravenherz.cse.util.PasswordHashes;
import com.ravenherz.cse.util.staticapps.StaticAppDeployer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstallServiceTest {

    @Mock
    private InstallSettings settings;
    @Mock
    private DataProvider dataProvider;
    @Mock
    private AccountService accountService;
    @Mock
    private RoleService roleService;
    @Mock
    private RoleMatrixService roleMatrixService;
    @Mock
    private CategoryService categoryService;
    @Mock
    private PasswordHashes passwordHashes;
    @Mock
    private StaticAppDeployer staticAppDeployer;
    @Mock
    private SiteReady siteReady;
    @Mock
    private UrlTemplateSeeds urlTemplateSeeds;
    @InjectMocks
    private InstallService installService;

    @Test
    void ownerRejectsMismatchedPasswords() {
        when(dataProvider.ping()).thenReturn(true);
        when(accountService.getAllAccounts()).thenReturn(List.of());
        InstallException ex = assertThrows(InstallException.class, () -> installService.createOwner(
                Map.of("login", "aleksei", "email", "a@example.com",
                        "password", "one", "passwordRetype", "two")));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void ownerConflictIfAccountExists() {
        when(dataProvider.ping()).thenReturn(true);
        when(accountService.getAllAccounts()).thenReturn(List.of(new AccountEntity()));
        InstallException ex = assertThrows(InstallException.class, () -> installService.createOwner(
                Map.of("login", "aleksei", "email", "a@example.com",
                        "password", "one", "passwordRetype", "one")));
        assertEquals(409, ex.getStatus());
    }

    @Test
    void connectMongoSkippedWhenEnvReady() {
        when(dataProvider.usesEnvironmentCredentials()).thenReturn(true);
        when(dataProvider.ping()).thenReturn(true);
        Map<String, Object> result = installService.connectMongo(Map.of());
        assertEquals(true, result.get("skipped"));
        assertEquals(true, result.get("ok"));
    }

    @Test
    void finishRequiresOwner() {
        when(dataProvider.ping()).thenReturn(true);
        when(accountService.getAllAccounts()).thenReturn(List.of());
        InstallException ex = assertThrows(InstallException.class,
                () -> installService.finish(Map.of(), ""));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void finishRequiresSiteTitle() {
        when(dataProvider.ping()).thenReturn(true);
        when(accountService.getAllAccounts()).thenReturn(List.of(new AccountEntity()));
        InstallException ex = assertThrows(InstallException.class,
                () -> installService.finish(Map.of(), ""));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void finishKeepsNameWhenDatabaseAlreadyHasOwner() throws IOException {
        when(dataProvider.ping()).thenReturn(true);
        when(accountService.getAllAccounts()).thenReturn(List.of(new AccountEntity()));
        when(categoryService.getAllCategories()).thenReturn(List.of(new CategoryEntity()));

        Map<String, Object> status = installService.status();
        assertEquals(true, status.get("existingSite"));

        Map<String, Object> result = installService.finish(Map.of(), "/rhz-we");

        assertEquals(true, result.get("ok"));
        assertEquals("/rhz-we/editor", result.get("redirect"));
        verify(settings, never()).putValue(eq(SettingKeys.CONTEXT_DATASOURCE_PERSONAL),
                eq(SettingKeys.KEY_TAG_COMPANY_TITLE), any());
        verify(urlTemplateSeeds).ensureSeeded();
        verify(siteReady).markFinished();
    }

    @Test
    void existingSiteDoesNotFlipAfterOwnerIsCreated() {
        when(dataProvider.ping()).thenReturn(true);
        when(accountService.getAllAccounts()).thenReturn(List.of());

        assertEquals(false, installService.status().get("existingSite"));

        when(accountService.getAllAccounts()).thenReturn(List.of(new AccountEntity()));
        Map<String, Object> afterOwner = installService.status();
        assertEquals(true, afterOwner.get("hasOwner"));
        assertEquals(false, afterOwner.get("existingSite"));
    }

    @Test
    void finishUndeploysAndRedirects() throws IOException {
        when(dataProvider.ping()).thenReturn(true);
        when(accountService.getAllAccounts()).thenReturn(List.of(new AccountEntity()));
        when(categoryService.getAllCategories()).thenReturn(List.of(new CategoryEntity()));
        when(settings.persistContext(any())).thenReturn(true);

        Map<String, Object> result = installService.finish(Map.of(
                "company-title", "My Site",
                "company-phone", "555"), "/rhz-we");

        assertEquals(true, result.get("ok"));
        assertEquals("/rhz-we/editor", result.get("redirect"));
        verify(settings).putValue(SettingKeys.CONTEXT_DATASOURCE_PERSONAL,
                SettingKeys.KEY_TAG_COMPANY_TITLE, "My Site");
        verify(settings).putValue(SettingKeys.CONTEXT_DATASOURCE_PERSONAL,
                SettingKeys.KEY_TAG_COMPANY_EMAIL, "");
        verify(settings).putValue(SettingKeys.CONTEXT_DATASOURCE_PERSONAL,
                SettingKeys.KEY_TAG_COMPANY_PHONE, "555");
        verify(urlTemplateSeeds).ensureSeeded();
        verify(siteReady).markFinished();
        verify(staticAppDeployer).undeployEngineApp(StaticAppDeployer.INSTALLER_SLUG);
    }
}

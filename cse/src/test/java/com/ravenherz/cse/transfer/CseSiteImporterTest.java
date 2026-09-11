package com.ravenherz.cse.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ravenherz.cse.constants.MongoCollections;
import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.dao.AccountService;
import com.ravenherz.cse.dal.dao.AppService;
import com.ravenherz.cse.dal.dao.CategoryService;
import com.ravenherz.cse.dal.dao.ItemService;
import com.ravenherz.cse.dal.dao.PlaylistService;
import com.ravenherz.cse.dal.dao.ResourceGroupService;
import com.ravenherz.cse.dal.dao.ResourceService;
import com.ravenherz.cse.dal.dao.ThemeService;
import com.ravenherz.cse.dal.dao.UrlTemplateService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.AppEntity;
import com.ravenherz.cse.dal.dto.CategoryEntity;
import com.ravenherz.cse.dal.dto.DataChunkEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.SettingContextEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.dto.basic.AppData;
import com.ravenherz.cse.dal.dto.basic.CategoryData;
import com.ravenherz.cse.dal.dto.basic.HistoryData;
import com.ravenherz.cse.dal.dto.basic.PageData;
import com.ravenherz.cse.dal.dto.basic.ResourceData;
import com.ravenherz.cse.dal.dto.basic.enums.ResourceType;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import com.ravenherz.cse.util.Settings;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CseSiteImporterTest {

    private final CseSiteExporter exporter = new CseSiteExporter();
    private final CseSiteImporter importer = new CseSiteImporter();
    private ServiceProvider services;
    private ResourceService resourceService;
    private MongoTemplate mongo;
    private Settings settings;

    @BeforeEach
    void stubs() {
        services = mock(ServiceProvider.class);
        AccountService accounts = mock(AccountService.class);
        CategoryService categories = mock(CategoryService.class);
        ResourceGroupService groups = mock(ResourceGroupService.class);
        resourceService = mock(ResourceService.class);
        ItemService items = mock(ItemService.class);
        PlaylistService playlists = mock(PlaylistService.class);
        AppService apps = mock(AppService.class);
        ThemeService themes = mock(ThemeService.class);
        UrlTemplateService urlTemplates = mock(UrlTemplateService.class);
        when(services.getAccountService()).thenReturn(accounts);
        when(services.getCategoryService()).thenReturn(categories);
        when(services.getResourceGroupService()).thenReturn(groups);
        when(services.getResourceService()).thenReturn(resourceService);
        when(services.getItemService()).thenReturn(items);
        when(services.getPlaylistService()).thenReturn(playlists);
        when(services.getAppService()).thenReturn(apps);
        when(services.getThemeService()).thenReturn(themes);
        when(services.getUrlTemplateService()).thenReturn(urlTemplates);
        when(accounts.getAll()).thenReturn(List.of());
        when(categories.getAll()).thenReturn(List.of());
        when(groups.getAll()).thenReturn(List.of());
        when(resourceService.getAll()).thenReturn(List.of());
        when(resourceService.listWithContent()).thenReturn(List.of());
        when(resourceService.getDataChunks(anyList())).thenReturn(List.of());
        when(items.getAll()).thenReturn(List.of());
        when(playlists.getAll()).thenReturn(List.of());
        when(apps.getAll()).thenReturn(List.of());
        when(themes.getAll()).thenReturn(List.of());
        when(urlTemplates.getAll()).thenReturn(List.of());
        mongo = mock(MongoTemplate.class);
        settings = mock(Settings.class);
        when(settings.isOverlayContext("config-personal")).thenReturn(true);
        when(settings.isOverlayContext("secret-dbms-access")).thenReturn(false);
    }

    @Test
    void roundTripRestoresPagesChunksAndHashesWithoutSessions() throws Exception {
        ObjectId ownerId = new ObjectId("68b000000000000000000001");
        ObjectId categoryId = new ObjectId("68b000000000000000000002");
        ObjectId imageId = new ObjectId("68b000000000000000000003");
        ObjectId chunkId = new ObjectId("68b000000000000000000004");
        ObjectId pageId = new ObjectId("68b000000000000000000005");

        AccountEntity owner = new AccountEntity(new AccountData("ada", "argon2-hash", "ada@example.com",
                SecurityLevel.OWNER));
        owner.setId(ownerId);
        owner.getAccountData().setSessions(Set.of(new AccountData.AccountSession(
                "127.0.0.1", "test-agent", "live-session-token",
                LocalDateTime.of(2026, Month.SEPTEMBER, 2, 10, 0))));

        CategoryEntity category = new CategoryEntity(
                new CategoryData("journal", "Journal", "desc", true, true), owner);
        category.setId(categoryId);

        ResourceData image = new ResourceData();
        image.setType(ResourceType.IMAGE);
        image.setPathPublic("/ada/res/images/cover.jpg");
        image.setContentRaw("Zm9v");
        ResourceEntity resource = new ResourceEntity(image, owner);
        resource.setId(imageId);

        DataChunkEntity chunk = new DataChunkEntity("Zm9vYmFy");
        chunk.setId(chunkId);
        image.setLargeFile(true);
        image.addDataChunkId(chunkId);

        PageData pageData = new PageData("Hello", "H", "S", "Body", List.of("tag"));
        pageData.setRefImage(resource);
        ItemEntity page = new ItemEntity("hello", pageData, owner);
        page.setId(pageId);
        page.setRefCategory(category);
        page.setHistoryData(new HistoryData(owner));

        when(services.getAccountService().getAll()).thenReturn(List.of(owner));
        when(services.getCategoryService().getAll()).thenReturn(List.of(category));
        when(resourceService.getAll()).thenReturn(List.of(resource));
        when(resourceService.listWithContent()).thenReturn(List.of(resource));
        when(resourceService.getDataChunks(anyList())).thenReturn(List.of(chunk));
        when(services.getItemService().getAll()).thenReturn(List.of(page));

        Map<String, Map<String, String>> overlay = new HashMap<>();
        overlay.put("config-personal", Map.of("company-title", "Ada"));
        overlay.put("secret-dbms-access", Map.of("dbms-access-pswd", "nope"));

        ByteArrayOutputStream zip = new ByteArrayOutputStream();
        exporter.write(zip, services, "mongodb", overlay);

        CseSiteImporter.ImportResult result = importer.apply(new ByteArrayInputStream(zip.toByteArray()),
                mongo, settings);

        assertEquals(1, result.counts().get(MongoCollections.DATABASE_ITEMS));
        assertEquals(1, result.counts().get(MongoCollections.DATABASE_ACCOUNTS));
        assertEquals(1, result.counts().get(MongoCollections.DATABASE_DATACHUNKS));
        assertEquals(1, result.counts().get(MongoCollections.DATABASE_SETTINGS));

        ArgumentCaptor<Object> saved = ArgumentCaptor.forClass(Object.class);
        verify(mongo, atLeast(1)).save(saved.capture());
        List<Object> stored = saved.getAllValues();

        ItemEntity importedPage = stored.stream()
                .filter(ItemEntity.class::isInstance)
                .map(ItemEntity.class::cast)
                .findFirst()
                .orElseThrow();
        assertEquals(pageId, importedPage.getId());
        assertEquals("hello", importedPage.getUniqueUriName());
        assertEquals(categoryId, importedPage.getRefCategoryId());
        assertEquals("Body", importedPage.getPageData().getDescription());
        assertEquals(imageId, importedPage.getPageData().getRefImageId());
        assertNull(importedPage.getPageData().getRefImage());

        AccountEntity importedAccount = stored.stream()
                .filter(AccountEntity.class::isInstance)
                .map(AccountEntity.class::cast)
                .findFirst()
                .orElseThrow();
        assertEquals("ada", importedAccount.getAccountData().getLogin());
        assertEquals("argon2-hash", importedAccount.getAccountData().getHash());
        assertNull(importedAccount.getAccountData().getSessions());

        DataChunkEntity importedChunk = stored.stream()
                .filter(DataChunkEntity.class::isInstance)
                .map(DataChunkEntity.class::cast)
                .findFirst()
                .orElseThrow();
        assertEquals("Zm9vYmFy", importedChunk.getData());

        SettingContextEntity overlayDoc = stored.stream()
                .filter(SettingContextEntity.class::isInstance)
                .map(SettingContextEntity.class::cast)
                .findFirst()
                .orElseThrow();
        assertEquals("config-personal", overlayDoc.getContext());
        assertEquals("Ada", overlayDoc.getValues().get("company-title"));
        verify(settings).reloadFromMongo();
        assertEquals("owner", importedAccount.getAccountData().getRoleId());
    }

    @Test
    void prunesDocumentsMissingFromTheArchiveAndLeavesSecrets() throws Exception {
        ByteArrayOutputStream zip = new ByteArrayOutputStream();
        exporter.write(zip, services, "mongodb", Map.of());

        AccountEntity stale = new AccountEntity();
        stale.setId(new ObjectId("68b000000000000000000099"));
        when(mongo.findAll(AccountEntity.class)).thenReturn(List.of(stale));

        SettingContextEntity secret = new SettingContextEntity();
        secret.setContext("secret-dbms-access");
        SettingContextEntity leftover = new SettingContextEntity();
        leftover.setContext("config-personal");
        leftover.setId(new ObjectId("68b000000000000000000098"));
        when(mongo.findAll(SettingContextEntity.class)).thenReturn(List.of(secret, leftover));

        importer.apply(new ByteArrayInputStream(zip.toByteArray()), mongo, settings);

        verify(mongo).remove(stale);
        verify(mongo).remove(leftover);
        verify(mongo, never()).remove(secret);
    }

    @Test
    void rejectsMissingManifest() {
        ByteArrayOutputStream zip = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(zip, StandardCharsets.UTF_8)) {
            out.putNextEntry(new ZipEntry("collections/cse-items.json"));
            out.write("[]".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        CseSiteImportException error = assertThrows(CseSiteImportException.class,
                () -> importer.apply(new ByteArrayInputStream(zip.toByteArray()), mongo, settings));
        assertTrue(error.getMessage().contains("manifest.json"));
    }

    @Test
    void rejectsBsonExtendedJson() throws Exception {
        Map<String, String> files = new HashMap<>();
        files.put("manifest.json", """
                {"format":"cse-site","version":1,"collections":{}}
                """);
        for (String name : CseSiteFormat.COLLECTIONS) {
            files.put("collections/" + name + ".json", "[]");
        }
        files.put("collections/cse-items.json", """
                [{"id":{"$oid":"68b000000000000000000005"}}]
                """);
        CseSiteImportException error = assertThrows(CseSiteImportException.class,
                () -> importer.apply(new ByteArrayInputStream(zipOf(files)), mongo, settings));
        assertTrue(error.getMessage().contains("$oid"));
        verify(mongo, never()).save(any());
    }

    @Test
    void importsAppStoreGrantFieldsFromAppsCollection() throws Exception {
        AppData appData = new AppData();
        appData.setSlug("fretlab");
        appData.setStoreEnabled(true);
        appData.setStoreOpen(true);
        appData.setStoreTables(List.of(new com.ravenherz.cse.store.AppStoreTableSpec(
                "progress", com.ravenherz.cse.store.AppStoreAccess.OWNER, null)));
        AppEntity app = new AppEntity();
        app.setId(new ObjectId("68b0000000000000000000a1"));
        app.setAppData(appData);
        when(services.getAppService().getAll()).thenReturn(List.of(app));

        ByteArrayOutputStream zip = new ByteArrayOutputStream();
        exporter.write(zip, services, "mongodb", Map.of());
        importer.apply(new ByteArrayInputStream(zip.toByteArray()), mongo, settings);

        ArgumentCaptor<Object> saved = ArgumentCaptor.forClass(Object.class);
        verify(mongo, atLeast(1)).save(saved.capture());
        AppEntity imported = saved.getAllValues().stream()
                .filter(AppEntity.class::isInstance)
                .map(AppEntity.class::cast)
                .findFirst()
                .orElseThrow();
        assertEquals("fretlab", imported.getAppData().getSlug());
        assertTrue(imported.getAppData().isStoreEnabled());
        assertTrue(imported.getAppData().isStoreOpen());
        assertEquals("progress", imported.getAppData().getStoreTables().get(0).getName());
    }

    @Test
    void importsAppStoreExtrasAndDropsLeftoverAppCollections() throws Exception {
        Map<String, String> files = new HashMap<>();
        files.put("manifest.json", """
                {"format":"cse-site","version":1,"collections":{}}
                """);
        for (String name : CseSiteFormat.COLLECTIONS) {
            files.put("collections/" + name + ".json", "[]");
        }
        files.put("collections/fretlab-progress.json", """
                [{"id":"68b0000000000000000000aa","ownerId":"68b000000000000000000001","createdAt":"2026-09-08T12:00:00Z","updatedAt":"2026-09-08T12:00:00Z","data":{"tuning":"E"}}]
                """);
        files.put("collections/cse-mystery.json", "[]");
        when(mongo.getCollectionNames()).thenReturn(Set.of("hello-snake-scores"));

        importer.apply(new ByteArrayInputStream(zipOf(files)), mongo, settings);

        verify(mongo).insert(anyList(), org.mockito.ArgumentMatchers.eq("fretlab-progress"));
        verify(mongo).dropCollection("hello-snake-scores");
        verify(mongo, never()).insert(anyList(), org.mockito.ArgumentMatchers.eq("cse-mystery"));
    }

    @Test
    void importsOldArchiveWithoutRolesCollectionsAndFillsRoleId() throws Exception {
        Map<String, String> files = new HashMap<>();
        files.put("manifest.json", """
                {"format":"cse-site","version":1,"collections":{}}
                """);
        for (String name : CseSiteFormat.COLLECTIONS) {
            if (MongoCollections.DATABASE_ROLES.equals(name)
                    || MongoCollections.DATABASE_ROLE_MATRIX.equals(name)
                    || MongoCollections.DATABASE_URL_TEMPLATES.equals(name)) {
                continue;
            }
            files.put("collections/" + name + ".json", "[]");
        }
        files.put("collections/cse-accounts.json", """
                [{"id":"68b000000000000000000001","accountData":{"login":"ada","hash":"h","emailAddress":"a@x","level":"OWNER","loginable":true}}]
                """);

        importer.apply(new ByteArrayInputStream(zipOf(files)), mongo, settings);

        ArgumentCaptor<Object> saved = ArgumentCaptor.forClass(Object.class);
        verify(mongo, atLeast(1)).save(saved.capture());
        AccountEntity imported = saved.getAllValues().stream()
                .filter(AccountEntity.class::isInstance)
                .map(AccountEntity.class::cast)
                .findFirst()
                .orElseThrow();
        assertEquals("owner", imported.getAccountData().getRoleId());
    }

    private static byte[] zipOf(Map<String, String> files) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> file : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(file.getKey()));
                zip.write(file.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
}

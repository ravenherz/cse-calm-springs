package com.ravenherz.cse.transfer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.ravenherz.cse.constants.MongoCollections;
import com.ravenherz.cse.dal.DataProvider;
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
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.dto.basic.AppData;
import com.ravenherz.cse.dal.dto.basic.CategoryData;
import com.ravenherz.cse.dal.dto.basic.HistoryData;
import com.ravenherz.cse.dal.dto.basic.PageData;
import com.ravenherz.cse.dal.dto.basic.ResourceData;
import com.ravenherz.cse.dal.dto.basic.enums.ResourceType;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CseSiteExporterTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final CseSiteExporter exporter = new CseSiteExporter();
    private ServiceProvider services;
    private ResourceService resourceService;

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
    }

    @Test
    void archiveIsZipOfJsonCollectionsWithHexIds() throws Exception {
        ObjectId ownerId = new ObjectId("68b000000000000000000001");
        ObjectId categoryId = new ObjectId("68b000000000000000000002");
        ObjectId imageId = new ObjectId("68b000000000000000000003");
        ObjectId chunkId = new ObjectId("68b000000000000000000004");
        ObjectId pageId = new ObjectId("68b000000000000000000005");

        AccountEntity owner = new AccountEntity(new AccountData("ada", "argon2-hash", "ada@example.com",
                SecurityLevel.OWNER));
        owner.setId(ownerId);
        owner.getAccountData().setShownName("Ada");
        owner.getAccountData().setAvatar("data:image/jpeg;base64,Zm9v");
        AccountData.AccountSession session = new AccountData.AccountSession(
                "127.0.0.1", "test-agent", "live-session-token",
                LocalDateTime.of(2026, Month.SEPTEMBER, 2, 10, 0));
        owner.getAccountData().setSessions(Set.of(session));

        CategoryEntity category = new CategoryEntity(
                new CategoryData("journal", "Journal", "desc", true, true), owner);
        category.setId(categoryId);

        ResourceData image = new ResourceData();
        image.setType(ResourceType.IMAGE);
        image.setPathPublic("/ada/res/images/cover.jpg");
        image.setContentRaw("Zm9v");
        image.setLargeFile(true);
        image.addDataChunkId(chunkId);
        ResourceEntity resource = new ResourceEntity(image, owner);
        resource.setId(imageId);

        DataChunkEntity chunk = new DataChunkEntity("Zm9vYmFy");
        chunk.setId(chunkId);

        PageData pageData = new PageData("H", "S", "Body", List.of("tag"));
        pageData.setRefImage(resource);
        ItemEntity page = new ItemEntity("hello", pageData, owner);
        page.setId(pageId);
        page.setRefCategory(category);
        page.setHistoryData(new HistoryData(owner));
        page.getHistoryData().getEvents()[0].setLocalDateTime(
                LocalDateTime.of(2026, Month.AUGUST, 22, 1, 14, 0));

        when(services.getAccountService().getAll()).thenReturn(List.of(owner));
        when(services.getCategoryService().getAll()).thenReturn(List.of(category));
        when(resourceService.getAll()).thenReturn(List.of(resource));
        when(resourceService.listWithContent()).thenReturn(List.of(resource));
        when(resourceService.getDataChunks(anyList())).thenReturn(List.of(chunk));
        when(services.getItemService().getAll()).thenReturn(List.of(page));

        Map<String, Map<String, String>> overlay = new HashMap<>();
        overlay.put("config-personal", Map.of("company-title", "Ada"));
        overlay.put("secret-dbms-access", Map.of("dbms-access-pswd", "nope"));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        exporter.write(bytes, services, "mongodb", overlay);
        Map<String, String> entries = unzip(bytes.toByteArray());

        assertTrue(entries.containsKey("manifest.json"));
        for (String collection : CseSiteFormat.COLLECTIONS) {
            assertTrue(entries.containsKey("collections/" + collection + ".json"), collection);
        }

        assertTrue(entries.containsKey("collections/"));

        JsonNode manifest = JSON.readTree(entries.get("manifest.json"));
        assertEquals(CseSiteFormat.FORMAT, manifest.get("format").asText());
        assertEquals(CseSiteFormat.VERSION, manifest.get("version").asInt());
        assertEquals("mongodb", manifest.get("sourceEngine").asText());
        assertEquals(1, manifest.get("collections").get(MongoCollections.DATABASE_ACCOUNTS).get("count").asInt());
        assertEquals(1, manifest.get("collections").get(MongoCollections.DATABASE_ITEMS).get("count").asInt());

        String all = String.join("\n", entries.values());
        assertFalse(all.contains("$oid"));
        assertFalse(all.contains("$date"));
        assertFalse(all.contains("_class"));
        assertFalse(all.contains("live-session-token"));
        assertFalse(all.contains("\"refCategory\""));
        assertFalse(all.contains("\"sessions\""));

        List<Map<String, Object>> accounts = JSON.readValue(entries.get("collections/cse-accounts.json"),
                new TypeReference<>() {});
        @SuppressWarnings("unchecked")
        Map<String, Object> accountData = (Map<String, Object>) accounts.get(0).get("accountData");
        assertEquals("ada", accountData.get("login"));
        assertEquals("argon2-hash", accountData.get("hash"));
        assertEquals("OWNER", accountData.get("level"));
        assertEquals("owner", accountData.get("roleId"));
        assertEquals("Ada", accountData.get("shownName"));
        assertEquals("data:image/jpeg;base64,Zm9v", accountData.get("avatar"));
        assertFalse(accountData.containsKey("sessions"));

        List<Map<String, Object>> items = JSON.readValue(entries.get("collections/cse-items.json"),
                new TypeReference<>() {});
        assertEquals(pageId.toHexString(), items.get(0).get("id"));
        assertEquals(categoryId.toHexString(), items.get(0).get("refCategoryId"));
        @SuppressWarnings("unchecked")
        Map<String, Object> exportedPage = (Map<String, Object>) items.get(0).get("pageData");
        assertEquals("Body", exportedPage.get("description"));
        assertEquals(imageId.toHexString(), exportedPage.get("refImageId"));
        assertEquals(false, exportedPage.get("noTopDisplayImage"));
        assertEquals(false, exportedPage.get("exportPdf"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = (List<Map<String, Object>>) ((Map<String, Object>) items.get(0)
                .get("historyData")).get("events");
        assertEquals("2026-08-22T01:14:00Z", events.get(0).get("localDateTime"));
        assertEquals(ownerId.toHexString(), events.get(0).get("ownerId"));
        assertFalse(events.get(0).containsKey("owner"));
        @SuppressWarnings("unchecked")
        Map<String, Object> security = (Map<String, Object>) items.get(0).get("securityData");
        @SuppressWarnings("unchecked")
        Map<String, Object> access = (Map<String, Object>) security.get("accessSettings");
        @SuppressWarnings("unchecked")
        Map<String, Object> readRule = (Map<String, Object>) access.get("ACCESS_READ");
        assertEquals(true, readRule.get("inherit"));
        assertTrue(readRule.containsKey("roleIds"));

        List<Map<String, Object>> chunks = JSON.readValue(entries.get("collections/cse-datachunks.json"),
                new TypeReference<>() {});
        assertEquals(chunkId.toHexString(), chunks.get(0).get("id"));
        assertEquals("Zm9vYmFy", chunks.get(0).get("data"));

        List<Map<String, Object>> settings = JSON.readValue(entries.get("collections/cse-settings.json"),
                new TypeReference<>() {});
        assertEquals(1, settings.size());
        assertEquals("config-personal", settings.get(0).get("context"));
    }

    @Test
    void mongoDumpWritesPagesAndEveryDatachunk() throws Exception {
        ObjectId pageId = new ObjectId("68b000000000000000000005");
        ObjectId chunkId = new ObjectId("68b000000000000000000004");
        ItemEntity page = new ItemEntity("hello", new PageData("H", "S", "Body", List.of("tag")),
                new AccountEntity(new AccountData("ada", "hash", "ada@example.com", SecurityLevel.OWNER)));
        page.setId(pageId);

        MongoTemplate mongo = mock(MongoTemplate.class);
        DataProvider data = mock(DataProvider.class);
        when(data.getMongoTemplate()).thenReturn(mongo);
        when(mongo.findAll(ItemEntity.class)).thenReturn(List.of(page));

        Document chunkBson = new Document("_id", chunkId).append("data", "Zm9vYmFy");
        @SuppressWarnings("unchecked")
        MongoCollection<Document> chunks = mock(MongoCollection.class);
        @SuppressWarnings("unchecked")
        FindIterable<Document> found = mock(FindIterable.class);
        when(mongo.getCollection(MongoCollections.DATABASE_DATACHUNKS)).thenReturn(chunks);
        when(chunks.find()).thenReturn(found);
        when(found.into(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<Document> into = invocation.getArgument(0);
            into.add(chunkBson);
            return into;
        });

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        exporter.write(bytes, data, services, "mongodb", Map.of());
        Map<String, String> entries = unzip(bytes.toByteArray());

        List<Map<String, Object>> items = JSON.readValue(entries.get("collections/cse-items.json"),
                new TypeReference<>() {});
        assertEquals(1, items.size());
        assertEquals("hello", items.get(0).get("uniqueUriName"));
        @SuppressWarnings("unchecked")
        Map<String, Object> pageData = (Map<String, Object>) items.get(0).get("pageData");
        assertEquals("Body", pageData.get("description"));

        List<Map<String, Object>> dumpedChunks = JSON.readValue(entries.get("collections/cse-datachunks.json"),
                new TypeReference<>() {});
        assertEquals(1, dumpedChunks.size());
        assertEquals(chunkId.toHexString(), dumpedChunks.get(0).get("id"));
        assertEquals("Zm9vYmFy", dumpedChunks.get(0).get("data"));
        assertTrue(entries.containsKey("collections/cse-roles.json"));
        assertTrue(entries.containsKey("collections/cse-role-matrix.json"));
    }

    @Test
    void mongoDumpWritesAppStoreCollectionsForInstalledSlugs() throws Exception {
        AppData appData = new AppData();
        appData.setSlug("hello-snake");
        appData.setStoreEnabled(true);
        AppEntity app = new AppEntity();
        app.setAppData(appData);
        app.setId(new ObjectId("68b0000000000000000000a1"));

        MongoTemplate mongo = mock(MongoTemplate.class);
        DataProvider data = mock(DataProvider.class);
        when(data.getMongoTemplate()).thenReturn(mongo);
        when(mongo.findAll(AppEntity.class)).thenReturn(List.of(app));
        when(mongo.getCollectionNames()).thenReturn(Set.of(
                "hello-snake-scores", "cse-accounts", "orphan-progress"));

        Document row = new Document("_id", new ObjectId("68b0000000000000000000aa"))
                .append("ownerId", "68b000000000000000000001")
                .append("createdAt", "2026-09-08T12:00:00Z")
                .append("updatedAt", "2026-09-08T12:00:00Z")
                .append("data", new Document("score", 9));
        @SuppressWarnings("unchecked")
        MongoCollection<Document> scores = mock(MongoCollection.class);
        @SuppressWarnings("unchecked")
        FindIterable<Document> found = mock(FindIterable.class);
        when(mongo.getCollection("hello-snake-scores")).thenReturn(scores);
        when(scores.find()).thenReturn(found);
        when(found.into(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<Document> into = invocation.getArgument(0);
            into.add(row);
            return into;
        });
        @SuppressWarnings("unchecked")
        MongoCollection<Document> chunks = mock(MongoCollection.class);
        @SuppressWarnings("unchecked")
        FindIterable<Document> emptyChunks = mock(FindIterable.class);
        when(mongo.getCollection(MongoCollections.DATABASE_DATACHUNKS)).thenReturn(chunks);
        when(chunks.find()).thenReturn(emptyChunks);
        when(emptyChunks.into(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        exporter.write(bytes, data, services, "mongodb", Map.of());
        Map<String, String> entries = unzip(bytes.toByteArray());

        assertTrue(entries.containsKey("collections/hello-snake-scores.json"));
        assertFalse(entries.containsKey("collections/orphan-progress.json"));
        List<Map<String, Object>> docs = JSON.readValue(entries.get("collections/hello-snake-scores.json"),
                new TypeReference<>() {});
        assertEquals(1, docs.size());
        assertEquals("68b0000000000000000000aa", docs.get(0).get("id"));
        assertEquals(9, ((Number) ((Map<?, ?>) docs.get(0).get("data")).get("score")).intValue());
        JsonNode manifest = JSON.readTree(entries.get("manifest.json"));
        assertEquals(1, manifest.get("collections").get("hello-snake-scores").get("count").asInt());
    }

    @Test
    void emptySiteStillWritesEveryCollectionFile() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        exporter.write(bytes, services, "mongodb", Map.of());
        Map<String, String> entries = unzip(bytes.toByteArray());
        assertTrue(entries.containsKey("collections/"));
        assertEquals(CseSiteFormat.COLLECTIONS.size() + 2, entries.size());
        assertEquals("[]", entries.get("collections/cse-accounts.json"));
        assertEquals("[]", entries.get("collections/cse-items.json"));
        assertEquals("[]", entries.get("collections/cse-datachunks.json"));
        assertEquals("[]", entries.get("collections/cse-roles.json"));
        assertEquals("[]", entries.get("collections/cse-role-matrix.json"));
    }

    @Test
    void zipWriteOrderCoversEveryCollection() {
        assertEquals(Set.copyOf(CseSiteFormat.COLLECTIONS), Set.copyOf(CseSiteFormat.ZIP_WRITE_ORDER));
        assertEquals(CseSiteFormat.COLLECTIONS.size(), CseSiteFormat.ZIP_WRITE_ORDER.size());
    }

    @Test
    void writeDoesNotCloseTheCallersStream() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream() {
            @Override
            public void close() {
                throw new IllegalStateException("servlet stream must not be closed");
            }
        };
        exporter.write(bytes, services, "mongodb", Map.of());
        Map<String, String> entries = unzip(bytes.toByteArray());
        assertTrue(entries.containsKey("manifest.json"));
        assertEquals("[]", entries.get("collections/cse-items.json"));
    }

    @Test
    void chunkDocumentKeepsBsonBytes() {
        ObjectId id = new ObjectId("68b000000000000000000004");
        org.bson.Document bson = new org.bson.Document("_id", id)
                .append("data", "Zm9vYmFy")
                .append("entityVersion", "0.1.0");
        Map<String, Object> chunk = CseSiteDocuments.chunkDocument(bson);
        assertEquals(id.toHexString(), chunk.get("id"));
        assertEquals("Zm9vYmFy", chunk.get("data"));
    }

    private static Map<String, String> unzip(byte[] zip) throws Exception {
        Map<String, String> entries = new HashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                entries.put(entry.getName(), new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return entries;
    }
}

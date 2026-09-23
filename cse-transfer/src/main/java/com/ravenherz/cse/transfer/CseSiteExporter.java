package com.ravenherz.cse.transfer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.ravenherz.cse.constants.MongoCollections;
import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.dal.DataProvider;
import com.ravenherz.cse.transfer.SiteServices;
import com.ravenherz.cse.dal.dao.ResourceService;
import com.ravenherz.cse.dal.dao.Store;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.AppEntity;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.CategoryEntity;
import com.ravenherz.cse.dal.dto.DataChunkEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.PlaylistEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.ResourceGroupEntity;
import com.ravenherz.cse.dal.dto.RoleEntity;
import com.ravenherz.cse.dal.dto.RoleMatrixDocument;
import com.ravenherz.cse.dal.dto.SettingContextEntity;
import com.ravenherz.cse.dal.dto.ThemeEntity;
import com.ravenherz.cse.dal.dto.UrlTemplateEntity;
import com.ravenherz.cse.store.AppStoreBson;
import com.ravenherz.cse.store.AppStoreNames;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class CseSiteExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CseSiteExporter.class);

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .disable(SerializationFeature.INDENT_OUTPUT)
            .build();

    public void write(OutputStream out, SiteServices services, String sourceEngine,
            Map<String, Map<String, String>> settingsOverlay) throws IOException {
        write(out, null, services, sourceEngine, settingsOverlay);
    }

    public void write(OutputStream out, DataProvider dataProvider, SiteServices services,
            String sourceEngine, Map<String, Map<String, String>> settingsOverlay) throws IOException {
        MongoTemplate mongo = dataProvider == null ? null : dataProvider.getMongoTemplate();
        ZipOutputStream zip = new ZipOutputStream(new NonClosingOutputStream(out), StandardCharsets.UTF_8);
        try {
            zip.putNextEntry(new ZipEntry(CseSiteFormat.COLLECTIONS_DIR));
            zip.closeEntry();
            Map<String, Long> counts;
            if (mongo != null) {
                counts = writeMongo(zip, mongo, settingsOverlay);
            } else {
                counts = writeSnapshot(zip, snapshot(services, settingsOverlay));
            }
            zip.putNextEntry(new ZipEntry(CseSiteFormat.MANIFEST_ENTRY));
            zip.write(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest(sourceEngine, counts)));
            zip.closeEntry();
            zip.finish();
            zip.flush();
        } finally {
            zip.flush();
        }
    }

    Map<String, List<Map<String, Object>>> snapshot(SiteServices services,
            Map<String, Map<String, String>> settingsOverlay) {
        List<AccountEntity> accounts = typed(services.getAccountService(), AccountEntity.class);
        List<CategoryEntity> categories = typed(services.getCategoryService(), CategoryEntity.class);
        List<ResourceGroupEntity> groups = typed(services.getResourceGroupService(), ResourceGroupEntity.class);
        List<ResourceEntity> resources = services.getResourceService() == null
                ? List.of() : services.getResourceService().listWithContent();
        List<ItemEntity> items = typed(services.getItemService(), ItemEntity.class);
        List<PlaylistEntity> playlists = typed(services.getPlaylistService(), PlaylistEntity.class);
        List<UrlTemplateEntity> urlTemplates = typed(services.getUrlTemplateService(), UrlTemplateEntity.class);
        List<AppEntity> apps = typed(services.getAppService(), AppEntity.class);
        List<ThemeEntity> themes = typed(services.getThemeService(), ThemeEntity.class);

        Set<ObjectId> chunkIds = new LinkedHashSet<>();
        resources.forEach(resource -> chunkIds.addAll(CseSiteDocuments.chunkIds(resource)));
        apps.forEach(app -> chunkIds.addAll(CseSiteDocuments.chunkIds(app)));
        themes.forEach(theme -> chunkIds.addAll(CseSiteDocuments.chunkIds(theme)));
        List<DataChunkEntity> chunks = loadChunks(services.getResourceService(), chunkIds);

        Map<String, List<Map<String, Object>>> collections = new LinkedHashMap<>();
        collections.put(MongoCollections.DATABASE_ROLES, mapAll(roles(services), CseSiteDocuments::role));
        RoleMatrixDocument matrix = matrix(services);
        collections.put(MongoCollections.DATABASE_ROLE_MATRIX,
                matrix == null ? List.of() : List.of(CseSiteDocuments.roleMatrix(matrix)));
        collections.put(MongoCollections.DATABASE_ACCOUNTS, mapAll(accounts, CseSiteDocuments::account));
        collections.put(MongoCollections.DATABASE_CATEGORIES, mapAll(categories, CseSiteDocuments::category));
        collections.put(MongoCollections.DATABASE_RESOURCE_GROUPS, mapAll(groups, CseSiteDocuments::resourceGroup));
        collections.put(MongoCollections.DATABASE_DATACHUNKS, mapAll(chunks, CseSiteDocuments::chunk));
        collections.put(MongoCollections.DATABASE_RESOURCES, mapAll(resources, CseSiteDocuments::resource));
        collections.put(MongoCollections.DATABASE_ITEMS, mapAll(items, CseSiteDocuments::item));
        collections.put(MongoCollections.DATABASE_PLAYLISTS, mapAll(playlists, CseSiteDocuments::playlist));
        collections.put(MongoCollections.DATABASE_URL_TEMPLATES, mapAll(urlTemplates, CseSiteDocuments::urlTemplate));
        collections.put(MongoCollections.DATABASE_APPS, mapAll(apps, CseSiteDocuments::app));
        collections.put(MongoCollections.DATABASE_THEMES, mapAll(themes, CseSiteDocuments::theme));
        collections.put(MongoCollections.DATABASE_SETTINGS, settings(settingsOverlay));
        return collections;
    }

    private Map<String, Long> writeMongo(ZipOutputStream zip, MongoTemplate mongo,
            Map<String, Map<String, String>> settingsOverlay) throws IOException {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String name : CseSiteFormat.ZIP_WRITE_ORDER) {
            long count = switch (name) {
                case MongoCollections.DATABASE_ROLES -> writeMapped(zip, name,
                        mongo.findAll(RoleEntity.class), CseSiteDocuments::role);
                case MongoCollections.DATABASE_ROLE_MATRIX -> writeMapped(zip, name,
                        mongo.findAll(RoleMatrixDocument.class), CseSiteDocuments::roleMatrix);
                case MongoCollections.DATABASE_ACCOUNTS -> writeMapped(zip, name,
                        mongo.findAll(AccountEntity.class), CseSiteDocuments::account);
                case MongoCollections.DATABASE_CATEGORIES -> writeMapped(zip, name,
                        mongo.findAll(CategoryEntity.class), CseSiteDocuments::category);
                case MongoCollections.DATABASE_RESOURCE_GROUPS -> writeMapped(zip, name,
                        mongo.findAll(ResourceGroupEntity.class), CseSiteDocuments::resourceGroup);
                case MongoCollections.DATABASE_ITEMS -> writeMapped(zip, name,
                        mongo.findAll(ItemEntity.class), CseSiteDocuments::item);
                case MongoCollections.DATABASE_PLAYLISTS -> writeMapped(zip, name,
                        mongo.findAll(PlaylistEntity.class), CseSiteDocuments::playlist);
                case MongoCollections.DATABASE_URL_TEMPLATES -> writeMapped(zip, name,
                        mongo.findAll(UrlTemplateEntity.class), CseSiteDocuments::urlTemplate);
                case MongoCollections.DATABASE_SETTINGS -> writeSettings(zip, mongo, settingsOverlay);
                case MongoCollections.DATABASE_APPS -> writeMapped(zip, name,
                        mongo.findAll(AppEntity.class), CseSiteDocuments::app);
                case MongoCollections.DATABASE_THEMES -> writeMapped(zip, name,
                        mongo.findAll(ThemeEntity.class), CseSiteDocuments::theme);
                case MongoCollections.DATABASE_RESOURCES -> writeMapped(zip, name,
                        mongo.findAll(ResourceEntity.class), CseSiteDocuments::resource);
                case MongoCollections.DATABASE_DATACHUNKS -> writeRawChunks(zip, mongo);
                default -> 0L;
            };
            counts.put(name, count);
            LOGGER.info("csesite export {}: {} documents", name, count);
        }
        writeAppStoreCollections(zip, mongo, counts);
        return counts;
    }

    private Map<String, Long> writeSnapshot(ZipOutputStream zip,
            Map<String, List<Map<String, Object>>> collections) throws IOException {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String name : CseSiteFormat.ZIP_WRITE_ORDER) {
            List<Map<String, Object>> docs = collections.getOrDefault(name, List.of());
            long count = writeMaps(zip, name, docs);
            counts.put(name, count);
            LOGGER.info("csesite export {}: {} documents", name, count);
        }
        return counts;
    }

    private long writeSettings(ZipOutputStream zip, MongoTemplate mongo,
            Map<String, Map<String, String>> overlay) throws IOException {
        List<Map<String, Object>> docs = new ArrayList<>();
        for (SettingContextEntity document : mongo.findAll(SettingContextEntity.class)) {
            if (document == null || document.getContext() == null
                    || SettingKeys.isSecretContext(document.getContext())) {
                continue;
            }
            docs.add(CseSiteDocuments.setting(document.getContext(), document.getValues()));
        }
        if (docs.isEmpty()) {
            docs = settings(overlay);
        }
        return writeMaps(zip, MongoCollections.DATABASE_SETTINGS, docs);
    }

    private long writeRawChunks(ZipOutputStream zip, MongoTemplate mongo) throws IOException {
        zip.putNextEntry(new ZipEntry(CseSiteFormat.collectionEntry(MongoCollections.DATABASE_DATACHUNKS)));
        JsonGenerator gen = openArray(zip);
        long count = 0;
        List<Document> documents = new ArrayList<>();
        mongo.getCollection(MongoCollections.DATABASE_DATACHUNKS).find().into(documents);
        for (Document document : documents) {
            try {
                MAPPER.writeValue(gen, CseSiteDocuments.chunkDocument(document));
                count++;
            } catch (Exception e) {
                LOGGER.warn("csesite: skip datachunk: {}", e.toString());
            }
        }
        closeArray(zip, gen);
        return count;
    }

    private void writeAppStoreCollections(ZipOutputStream zip, MongoTemplate mongo,
            Map<String, Long> counts) throws IOException {
        Set<String> slugs = new LinkedHashSet<>();
        List<AppEntity> apps = mongo.findAll(AppEntity.class);
        if (apps != null) {
            for (AppEntity app : apps) {
                if (app == null || app.getAppData() == null || app.getAppData().getSlug() == null) {
                    continue;
                }
                String slug = app.getAppData().getSlug().trim().toLowerCase(java.util.Locale.ROOT);
                if (AppStoreNames.isSlug(slug) && !AppStoreNames.isStoreIneligible(slug)) {
                    slugs.add(slug);
                }
            }
        }
        java.util.Collection<String> names = mongo.getCollectionNames();
        if (names == null || slugs.isEmpty()) {
            return;
        }
        List<String> extra = new ArrayList<>();
        for (String name : names) {
            String slug = AppStoreNames.slugOf(name);
            if (slug != null && slugs.contains(slug)) {
                extra.add(name);
            }
        }
        extra.sort(String::compareTo);
        for (String name : extra) {
            List<Map<String, Object>> docs = new ArrayList<>();
            List<Document> raw = new ArrayList<>();
            mongo.getCollection(name).find().into(raw);
            for (Document document : raw) {
                docs.add(AppStoreBson.toJson(document));
            }
            long count = writeMaps(zip, name, docs);
            counts.put(name, count);
            LOGGER.info("csesite export {}: {} documents", name, count);
        }
    }

    private <T> long writeMapped(ZipOutputStream zip, String collection, List<T> entities,
            Function<T, Map<String, Object>> mapper) throws IOException {
        zip.putNextEntry(new ZipEntry(CseSiteFormat.collectionEntry(collection)));
        JsonGenerator gen = openArray(zip);
        long count = 0;
        if (entities != null) {
            for (T entity : entities) {
                if (entity == null) {
                    continue;
                }
                try {
                    MAPPER.writeValue(gen, mapper.apply(entity));
                    count++;
                } catch (Exception e) {
                    LOGGER.warn("csesite: skip {} document: {}", collection, e.toString());
                }
            }
        }
        closeArray(zip, gen);
        return count;
    }

    private long writeMaps(ZipOutputStream zip, String collection, List<Map<String, Object>> docs)
            throws IOException {
        zip.putNextEntry(new ZipEntry(CseSiteFormat.collectionEntry(collection)));
        JsonGenerator gen = openArray(zip);
        long count = 0;
        if (docs != null) {
            for (Map<String, Object> doc : docs) {
                if (doc == null) {
                    continue;
                }
                MAPPER.writeValue(gen, doc);
                count++;
            }
        }
        closeArray(zip, gen);
        return count;
    }

    private static JsonGenerator openArray(ZipOutputStream zip) throws IOException {
        JsonGenerator gen = MAPPER.getFactory().createGenerator((OutputStream) zip, JsonEncoding.UTF8);
        gen.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        gen.writeStartArray();
        return gen;
    }

    private static void closeArray(ZipOutputStream zip, JsonGenerator gen) throws IOException {
        gen.writeEndArray();
        gen.flush();
        gen.close();
        zip.closeEntry();
    }

    private static Map<String, Object> manifest(String sourceEngine, Map<String, Long> counts) {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("format", CseSiteFormat.FORMAT);
        manifest.put("version", CseSiteFormat.VERSION);
        manifest.put("exportedAt", Instant.now().toString());
        manifest.put("sourceEngine", sourceEngine == null || sourceEngine.isBlank() ? "mongodb" : sourceEngine);
        Map<String, Object> collections = new LinkedHashMap<>();
        Set<String> names = new LinkedHashSet<>(CseSiteFormat.COLLECTIONS);
        names.addAll(counts.keySet());
        for (String name : names) {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("count", counts.getOrDefault(name, 0L));
            collections.put(name, meta);
        }
        manifest.put("collections", collections);
        return manifest;
    }

    private static List<Map<String, Object>> settings(Map<String, Map<String, String>> overlay) {
        List<Map<String, Object>> docs = new ArrayList<>();
        if (overlay == null) {
            return docs;
        }
        overlay.forEach((context, values) -> {
            if (context == null || SettingKeys.isSecretContext(context)) {
                return;
            }
            docs.add(CseSiteDocuments.setting(context, values));
        });
        return docs;
    }

    private static List<DataChunkEntity> loadChunks(ResourceService resources, Set<ObjectId> ids) {
        if (resources == null || ids.isEmpty()) {
            return List.of();
        }
        List<DataChunkEntity> loaded = resources.getDataChunks(new ArrayList<>(ids));
        return loaded == null ? List.of() : loaded;
    }

    private static <T> List<T> typed(Store service, Class<T> type) {
        if (service == null) {
            return List.of();
        }
        List<BasicEntity> raw = service.getAll();
        if (raw == null) {
            return List.of();
        }
        List<T> out = new ArrayList<>();
        for (BasicEntity entity : raw) {
            if (type.isInstance(entity)) {
                out.add(type.cast(entity));
            }
        }
        return out;
    }

    private static <T> List<Map<String, Object>> mapAll(List<T> entities, Function<T, Map<String, Object>> mapper) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (entities == null) {
            return out;
        }
        for (T entity : entities) {
            if (entity != null) {
                out.add(mapper.apply(entity));
            }
        }
        return out;
    }

    private static List<RoleEntity> roles(SiteServices services) {
        if (services == null || services.getRoleService() == null) {
            return List.of();
        }
        List<RoleEntity> all = services.getRoleService().getAll();
        return all == null ? List.of() : all;
    }

    private static RoleMatrixDocument matrix(SiteServices services) {
        if (services == null || services.getRoleMatrixService() == null) {
            return null;
        }
        return services.getRoleMatrixService().get();
    }

    /**
     * ZipOutputStream.close() would close the servlet stream and Tomcat can drop the buffer.
     */
    private static final class NonClosingOutputStream extends FilterOutputStream {
        private NonClosingOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
        }

        @Override
        public void close() throws IOException {
            flush();
        }
    }
}

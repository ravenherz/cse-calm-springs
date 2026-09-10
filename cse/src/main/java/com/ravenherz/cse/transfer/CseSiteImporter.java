package com.ravenherz.cse.transfer;

import com.ravenherz.cse.constants.MongoCollections;
import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.AppEntity;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.CategoryEntity;
import com.ravenherz.cse.dal.dto.DataChunkEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.PlaylistEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.ResourceGroupEntity;
import com.ravenherz.cse.dal.dto.SettingContextEntity;
import com.ravenherz.cse.dal.dto.ThemeEntity;
import com.ravenherz.cse.dal.dao.impl.AppStoreServiceImpl;
import com.ravenherz.cse.present.ResourceGroupIndex;
import com.ravenherz.cse.store.AppStoreNames;
import com.ravenherz.cse.util.Settings;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Component
public class CseSiteImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CseSiteImporter.class);

    private final ResourceGroupIndex resourceGroupIndex;

    public CseSiteImporter() {
        this.resourceGroupIndex = null;
    }

    @Autowired
    public CseSiteImporter(ResourceGroupIndex resourceGroupIndex) {
        this.resourceGroupIndex = resourceGroupIndex;
    }

    public ImportResult apply(InputStream zip, MongoTemplate mongo, Settings settings) throws IOException {
        if (mongo == null) {
            throw new CseSiteImportException("Database is not reachable.");
        }
        CseSiteArchive archive = CseSiteArchive.read(zip);
        ParsedSite parsed = parse(archive);
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put(MongoCollections.DATABASE_ACCOUNTS,
                replace(mongo, AccountEntity.class, parsed.accounts));
        counts.put(MongoCollections.DATABASE_CATEGORIES,
                replace(mongo, CategoryEntity.class, parsed.categories));
        counts.put(MongoCollections.DATABASE_RESOURCE_GROUPS,
                replace(mongo, ResourceGroupEntity.class, parsed.groups));
        counts.put(MongoCollections.DATABASE_DATACHUNKS,
                replace(mongo, DataChunkEntity.class, parsed.chunks));
        counts.put(MongoCollections.DATABASE_RESOURCES,
                replace(mongo, ResourceEntity.class, parsed.resources));
        counts.put(MongoCollections.DATABASE_ITEMS,
                replace(mongo, ItemEntity.class, parsed.items));
        counts.put(MongoCollections.DATABASE_PLAYLISTS,
                replace(mongo, PlaylistEntity.class, parsed.playlists));
        counts.put(MongoCollections.DATABASE_APPS,
                replace(mongo, AppEntity.class, parsed.apps));
        counts.put(MongoCollections.DATABASE_THEMES,
                replace(mongo, ThemeEntity.class, parsed.themes));
        counts.put(MongoCollections.DATABASE_SETTINGS, replaceSettings(mongo, parsed.settings, settings));
        importAppStores(archive, mongo, counts);
        if (settings != null) {
            settings.reloadFromMongo();
        }
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            LOGGER.info("csesite import {}: {} documents", entry.getKey(), entry.getValue());
        }
        if (resourceGroupIndex != null) {
            resourceGroupIndex.rebuild();
        }
        return new ImportResult(counts);
    }

    private static ParsedSite parse(CseSiteArchive archive) {
        ParsedSite parsed = new ParsedSite();
        parsed.accounts = readAll(archive.collection(MongoCollections.DATABASE_ACCOUNTS), CseSiteReaders::account);
        parsed.categories = readAll(archive.collection(MongoCollections.DATABASE_CATEGORIES), CseSiteReaders::category);
        parsed.groups = readAll(archive.collection(MongoCollections.DATABASE_RESOURCE_GROUPS),
                CseSiteReaders::resourceGroup);
        parsed.chunks = readAll(archive.collection(MongoCollections.DATABASE_DATACHUNKS), CseSiteReaders::chunk);
        parsed.resources = readAll(archive.collection(MongoCollections.DATABASE_RESOURCES), CseSiteReaders::resource);
        parsed.items = readAll(archive.collection(MongoCollections.DATABASE_ITEMS), CseSiteReaders::item);
        parsed.playlists = readAll(archive.collection(MongoCollections.DATABASE_PLAYLISTS), CseSiteReaders::playlist);
        parsed.apps = readAll(archive.collection(MongoCollections.DATABASE_APPS), CseSiteReaders::app);
        parsed.themes = readAll(archive.collection(MongoCollections.DATABASE_THEMES), CseSiteReaders::theme);
        parsed.settings = new ArrayList<>();
        for (Map<String, Object> doc : archive.collection(MongoCollections.DATABASE_SETTINGS)) {
            SettingContextEntity entity = CseSiteReaders.setting(doc);
            if (entity != null) {
                parsed.settings.add(entity);
            }
        }
        return parsed;
    }

    private static <T> List<T> readAll(List<Map<String, Object>> docs, Function<Map<String, Object>, T> reader) {
        List<T> out = new ArrayList<>();
        if (docs == null) {
            return out;
        }
        for (Map<String, Object> doc : docs) {
            T entity = reader.apply(doc);
            if (entity != null) {
                out.add(entity);
            }
        }
        return out;
    }

    private static <T extends BasicEntity> int replace(MongoTemplate mongo, Class<T> type, List<T> incoming) {
        Set<ObjectId> keep = new HashSet<>();
        for (T entity : incoming) {
            if (entity == null || entity.getId() == null) {
                continue;
            }
            mongo.save(entity);
            keep.add(entity.getId());
        }
        for (T existing : mongo.findAll(type)) {
            if (existing != null && existing.getId() != null && !keep.contains(existing.getId())) {
                mongo.remove(existing);
            }
        }
        return incoming.size();
    }

    private static int replaceSettings(MongoTemplate mongo, List<SettingContextEntity> incoming, Settings settings) {
        Set<String> keep = new HashSet<>();
        int stored = 0;
        for (SettingContextEntity entity : incoming) {
            if (entity == null || entity.getContext() == null) {
                continue;
            }
            if (settings != null && !settings.isOverlayContext(entity.getContext())) {
                continue;
            }
            if (settings == null && !isOverlayWithoutSettings(entity.getContext())) {
                continue;
            }
            mongo.save(entity);
            keep.add(entity.getContext());
            stored++;
        }
        for (SettingContextEntity existing : mongo.findAll(SettingContextEntity.class)) {
            if (existing == null || existing.getContext() == null) {
                continue;
            }
            boolean overlay = settings == null
                    ? isOverlayWithoutSettings(existing.getContext())
                    : settings.isOverlayContext(existing.getContext());
            if (overlay && !keep.contains(existing.getContext())) {
                mongo.remove(existing);
            }
        }
        return stored;
    }

    private static void importAppStores(CseSiteArchive archive, MongoTemplate mongo,
            Map<String, Integer> counts) {
        Set<String> imported = new HashSet<>();
        for (Map.Entry<String, List<Map<String, Object>>> extra : archive.extraCollections().entrySet()) {
            String name = extra.getKey();
            if (name != null && name.startsWith("cse-")) {
                LOGGER.warn("csesite: skip unknown CMS collection {}", name);
                continue;
            }
            if (!AppStoreNames.isAppCollection(name)) {
                LOGGER.warn("csesite: skip extra collection {}", name);
                continue;
            }
            List<Map<String, Object>> docs = extra.getValue() == null ? List.of() : extra.getValue();
            replaceAppStore(mongo, name, docs);
            imported.add(name);
            counts.put(name, docs.size());
        }
        java.util.Collection<String> live = mongo.getCollectionNames();
        if (live == null) {
            return;
        }
        for (String name : live) {
            if (AppStoreNames.isAppCollection(name) && !imported.contains(name)) {
                mongo.dropCollection(name);
            }
        }
    }

    private static void replaceAppStore(MongoTemplate mongo, String collection,
            List<Map<String, Object>> docs) {
        mongo.dropCollection(collection);
        List<Document> rows = new ArrayList<>();
        for (Map<String, Object> doc : docs) {
            Document bson = AppStoreServiceImpl.toBson(doc);
            if (bson != null) {
                rows.add(bson);
            }
        }
        if (!rows.isEmpty()) {
            mongo.insert(rows, collection);
        }
    }

    private static boolean isOverlayWithoutSettings(String context) {
        return context != null
                && !SettingKeys.isSecretContext(context)
                && !SettingKeys.CONTEXT_DATASOURCE_BUILD_INFO.equals(context)
                && !"config-dbms-instance".equals(context)
                && !"config-dbms-access".equals(context);
    }

    public record ImportResult(Map<String, Integer> counts) {
    }

    private static final class ParsedSite {
        private List<AccountEntity> accounts = List.of();
        private List<CategoryEntity> categories = List.of();
        private List<ResourceGroupEntity> groups = List.of();
        private List<DataChunkEntity> chunks = List.of();
        private List<ResourceEntity> resources = List.of();
        private List<ItemEntity> items = List.of();
        private List<PlaylistEntity> playlists = List.of();
        private List<AppEntity> apps = List.of();
        private List<ThemeEntity> themes = List.of();
        private List<SettingContextEntity> settings = List.of();
    }
}

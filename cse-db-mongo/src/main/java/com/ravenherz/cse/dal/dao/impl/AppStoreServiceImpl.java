package com.ravenherz.cse.dal.dao.impl;

import com.mongodb.client.MongoCollection;
import com.ravenherz.cse.dal.DataProvider;
import com.ravenherz.cse.dal.dao.AppService;
import com.ravenherz.cse.dal.dao.AppStoreService;
import com.ravenherz.cse.dal.dao.BasicService;
import com.ravenherz.cse.dal.dto.AppEntity;
import com.ravenherz.cse.dal.dto.basic.AppData;
import com.ravenherz.cse.dal.dto.basic.AppStoreSettings;
import com.ravenherz.cse.store.AppStoreBson;
import com.ravenherz.cse.store.AppStoreDocument;
import com.ravenherz.cse.store.AppStoreException;
import com.ravenherz.cse.store.AppStoreNames;
import com.ravenherz.cse.store.AppStoreTableSpec;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Repository(value = "appStoreService")
public class AppStoreServiceImpl extends BasicService implements AppStoreService {

    private final AppService appService;

    public AppStoreServiceImpl(DataProvider dataProvider, AppService appService) {
        super(dataProvider);
        this.appService = appService;
    }

    @Override
    public AppEntity requireApp(String slug) {
        String normalized = slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT);
        if (!AppStoreNames.isSlug(normalized) || AppStoreNames.isStoreIneligible(normalized)) {
            throw new AppStoreException(404, "Unknown app");
        }
        AppEntity app = appService.getBySlug(normalized);
        if (app == null || app.getAppData() == null) {
            throw new AppStoreException(404, "Unknown app");
        }
        return app;
    }

    @Override
    public boolean isStoreEnabled(String slug) {
        return requireApp(slug).getAppData().isStoreEnabled();
    }

    @Override
    public List<AppStoreTableSpec> tables(String slug) {
        return new ArrayList<>(requireEnabled(slug).getAppData().getStoreTables());
    }

    @Override
    public AppStoreTableSpec requireTable(String slug, String table) {
        return findTable(requireEnabled(slug), table, false);
    }

    @Override
    public AppStoreTableSpec requireTableOrCreate(String slug, String table) {
        return findTable(requireEnabled(slug), table, true);
    }

    @Override
    public void setGrant(String slug, boolean enabled, boolean open) {
        AppStoreSettings settings = AppStoreSettings.copyOf(requireApp(slug).getAppData().storeSettings());
        settings.setEnabled(enabled);
        settings.setSchemaOpen(open);
        updateStoreSettings(slug, settings);
    }

    @Override
    public void updateStoreSettings(String slug, AppStoreSettings settings) {
        AppEntity app = requireApp(slug);
        app.getAppData().applyStoreSettings(settings);
        appService.replace(app);
    }

    @Override
    public void defineTable(String slug, AppStoreTableSpec spec) {
        if (spec == null || spec.getName() == null) {
            throw new AppStoreException(400, "Invalid table name");
        }
        AppEntity app = requireEnabled(slug);
        String table = AppStoreNames.requireTable(spec.getName());
        List<AppStoreTableSpec> tables = new ArrayList<>(app.getAppData().getStoreTables());
        boolean replaced = false;
        for (int i = 0; i < tables.size(); i++) {
            if (table.equals(tables.get(i).getName())) {
                tables.set(i, new AppStoreTableSpec(table, spec.getAccess(), spec.getSchema()));
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            if (!app.getAppData().isStoreOpen()) {
                throw new AppStoreException(403, "Table is not on the allowlist");
            }
            tables.add(new AppStoreTableSpec(table, spec.getAccess(), spec.getSchema()));
        }
        app.getAppData().setStoreTables(tables);
        appService.replace(app);
        ensureIndexes(AppStoreNames.collectionName(slug, table));
    }

    @Override
    public AppStoreDocument insert(String slug, String table, String ownerId, Map<String, Object> data) {
        AppStoreTableSpec spec = requireTableOrCreate(slug, table);
        Map<String, Object> payload = copyData(data);
        int bytes = dataBytes(payload);
        AppStoreSettings settings = requireApp(slug).getAppData().storeSettings();
        if (bytes > settings.resolvedMaxDataBytes()) {
            throw new AppStoreException(413, "Document data is too large");
        }
        String collection = AppStoreNames.collectionName(slug, spec.getName());
        checkQuota(slug, bytes);
        Instant now = Instant.now();
        ObjectId id = new ObjectId();
        Document document = new Document();
        document.put("_id", id);
        document.put("ownerId", ownerId);
        document.put("createdAt", now.toString());
        document.put("updatedAt", now.toString());
        document.put("data", new Document(payload));
        mongo().insert(document, collection);
        ensureIndexes(collection);
        return fromBson(document);
    }

    @Override
    public AppStoreDocument get(String slug, String table, String id) {
        AppStoreTableSpec spec = requireTable(slug, table);
        ObjectId objectId = objectId(id);
        Document document = mongo().findById(objectId, Document.class,
                AppStoreNames.collectionName(slug, spec.getName()));
        if (document == null) {
            throw new AppStoreException(404, "Unknown document");
        }
        return fromBson(document);
    }

    @Override
    public AppStoreList list(String slug, String table, String ownerId, boolean mineOnly,
            String after, int limit) {
        AppStoreTableSpec spec = requireTable(slug, table);
        int cap = Math.min(Math.max(limit, 1), MAX_LIST);
        Query query = new Query();
        if (mineOnly && ownerId != null) {
            query.addCriteria(Criteria.where("ownerId").is(ownerId));
        }
        if (after != null && !after.isBlank()) {
            query.addCriteria(Criteria.where("_id").gt(objectId(after)));
        }
        query.with(Sort.by(Sort.Direction.ASC, "_id"));
        query.limit(cap + 1);
        List<Document> found = mongo().find(query, Document.class,
                AppStoreNames.collectionName(slug, spec.getName()));
        List<AppStoreDocument> items = new ArrayList<>();
        String next = null;
        for (int i = 0; i < found.size(); i++) {
            if (i >= cap) {
                next = items.get(items.size() - 1).getId();
                break;
            }
            items.add(fromBson(found.get(i)));
        }
        return new AppStoreList(items, next);
    }

    @Override
    public AppStoreDocument patch(String slug, String table, String id, Map<String, Object> dataPatch) {
        AppStoreTableSpec spec = requireTable(slug, table);
        String collection = AppStoreNames.collectionName(slug, spec.getName());
        Document existing = mongo().findById(objectId(id), Document.class, collection);
        if (existing == null) {
            throw new AppStoreException(404, "Unknown document");
        }
        Map<String, Object> data = copyData(asMap(existing.get("data")));
        if (dataPatch != null) {
            for (Map.Entry<String, Object> entry : dataPatch.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                if (entry.getValue() == null) {
                    data.remove(entry.getKey());
                } else {
                    data.put(entry.getKey(), entry.getValue());
                }
            }
        }
        int bytes = dataBytes(data);
        AppStoreSettings settings = requireApp(slug).getAppData().storeSettings();
        if (bytes > settings.resolvedMaxDataBytes()) {
            throw new AppStoreException(413, "Document data is too large");
        }
        existing.put("data", new Document(data));
        existing.put("updatedAt", Instant.now().toString());
        mongo().save(existing, collection);
        return fromBson(existing);
    }

    @Override
    public void delete(String slug, String table, String id) {
        AppStoreTableSpec spec = requireTable(slug, table);
        Query query = Query.query(Criteria.where("_id").is(objectId(id)));
        var result = mongo().remove(query, AppStoreNames.collectionName(slug, spec.getName()));
        if (result.getDeletedCount() == 0) {
            throw new AppStoreException(404, "Unknown document");
        }
    }

    @Override
    public List<String> collectionNames(String slug) {
        String normalized = slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT);
        if (!AppStoreNames.isSlug(normalized) || AppStoreNames.isStoreIneligible(normalized)) {
            return List.of();
        }
        Collection<String> names = mongo().getCollectionNames();
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String name : names) {
            if (AppStoreNames.belongsTo(name, normalized)) {
                out.add(name);
            }
        }
        return out;
    }

    @Override
    public void replaceCollection(String collection, List<Map<String, Object>> documents) {
        if (!AppStoreNames.isAppCollection(collection)) {
            throw new AppStoreException(400, "Invalid collection");
        }
        MongoTemplate mongo = mongo();
        mongo.dropCollection(collection);
        if (documents == null || documents.isEmpty()) {
            return;
        }
        List<Document> rows = new ArrayList<>();
        for (Map<String, Object> row : documents) {
            Document bson = toBson(row);
            if (bson != null) {
                rows.add(bson);
            }
        }
        if (!rows.isEmpty()) {
            mongo.insert(rows, collection);
            ensureIndexes(collection);
        }
    }

    @Override
    public void dropCollectionsNotIn(List<String> keepCollections) {
        Set<String> keep = keepCollections == null ? Set.of() : Set.copyOf(keepCollections);
        Collection<String> names = mongo().getCollectionNames();
        if (names == null) {
            return;
        }
        for (String name : names) {
            if (AppStoreNames.isAppCollection(name) && !keep.contains(name)) {
                mongo().dropCollection(name);
            }
        }
    }

    private AppEntity requireEnabled(String slug) {
        AppEntity app = requireApp(slug);
        if (!app.getAppData().isStoreEnabled()) {
            throw new AppStoreException(403, "Data store is not enabled");
        }
        return app;
    }

    private AppStoreTableSpec findTable(AppEntity app, String table, boolean createIfOpen) {
        String normalized = AppStoreNames.requireTable(table);
        if (AppStoreNames.isReservedTable(normalized)) {
            throw new AppStoreException(400, "Invalid table name");
        }
        AppData data = app.getAppData();
        for (AppStoreTableSpec spec : data.getStoreTables()) {
            if (normalized.equals(spec.getName())) {
                return spec;
            }
        }
        if (!createIfOpen || !data.isStoreOpen()) {
            throw new AppStoreException(404, "Unknown table");
        }
        AppStoreTableSpec spec = new AppStoreTableSpec(normalized, null, null);
        List<AppStoreTableSpec> tables = new ArrayList<>(data.getStoreTables());
        tables.add(spec);
        data.setStoreTables(tables);
        appService.replace(app);
        return spec;
    }

    private void checkQuota(String slug, int extraBytes) {
        AppStoreSettings settings = requireApp(slug).getAppData().storeSettings();
        long docs = 0;
        long bytes = extraBytes;
        for (String collection : collectionNames(slug)) {
            docs += mongo().count(new Query(), collection);
            bytes += collectionSize(collection);
        }
        if (docs >= settings.resolvedMaxDocs()) {
            throw new AppStoreException(507, "App data quota exceeded");
        }
        if (bytes > settings.resolvedMaxBytes()) {
            throw new AppStoreException(507, "App data quota exceeded");
        }
    }

    private long collectionSize(String collection) {
        try {
            Document stats = mongo().executeCommand(new Document("collStats", collection));
            if (stats == null) {
                return 0;
            }
            Number size = stats.get("size", Number.class);
            return size == null ? 0 : size.longValue();
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private void ensureIndexes(String collection) {
        try {
            MongoCollection<Document> coll = mongo().getCollection(collection);
            coll.createIndex(new Document("ownerId", 1));
        } catch (RuntimeException ignored) {
            // Collection may not exist yet, or the index already does.
        }
    }

    private static ObjectId objectId(String id) {
        if (id == null || !ObjectId.isValid(id)) {
            throw new AppStoreException(400, "Invalid document id");
        }
        return new ObjectId(id);
    }

    private static Map<String, Object> copyData(Map<String, Object> data) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (data == null) {
            return out;
        }
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            if ("id".equals(entry.getKey()) || "ownerId".equals(entry.getKey())
                    || "createdAt".equals(entry.getKey()) || "updatedAt".equals(entry.getKey())) {
                continue;
            }
            out.put(entry.getKey(), entry.getValue());
        }
        return out;
    }

    private static int dataBytes(Map<String, Object> data) {
        try {
            return new Document(data == null ? Map.of() : data)
                    .toJson()
                    .getBytes(StandardCharsets.UTF_8).length;
        } catch (RuntimeException ex) {
            throw new AppStoreException(400, "Invalid document data");
        }
    }

    public static AppStoreDocument fromBson(Document bson) {
        return AppStoreBson.fromBson(bson);
    }

    public static Document toBson(Map<String, Object> json) {
        return AppStoreBson.toBson(json);
    }

    public static Map<String, Object> toJson(Document bson) {
        return AppStoreBson.toJson(bson);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object raw) {
        if (raw instanceof Document document) {
            return new LinkedHashMap<>(document);
        }
        if (raw instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    out.put(entry.getKey().toString(), entry.getValue());
                }
            }
            return out;
        }
        return new LinkedHashMap<>();
    }
}

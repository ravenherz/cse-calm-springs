package com.ravenherz.cse.dal.dao;

import com.ravenherz.cse.dal.dto.AppEntity;
import com.ravenherz.cse.dal.dto.basic.AppStoreSettings;
import com.ravenherz.cse.store.AppStoreDocument;
import com.ravenherz.cse.store.AppStoreTableSpec;

import java.util.List;
import java.util.Map;

public interface AppStoreService {

    int MAX_LIST = 100;
    int MAX_DATA_BYTES = AppStoreSettings.DEFAULT_MAX_DATA_BYTES;
    int MAX_DOCS_PER_APP = AppStoreSettings.DEFAULT_MAX_DOCS;
    long MAX_BYTES_PER_APP = AppStoreSettings.DEFAULT_MAX_BYTES;

    AppEntity requireApp(String slug);

    boolean isStoreEnabled(String slug);

    List<AppStoreTableSpec> tables(String slug);

    AppStoreTableSpec requireTable(String slug, String table);

    AppStoreTableSpec requireTableOrCreate(String slug, String table);

    void setGrant(String slug, boolean enabled, boolean open);

    void updateStoreSettings(String slug, AppStoreSettings settings);

    void defineTable(String slug, AppStoreTableSpec spec);

    AppStoreDocument insert(String slug, String table, String ownerId, Map<String, Object> data);

    AppStoreDocument get(String slug, String table, String id);

    AppStoreList list(String slug, String table, String ownerId, boolean mineOnly, String after, int limit);

    AppStoreDocument patch(String slug, String table, String id, Map<String, Object> dataPatch);

    void delete(String slug, String table, String id);

    List<String> collectionNames(String slug);

    void replaceCollection(String collection, List<Map<String, Object>> documents);

    void dropCollectionsNotIn(List<String> keepCollections);

    record AppStoreList(List<AppStoreDocument> items, String after) {
    }
}

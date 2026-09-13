package com.ravenherz.cse.transfer;

import com.ravenherz.cse.constants.MongoCollections;

import java.util.List;

/**
 * Portable site archive ({@code .csesite}). JSON collections with opaque string ids.
 * Mongo import/export live in this package. Spec: {@code internal-docs/cse-site-format.md}.
 */
public final class CseSiteFormat {

    public static final String FORMAT = "cse-site";
    public static final int VERSION = 1;
    public static final String EXTENSION = "csesite";
    public static final String MANIFEST_ENTRY = "manifest.json";
    public static final String COLLECTIONS_DIR = "collections/";

    /**
     * Suggested import order (parents before refs). Not the zip physical order.
     */
    public static final List<String> COLLECTIONS = List.of(
            MongoCollections.DATABASE_ROLES,
            MongoCollections.DATABASE_ROLE_MATRIX,
            MongoCollections.DATABASE_ACCOUNTS,
            MongoCollections.DATABASE_CATEGORIES,
            MongoCollections.DATABASE_RESOURCE_GROUPS,
            MongoCollections.DATABASE_DATACHUNKS,
            MongoCollections.DATABASE_RESOURCES,
            MongoCollections.DATABASE_ITEMS,
            MongoCollections.DATABASE_PLAYLISTS,
            MongoCollections.DATABASE_APPS,
            MongoCollections.DATABASE_THEMES,
            MongoCollections.DATABASE_SETTINGS);

    /**
     * Zip write order: pages and other metadata first so a truncated download still has them.
     * Blobs last. Importers must use {@link #COLLECTIONS}, not zip order.
     */
    public static final List<String> ZIP_WRITE_ORDER = List.of(
            MongoCollections.DATABASE_ROLES,
            MongoCollections.DATABASE_ROLE_MATRIX,
            MongoCollections.DATABASE_ACCOUNTS,
            MongoCollections.DATABASE_CATEGORIES,
            MongoCollections.DATABASE_RESOURCE_GROUPS,
            MongoCollections.DATABASE_ITEMS,
            MongoCollections.DATABASE_PLAYLISTS,
            MongoCollections.DATABASE_SETTINGS,
            MongoCollections.DATABASE_APPS,
            MongoCollections.DATABASE_THEMES,
            MongoCollections.DATABASE_RESOURCES,
            MongoCollections.DATABASE_DATACHUNKS);

    private CseSiteFormat() {
    }

    public static String collectionEntry(String collection) {
        return COLLECTIONS_DIR + collection + ".json";
    }
}

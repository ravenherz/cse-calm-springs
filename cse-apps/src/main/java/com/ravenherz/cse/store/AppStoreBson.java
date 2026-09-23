package com.ravenherz.cse.store;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * App-store rows as BSON. Shared by the Mongo adapter and site transfer.
 */
public final class AppStoreBson {

    private AppStoreBson() {
    }

    public static AppStoreDocument fromBson(Document bson) {
        AppStoreDocument out = new AppStoreDocument();
        if (bson == null) {
            return out;
        }
        Object id = bson.get("_id");
        if (id instanceof ObjectId objectId) {
            out.setId(objectId.toHexString());
        } else if (id != null) {
            out.setId(id.toString());
        }
        Object owner = bson.get("ownerId");
        out.setOwnerId(owner == null ? null : owner.toString());
        Object created = bson.get("createdAt");
        out.setCreatedAt(created == null ? null : created.toString());
        Object updated = bson.get("updatedAt");
        out.setUpdatedAt(updated == null ? null : updated.toString());
        out.setData(asMap(bson.get("data")));
        return out;
    }

    public static Document toBson(Map<String, Object> json) {
        if (json == null) {
            return null;
        }
        Object rawId = json.get("id");
        if (rawId == null || !ObjectId.isValid(rawId.toString())) {
            return null;
        }
        Document document = new Document();
        document.put("_id", new ObjectId(rawId.toString()));
        Object owner = json.get("ownerId");
        document.put("ownerId", owner == null ? null : owner.toString());
        Object created = json.get("createdAt");
        if (created != null) {
            document.put("createdAt", created.toString());
        }
        Object updated = json.get("updatedAt");
        if (updated != null) {
            document.put("updatedAt", updated.toString());
        }
        document.put("data", new Document(asMap(json.get("data"))));
        return document;
    }

    public static Map<String, Object> toJson(Document bson) {
        return fromBson(bson).toMap();
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

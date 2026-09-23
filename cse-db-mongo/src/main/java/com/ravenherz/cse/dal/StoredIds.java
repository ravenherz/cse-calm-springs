package com.ravenherz.cse.dal;

import org.bson.types.ObjectId;

/**
 * Bridge while a feature document still stores a foreign key as {@link ObjectId}.
 * Document ids themselves are {@link EntityId}. Mongo reads and writes those inside cse-db-mongo.
 */
public final class StoredIds {

    private StoredIds() {
    }

    public static ObjectId objectId(EntityId id) {
        return id == null ? null : new ObjectId(id.toHexString());
    }

    public static EntityId entityId(ObjectId id) {
        return id == null ? null : EntityId.of(id.toHexString());
    }
}

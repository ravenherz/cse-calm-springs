package com.ravenherz.cse.dal;

import org.bson.types.ObjectId;

/** ObjectId bridge for media foreign keys that are still stored as ObjectId. */
public final class MediaIds {

    private MediaIds() {
    }

    public static ObjectId objectId(EntityId id) {
        return id == null ? null : new ObjectId(id.toHexString());
    }
}

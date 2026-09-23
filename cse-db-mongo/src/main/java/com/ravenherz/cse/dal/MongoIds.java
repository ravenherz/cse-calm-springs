package com.ravenherz.cse.dal;

import org.bson.types.ObjectId;

public final class MongoIds {

    private MongoIds() {
    }

    public static ObjectId toObjectId(EntityId id) {
        return id == null ? null : new ObjectId(id.hex());
    }

    public static EntityId toEntityId(ObjectId id) {
        return id == null ? null : EntityId.of(id.toHexString());
    }
}

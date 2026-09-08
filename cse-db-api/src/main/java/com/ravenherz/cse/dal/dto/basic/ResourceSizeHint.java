package com.ravenherz.cse.dal.dto.basic;

import org.bson.types.ObjectId;

/**
 * Lightweight resource row for the group index. {@code groupId} is null for files
 * not assigned to a group (shown under Unsorted when that group exists).
 * {@code id} / {@code pathPublic} may be null in tests that only care about counts.
 */
public record ResourceSizeHint(ObjectId id, ObjectId groupId, String pathPublic, long sizeInBytes) {

    public ResourceSizeHint(ObjectId groupId, long sizeInBytes) {
        this(null, groupId, null, sizeInBytes);
    }

    public String fileName() {
        if (pathPublic == null || pathPublic.isEmpty()) {
            return "";
        }
        int slash = pathPublic.lastIndexOf('/');
        return slash >= 0 ? pathPublic.substring(slash + 1) : pathPublic;
    }
}

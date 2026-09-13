package com.ravenherz.cse.dal.dto.basic;

import org.bson.types.ObjectId;

/**
 * Image bytes used to build the editor tree's 64×64 thumbs. Prefer the stored
 * low-res preview; originals are only included when there is no preview.
 * Held only while one thumb is encoded, then discarded.
 */
public record ResourcePreviewSource(ObjectId id, byte[] bytes) {
}

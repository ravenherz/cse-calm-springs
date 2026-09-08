package com.ravenherz.cse.dal.dao;

import com.ravenherz.cse.dal.dto.DataChunkEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.ResourceGroupEntity;
import com.ravenherz.cse.dal.dto.basic.ResourcePreviewSource;
import com.ravenherz.cse.dal.dto.basic.ResourceSizeHint;
import org.bson.types.ObjectId;

import java.util.List;

public interface ResourceService extends Service {

    ResourceEntity getByPublicPath(String publicPath);
    ResourceEntity getByProtectedPath(String protectedPath);
    void deleteByPublicPath(String publicPath);
    List<DataChunkEntity> getDataChunks(List<ObjectId> chunkIds);
    byte[] getRawBytesFromChunks(List<ObjectId> chunkIds);
    void saveDataChunk(DataChunkEntity chunk);
    List<ResourceEntity> getImagesByGroup(ResourceGroupEntity group);

    /**
     * Group, size, and public path for the editor tree. Does not load {@code contentRaw}
     * or hydrate parent groups.
     */
    List<ResourceSizeHint> listSizeHints();

    /**
     * Preview or (small) original image bytes for tree thumbs. Skips audio
     * originals and large files without a stored preview.
     */
    default List<ResourcePreviewSource> listPreviewSources() {
        return List.of();
    }

    /**
     * Files in one group for the Resources pane. {@code groupId == null} is files
     * with no group; the editor shows those under Unsorted.
     * Omits {@code contentRaw} on the file and preview.
     */
    List<ResourceEntity> listForEditor(ObjectId groupId);
}


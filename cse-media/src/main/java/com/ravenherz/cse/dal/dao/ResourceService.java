package com.ravenherz.cse.dal.dao;

import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.DataChunkEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.ResourceGroupEntity;
import com.ravenherz.cse.dal.dto.basic.ResourceData;
import com.ravenherz.cse.dal.dto.basic.ResourcePreviewSource;
import com.ravenherz.cse.dal.dto.basic.ResourceSizeHint;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public interface ResourceService extends Store {

    ResourceEntity getByPublicPath(String publicPath);
    ResourceEntity getByProtectedPath(String protectedPath);
    void deleteByPublicPath(String publicPath);
    List<DataChunkEntity> getDataChunks(List<ObjectId> chunkIds);
    byte[] getRawBytesFromChunks(List<ObjectId> chunkIds);
    void saveDataChunk(DataChunkEntity chunk);

    /**
     * Ids of every {@code cse-datachunks} row. Does not load chunk bytes.
     */
    List<ObjectId> listDataChunkIds();

    void deleteDataChunk(ObjectId id);
    void fillFromFile(ResourceData data, Path file) throws IOException;
    void writeToFile(ResourceData data, Path dest) throws IOException;
    void deleteStoredContent(ResourceData data);
    List<ObjectId> listProcessingVideoIds();
    List<ResourceEntity> getImagesByGroup(ResourceGroupEntity group);

    /**
     * All resources including {@code contentRaw}. For site export only.
     * Editor lists must use {@link #getAll()}, which omits binaries.
     */
    default List<ResourceEntity> listWithContent() {
        List<ResourceEntity> out = new ArrayList<>();
        List<BasicEntity> all = getAll();
        if (all == null) {
            return out;
        }
        for (BasicEntity entity : all) {
            if (entity instanceof ResourceEntity resource) {
                out.add(resource);
            }
        }
        return out;
    }

    /**
     * Group, size, and public path for the editor tree. Does not load {@code contentRaw}
     * or hydrate parent groups.
     */
    List<ResourceSizeHint> listSizeHints();

    /**
     * Preview or (small) original image bytes for tree thumbs. Skips audio
     * originals and large files without a stored preview.
     * <p>
     * Prefer {@link #forEachPreviewSource(Consumer)} so callers can discard
     * each JPEG before the next one is loaded.
     */
    default List<ResourcePreviewSource> listPreviewSources() {
        List<ResourcePreviewSource> out = new ArrayList<>();
        forEachPreviewSource(out::add);
        return out;
    }

    /**
     * Same sources as {@link #listPreviewSources()}, one at a time. The
     * consumer must not keep {@link ResourcePreviewSource#bytes()} if it
     * can finish with them first.
     */
    default void forEachPreviewSource(Consumer<ResourcePreviewSource> consumer) {
    }

    /**
     * Files in one group for the Resources pane. {@code groupId == null} is files
     * with no group; the editor shows those under Unsorted.
     * Omits {@code contentRaw} on the file and preview.
     */
    List<ResourceEntity> listForEditor(ObjectId groupId);
}


package com.ravenherz.rhzwe.dal.dao;

import com.ravenherz.rhzwe.dal.dto.DataChunkEntity;
import com.ravenherz.rhzwe.dal.dto.ResourceEntity;
import org.bson.types.ObjectId;

import java.util.List;

public interface ResourceService extends Service {

    ResourceEntity getByPublicPath(String publicPath);
    ResourceEntity getByProtectedPath(String protectedPath);
    void deleteByPublicPath(String publicPath);
    List<DataChunkEntity> getDataChunks(List<ObjectId> chunkIds);
    byte[] getRawBytesFromChunks(List<ObjectId> chunkIds);
    void saveDataChunk(DataChunkEntity chunk);
}

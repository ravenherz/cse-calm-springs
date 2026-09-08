package com.ravenherz.cse.dal.dao;

import com.ravenherz.cse.dal.dto.AppEntity;
import com.ravenherz.cse.dal.dto.DataChunkEntity;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.stream.Collectors;

public interface AppService extends Service {

    AppEntity getBySlug(String slug);

    void delete(AppEntity app);

    void deleteChunks(AppEntity app);

    void saveDataChunk(DataChunkEntity chunk);

    byte[] getRawBytesFromChunks(List<ObjectId> chunkIds);

    byte[] loadZipBytes(AppEntity app);

    default List<AppEntity> getAllApps() {
        return getAll().stream()
                .filter(e -> e instanceof AppEntity)
                .map(e -> (AppEntity) e)
                .collect(Collectors.toList());
    }
}

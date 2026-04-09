package com.ravenherz.rhzwe.dal.dao.impl;

import com.ravenherz.rhzwe.dal.dao.BasicService;
import com.ravenherz.rhzwe.dal.dao.ResourceService;
import com.ravenherz.rhzwe.dal.dto.BasicEntity;
import com.ravenherz.rhzwe.dal.dto.DataChunkEntity;
import com.ravenherz.rhzwe.dal.dto.ResourceEntity;
import dev.morphia.query.filters.Filters;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Lazy
@Repository(value = "resourceService")
@Scope(value = "singleton")
public class ResourceServiceImpl extends BasicService implements ResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceServiceImpl.class);

    @Override
    public ResourceEntity getByPublicPath(String publicPath) {
        return dataProvider.getDatastore().find(ResourceEntity.class)
                .filter(Filters.eq("resourceData.pathPublic", publicPath))
                .first();
    }

    @Override
    public List<BasicEntity> getAll() {
        return dataProvider.getDatastore().find(ResourceEntity.class).stream().collect(Collectors.toList());
    }

    @Override
    public ResourceEntity getByProtectedPath(String protectedPath) {
        return dataProvider.getDatastore().find(ResourceEntity.class)
                .filter(Filters.eq("resourceData.pathProtected", protectedPath))
                .first();
    }

    @Override
    public void deleteByPublicPath(String publicPath) {
        ResourceEntity entity = getByPublicPath(publicPath);
        if (entity != null) {
            String filePath = "/var/cse/content-cache" + entity.getResourceData().getPathProtected();
            java.io.File cacheFile = new java.io.File(filePath);
            if (cacheFile.exists()) {
                if (cacheFile.delete()) LOGGER.info("Deleted cache file: " + filePath);
                else LOGGER.warn("Failed to delete cache file: " + filePath);
            } else LOGGER.warn("Cache file does not exist: " + filePath);
            if (entity.getResourceData().isLargeFile() && entity.getResourceData().getDataChunkIds() != null) {
                entity.getResourceData().getDataChunkIds().forEach(id -> {
                    DataChunkEntity chunk = dataProvider.getDatastore().find(DataChunkEntity.class)
                            .filter(Filters.eq("id", id))
                            .first();
                    if (chunk != null) {
                        dataProvider.getDatastore().delete(chunk);
                        LOGGER.info("Deleted data chunk: " + id);
                    }
                });
            }

            dataProvider.getDatastore().delete(entity);
        }
    }

    @Override
    public List<DataChunkEntity> getDataChunks(List<ObjectId> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<DataChunkEntity> chunks = new ArrayList<>();
        for (ObjectId id : chunkIds) {
            DataChunkEntity chunk = dataProvider.getDatastore().find(DataChunkEntity.class)
                    .filter(Filters.eq("id", id))
                    .first();
            if (chunk != null) {
                chunks.add(chunk);
            }
        }
        return chunks;
    }

    @Override
    public byte[] getRawBytesFromChunks(List<ObjectId> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (ObjectId id : chunkIds) {
            DataChunkEntity chunk = dataProvider.getDatastore().find(DataChunkEntity.class)
                    .filter(Filters.eq("id", id))
                    .first();
            if (chunk != null && chunk.getData() != null) {
                sb.append(chunk.getData());
            }
        }
        if (sb.length() == 0) {
            return null;
        }
        return Base64.getDecoder().decode(sb.toString());
    }

    @Override
    public void saveDataChunk(DataChunkEntity chunk) {
        dataProvider.getDatastore().save(chunk);
    }

}

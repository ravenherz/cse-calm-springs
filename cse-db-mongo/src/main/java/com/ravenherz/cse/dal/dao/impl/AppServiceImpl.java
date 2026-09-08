package com.ravenherz.cse.dal.dao.impl;

import com.ravenherz.cse.dal.DataProvider;
import com.ravenherz.cse.dal.dao.AppService;
import com.ravenherz.cse.dal.dao.BasicService;
import com.ravenherz.cse.dal.dto.AppEntity;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.DataChunkEntity;
import com.ravenherz.cse.dal.dto.basic.AppData;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Base64;
import java.util.List;

@Repository(value = "appService")
public class AppServiceImpl extends BasicService implements AppService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppServiceImpl.class);

    public AppServiceImpl(DataProvider dataProvider) {
        super(dataProvider);
    }

    @Override
    public List<BasicEntity> getAll() {
        return new java.util.ArrayList<>(mongo().findAll(AppEntity.class));
    }

    @Override
    public AppEntity getBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return null;
        }
        return mongo().findOne(Query.query(Criteria.where("appData.slug").is(slug)), AppEntity.class);
    }

    @Override
    public void delete(AppEntity app) {
        if (app == null) {
            return;
        }
        deleteChunks(app);
        mongo().remove(app);
    }

    @Override
    public void deleteChunks(AppEntity app) {
        if (app == null || app.getAppData() == null || app.getAppData().getDataChunkIds() == null) {
            return;
        }
        for (ObjectId id : app.getAppData().getDataChunkIds()) {
            DataChunkEntity chunk = mongo().findById(id, DataChunkEntity.class);
            if (chunk != null) {
                mongo().remove(chunk);
                LOGGER.info("Deleted app data chunk: " + id);
            }
        }
    }

    @Override
    public void saveDataChunk(DataChunkEntity chunk) {
        mongo().save(chunk);
    }

    @Override
    public byte[] getRawBytesFromChunks(List<ObjectId> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (ObjectId id : chunkIds) {
            DataChunkEntity chunk = mongo().findById(id, DataChunkEntity.class);
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
    public byte[] loadZipBytes(AppEntity app) {
        if (app == null || app.getAppData() == null) {
            return null;
        }
        AppData data = app.getAppData();
        if (data.isLargeFile() && data.getDataChunkIds() != null) {
            return getRawBytesFromChunks(data.getDataChunkIds());
        }
        return app.getRawBytes();
    }
}

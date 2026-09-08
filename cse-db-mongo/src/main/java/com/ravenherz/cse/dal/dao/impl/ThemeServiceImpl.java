package com.ravenherz.cse.dal.dao.impl;

import com.ravenherz.cse.dal.DataProvider;
import com.ravenherz.cse.dal.dao.BasicService;
import com.ravenherz.cse.dal.dao.ThemeService;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.DataChunkEntity;
import com.ravenherz.cse.dal.dto.ThemeEntity;
import com.ravenherz.cse.dal.dto.basic.ThemeData;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Base64;
import java.util.List;

@Repository(value = "themeService")
public class ThemeServiceImpl extends BasicService implements ThemeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThemeServiceImpl.class);

    public ThemeServiceImpl(DataProvider dataProvider) {
        super(dataProvider);
    }

    @Override
    public List<BasicEntity> getAll() {
        return new java.util.ArrayList<>(mongo().findAll(ThemeEntity.class));
    }

    @Override
    public ThemeEntity getByThemeId(String themeId) {
        if (themeId == null || themeId.isBlank()) {
            return null;
        }
        return mongo().findOne(Query.query(Criteria.where("themeData.themeId").is(themeId)), ThemeEntity.class);
    }

    @Override
    public void delete(ThemeEntity theme) {
        if (theme == null) {
            return;
        }
        deleteChunks(theme);
        mongo().remove(theme);
    }

    @Override
    public void deleteChunks(ThemeEntity theme) {
        if (theme == null || theme.getThemeData() == null || theme.getThemeData().getDataChunkIds() == null) {
            return;
        }
        for (ObjectId id : theme.getThemeData().getDataChunkIds()) {
            DataChunkEntity chunk = mongo().findById(id, DataChunkEntity.class);
            if (chunk != null) {
                mongo().remove(chunk);
                LOGGER.info("Deleted theme data chunk: " + id);
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
    public byte[] loadZipBytes(ThemeEntity theme) {
        if (theme == null || theme.getThemeData() == null) {
            return null;
        }
        ThemeData data = theme.getThemeData();
        if (data.isLargeFile() && data.getDataChunkIds() != null) {
            return getRawBytesFromChunks(data.getDataChunkIds());
        }
        return theme.getRawBytes();
    }
}

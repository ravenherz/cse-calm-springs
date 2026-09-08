package com.ravenherz.cse.dal.dao;

import com.ravenherz.cse.dal.dto.ThemeEntity;
import com.ravenherz.cse.dal.dto.DataChunkEntity;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.stream.Collectors;

public interface ThemeService extends Service {

    ThemeEntity getByThemeId(String themeId);

    void delete(ThemeEntity theme);

    void deleteChunks(ThemeEntity theme);

    void saveDataChunk(DataChunkEntity chunk);

    byte[] getRawBytesFromChunks(List<ObjectId> chunkIds);

    byte[] loadZipBytes(ThemeEntity theme);

    default List<ThemeEntity> getAllThemes() {
        return getAll().stream()
                .filter(e -> e instanceof ThemeEntity)
                .map(e -> (ThemeEntity) e)
                .collect(Collectors.toList());
    }
}

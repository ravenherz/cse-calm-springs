package com.ravenherz.cse.dal.dto;

import com.ravenherz.cse.constants.MongoCollections;
import com.ravenherz.cse.dal.dto.basic.ThemeData;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Base64;

@Document(collection = MongoCollections.DATABASE_THEMES)
public final class ThemeEntity extends BasicEntity {

    private ThemeData themeData;

    public ThemeEntity() {
    }

    public ThemeEntity(ThemeData themeData, AccountEntity creator) {
        super(null, creator);
        this.themeData = themeData;
    }

    public ThemeData getThemeData() {
        return themeData;
    }

    public void setThemeData(ThemeData themeData) {
        this.themeData = themeData;
    }

    public byte[] getRawBytes() {
        if (themeData == null) {
            return null;
        }
        if (themeData.isLargeFile() && themeData.getDataChunkIds() != null
                && !themeData.getDataChunkIds().isEmpty()) {
            return null;
        }
        String contentRaw = themeData.getContentRaw();
        if (contentRaw == null || contentRaw.isEmpty()) {
            return null;
        }
        return Base64.getDecoder().decode(contentRaw);
    }
}

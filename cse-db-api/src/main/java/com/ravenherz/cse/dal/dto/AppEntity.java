package com.ravenherz.cse.dal.dto;

import com.ravenherz.cse.constants.MongoCollections;
import com.ravenherz.cse.dal.dto.basic.AppData;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Base64;

@Document(collection = MongoCollections.DATABASE_APPS)
public final class AppEntity extends BasicEntity {

    private AppData appData;

    public AppEntity() {
    }

    public AppEntity(AppData appData, AccountEntity creator) {
        super("0.1.0", null, creator);
        this.appData = appData;
    }

    public AppData getAppData() {
        return appData;
    }

    public void setAppData(AppData appData) {
        this.appData = appData;
    }

    public byte[] getRawBytes() {
        if (appData == null) {
            return null;
        }
        if (appData.isLargeFile() && appData.getDataChunkIds() != null && !appData.getDataChunkIds().isEmpty()) {
            return null;
        }
        String contentRaw = appData.getContentRaw();
        if (contentRaw == null || contentRaw.isEmpty()) {
            return null;
        }
        return Base64.getDecoder().decode(contentRaw);
    }
}

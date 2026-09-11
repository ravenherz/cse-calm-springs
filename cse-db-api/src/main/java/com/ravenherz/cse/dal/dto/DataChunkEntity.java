package com.ravenherz.cse.dal.dto;

import com.ravenherz.cse.constants.MongoCollections;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = MongoCollections.DATABASE_DATACHUNKS)
public final class DataChunkEntity extends BasicEntity {

    private String data;

    public DataChunkEntity() {
    }

    public DataChunkEntity(String data) {
        super(null, null);
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}

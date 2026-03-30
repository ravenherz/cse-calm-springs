package com.ravenherz.rhzwe.dal.dto;

import dev.morphia.annotations.Entity;

@Entity("rhz-we-datachunks")
public final class DataChunkEntity extends BasicEntity {

    private String data;

    public DataChunkEntity() {
    }

    public DataChunkEntity(String data) {
        super("0.1.0", null, null);
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}

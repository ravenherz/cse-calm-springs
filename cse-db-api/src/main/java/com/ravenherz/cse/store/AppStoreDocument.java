package com.ravenherz.cse.store;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AppStoreDocument {

    private String id;
    private String ownerId;
    private String createdAt;
    private String updatedAt;
    private Map<String, Object> data = new LinkedHashMap<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data == null ? new LinkedHashMap<>() : data;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", id);
        out.put("ownerId", ownerId);
        out.put("createdAt", createdAt);
        out.put("updatedAt", updatedAt);
        out.put("data", data == null ? Map.of() : data);
        return out;
    }
}

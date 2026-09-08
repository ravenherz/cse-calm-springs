package com.ravenherz.cse.dal.dto;

import com.ravenherz.cse.constants.MongoCollections;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

@Document(collection = MongoCollections.DATABASE_SETTINGS)
public class SettingContextEntity {

    @Id
    private ObjectId id;

    @Indexed(unique = true)
    private String context;

    private Map<String, String> values = new HashMap<>();

    public SettingContextEntity() {
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public Map<String, String> getValues() {
        return values;
    }

    public void setValues(Map<String, String> values) {
        this.values = values == null ? new HashMap<>() : values;
    }
}

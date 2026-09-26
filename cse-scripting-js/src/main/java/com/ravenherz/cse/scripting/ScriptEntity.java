package com.ravenherz.cse.scripting;

import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.dto.BasicEntity;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

@Document(collection = ScriptEntity.COLLECTION)
public final class ScriptEntity extends BasicEntity implements Serializable {

    public static final String COLLECTION = "cse-scripts";

    @Indexed(unique = true)
    private String scriptId;
    private String folder;
    private String source;

    public ScriptEntity() {
    }

    public ScriptEntity(String scriptId, String folder, String source, EntityId creatorId) {
        super(null, creatorId);
        this.scriptId = scriptId;
        this.folder = folder == null ? "" : folder;
        this.source = source == null ? "" : source;
    }

    public String getScriptId() {
        return scriptId;
    }

    public void setScriptId(String scriptId) {
        this.scriptId = scriptId;
    }

    public String getFolder() {
        return folder == null ? "" : folder;
    }

    public void setFolder(String folder) {
        this.folder = folder == null ? "" : folder;
    }

    public String getSource() {
        return source == null ? "" : source;
    }

    public void setSource(String source) {
        this.source = source == null ? "" : source;
    }
}

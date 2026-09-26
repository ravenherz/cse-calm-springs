package com.ravenherz.cse.scripting;

import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.dto.BasicEntity;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

@Document(collection = ScriptRunEntity.COLLECTION)
public final class ScriptRunEntity extends BasicEntity implements Serializable {

    public static final String COLLECTION = "cse-script-runs";

    private String scriptEntityId;
    private String scriptId;
    private ScriptRunStatus status;
    private String message;
    private long startedAt;

    public ScriptRunEntity() {
    }

    public EntityId getScriptEntityId() {
        return scriptEntityId == null || scriptEntityId.isBlank() ? null : EntityId.of(scriptEntityId);
    }

    public void setScriptEntityId(EntityId scriptEntityId) {
        this.scriptEntityId = scriptEntityId == null ? null : scriptEntityId.toHexString();
    }

    public String getScriptId() {
        return scriptId == null ? "" : scriptId;
    }

    public void setScriptId(String scriptId) {
        this.scriptId = scriptId;
    }

    public ScriptRunStatus getStatus() {
        return status == null ? ScriptRunStatus.QUEUED : status;
    }

    public void setStatus(ScriptRunStatus status) {
        this.status = status == null ? ScriptRunStatus.QUEUED : status;
    }

    public String getMessage() {
        return message == null ? "" : message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }
}

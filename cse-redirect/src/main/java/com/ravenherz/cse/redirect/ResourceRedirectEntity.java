package com.ravenherz.cse.redirect;

import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.dto.BasicEntity;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Objects;

@Document(collection = ResourceRedirectEntity.COLLECTION)
public final class ResourceRedirectEntity extends BasicEntity implements Serializable {

    public static final String COLLECTION = "cse-redirects";

    @Indexed(unique = true)
    private String fromPath;
    private String targetPath;
    private EntityId targetEntityId;
    private boolean enabled;
    private int status;
    private boolean preserveQuery;

    public ResourceRedirectEntity() {
    }

    public ResourceRedirectEntity(String fromPath, String targetPath, EntityId targetEntityId,
            boolean enabled, int status, boolean preserveQuery, EntityId creatorId) {
        super(null, creatorId);
        this.fromPath = fromPath;
        this.targetPath = targetPath;
        this.targetEntityId = targetEntityId;
        this.enabled = enabled;
        this.status = status;
        this.preserveQuery = preserveQuery;
    }

    public String getFromPath() {
        return fromPath;
    }

    public void setFromPath(String fromPath) {
        this.fromPath = fromPath;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public EntityId getTargetEntityId() {
        return targetEntityId;
    }

    public void setTargetEntityId(EntityId targetEntityId) {
        this.targetEntityId = targetEntityId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isPreserveQuery() {
        return preserveQuery;
    }

    public void setPreserveQuery(boolean preserveQuery) {
        this.preserveQuery = preserveQuery;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ResourceRedirectEntity that = (ResourceRedirectEntity) o;
        return enabled == that.enabled
                && status == that.status
                && preserveQuery == that.preserveQuery
                && Objects.equals(fromPath, that.fromPath)
                && Objects.equals(targetPath, that.targetPath)
                && Objects.equals(targetEntityId, that.targetEntityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), fromPath, targetPath, targetEntityId, enabled, status, preserveQuery);
    }
}

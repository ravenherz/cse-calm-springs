package com.ravenherz.cse.util.video;

import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.dto.BasicEntity;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

@Document(collection = VideoTranscodeEntity.COLLECTION)
public final class VideoTranscodeEntity extends BasicEntity implements Serializable {

    public static final String COLLECTION = "cse-video-transcodes";

    @Indexed
    private String resourceId;
    private VideoTranscodeStatus status;
    private int percent;
    private String error;
    private int rank;
    private long updatedAt;

    public VideoTranscodeEntity() {
    }

    public EntityId getResourceId() {
        return resourceId == null ? null : EntityId.of(resourceId);
    }

    public void setResourceId(EntityId resourceId) {
        this.resourceId = resourceId == null ? null : resourceId.toHexString();
    }

    public VideoTranscodeStatus getStatus() {
        return status == null ? VideoTranscodeStatus.QUEUED : status;
    }

    public void setStatus(VideoTranscodeStatus status) {
        this.status = status == null ? VideoTranscodeStatus.QUEUED : status;
        this.rank = this.status.rank();
    }

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = Math.max(0, Math.min(100, percent));
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}

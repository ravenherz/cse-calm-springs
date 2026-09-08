package com.ravenherz.cse.dal.dto.basic;

import com.ravenherz.cse.dal.dto.ResourceEntity;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Field;

public class PlaylistTrack {

    @Field("refResource")
    private ObjectId refResourceId;
    @Transient
    private ResourceEntity refResource;
    private String title;
    private String artist;

    public PlaylistTrack() {
    }

    public ObjectId getRefResourceId() {
        return refResourceId != null ? refResourceId : (refResource == null ? null : refResource.getId());
    }

    public ResourceEntity getRefResource() {
        return refResource;
    }

    public void setRefResource(ResourceEntity refResource) {
        this.refResource = refResource;
        this.refResourceId = refResource == null ? null : refResource.getId();
    }

    public void attachRefResource(ResourceEntity refResource) {
        this.refResource = refResource;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = blankToNull(title);
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = blankToNull(artist);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

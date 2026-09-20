package com.ravenherz.cse.dal.dto.basic;

import com.ravenherz.cse.dal.dto.ResourceEntity;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;

public class PlaylistData {

    private String title;
    private String description;
    @Field("refImage")
    private ObjectId refImageId;
    @Transient
    private ResourceEntity refImage;
    private List<PlaylistTrack> tracks = new ArrayList<>();

    public PlaylistData() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ObjectId getRefImageId() {
        return refImageId != null ? refImageId : (refImage == null ? null : refImage.getId());
    }

    public ResourceEntity getRefImage() {
        return refImage;
    }

    public void setRefImage(ResourceEntity refImage) {
        this.refImage = refImage;
        this.refImageId = refImage == null ? null : refImage.getId();
    }

    public void attachRefImage(ResourceEntity refImage) {
        this.refImage = refImage;
    }

    public List<PlaylistTrack> getTracks() {
        if (tracks == null) {
            tracks = new ArrayList<>();
        }
        return tracks;
    }

    public void setTracks(List<PlaylistTrack> tracks) {
        this.tracks = tracks == null ? new ArrayList<>() : tracks;
    }
}

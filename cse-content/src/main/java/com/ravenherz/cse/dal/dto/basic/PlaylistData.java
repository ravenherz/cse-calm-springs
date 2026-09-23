package com.ravenherz.cse.dal.dto.basic;

import com.ravenherz.cse.dal.EntityId;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;

public class PlaylistData {

    private String title;
    private String description;
    @Field("refImage")
    private EntityId refImageId;
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

    public EntityId getRefImageId() {
        return refImageId;
    }

    public void setRefImageId(EntityId refImageId) {
        this.refImageId = refImageId;
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

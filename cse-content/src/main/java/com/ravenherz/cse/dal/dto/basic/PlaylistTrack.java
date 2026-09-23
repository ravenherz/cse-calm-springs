package com.ravenherz.cse.dal.dto.basic;

import com.ravenherz.cse.dal.EntityId;
import org.springframework.data.mongodb.core.mapping.Field;

public class PlaylistTrack {

    @Field("refResource")
    private EntityId refResourceId;
    private String title;
    private String artist;

    public PlaylistTrack() {
    }

    public EntityId getRefResourceId() {
        return refResourceId;
    }

    public void setRefResourceId(EntityId refResourceId) {
        this.refResourceId = refResourceId;
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

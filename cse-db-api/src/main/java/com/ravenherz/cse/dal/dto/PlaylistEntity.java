package com.ravenherz.cse.dal.dto;

import com.ravenherz.cse.constants.MongoCollections;
import com.ravenherz.cse.dal.dto.basic.PlaylistData;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Objects;

@Document(collection = MongoCollections.DATABASE_PLAYLISTS)
public final class PlaylistEntity extends BasicEntity implements Serializable {

    @Indexed(unique = true)
    private String playlistId;
    private PlaylistData playlistData;

    public PlaylistEntity() {
    }

    public PlaylistEntity(String playlistId, PlaylistData playlistData, AccountEntity creator) {
        super("0.1.0", null, creator);
        this.playlistId = playlistId;
        this.playlistData = playlistData;
    }

    public String getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(String playlistId) {
        this.playlistId = playlistId;
    }

    public PlaylistData getPlaylistData() {
        return playlistData;
    }

    public void setPlaylistData(PlaylistData playlistData) {
        this.playlistData = playlistData;
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
        PlaylistEntity that = (PlaylistEntity) o;
        return Objects.equals(playlistId, that.playlistId)
                && Objects.equals(playlistData, that.playlistData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playlistId, playlistData);
    }
}

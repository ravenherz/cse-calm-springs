package com.ravenherz.cse.dal.dao;

import com.ravenherz.cse.dal.dto.PlaylistEntity;

import java.util.List;

public interface PlaylistService extends Service {

    PlaylistEntity getByPlaylistId(String playlistId);

    void delete(PlaylistEntity playlist);

    default List<PlaylistEntity> getAllPlaylists() {
        return getAll().stream()
                .filter(PlaylistEntity.class::isInstance)
                .map(PlaylistEntity.class::cast)
                .collect(java.util.stream.Collectors.toList());
    }
}

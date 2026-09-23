package com.ravenherz.cse.dal.dao.impl;

import com.ravenherz.cse.dal.DataProvider;
import com.ravenherz.cse.dal.dao.BasicService;
import com.ravenherz.cse.dal.dao.PlaylistService;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.PlaylistEntity;
import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.MongoIds;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository(value = "playlistService")
public class PlaylistServiceImpl extends BasicService implements PlaylistService {

    public PlaylistServiceImpl(DataProvider dataProvider) {
        super(dataProvider);
    }

    @Override
    public List<BasicEntity> getAll() {
        return new ArrayList<>(mongo().findAll(PlaylistEntity.class));
    }

    @Override
    public PlaylistEntity getByPlaylistId(String playlistId) {
        if (playlistId == null || playlistId.isBlank()) {
            return null;
        }
        return mongo().findOne(Query.query(Criteria.where("playlistId").is(playlistId)),
                PlaylistEntity.class);
    }

    @Override
    public List<PlaylistEntity> getAllByRefImage(EntityId imageId) {
        if (imageId == null) {
            return List.of();
        }
        return mongo().find(Query.query(Criteria.where("playlistData.refImage").is(MongoIds.toObjectId(imageId))),
                PlaylistEntity.class);
    }

    @Override
    public void delete(PlaylistEntity playlist) {
        if (playlist != null) {
            mongo().remove(playlist);
        }
    }
}

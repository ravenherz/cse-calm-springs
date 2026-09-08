package com.ravenherz.cse.dal;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.CategoryEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.PlaylistEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.ResourceGroupEntity;
import com.ravenherz.cse.dal.dto.basic.AlbumData;
import com.ravenherz.cse.dal.dto.basic.Event;
import com.ravenherz.cse.dal.dto.basic.HistoryData;
import com.ravenherz.cse.dal.dto.basic.PageData;
import com.ravenherz.cse.dal.dto.basic.PlaylistData;
import com.ravenherz.cse.dal.dto.basic.PlaylistTrack;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class ReferenceHydrator {

    private static final ThreadLocal<Boolean> LOADING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private final MongoTemplate mongo;

    public ReferenceHydrator(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public void hydrate(Object entity) {
        if (entity == null || LOADING.get()) {
            return;
        }
        LOADING.set(Boolean.TRUE);
        try {
            if (entity instanceof ItemEntity item) {
                item.attachRefCategory(load(item.getRefCategoryId(), CategoryEntity.class));
                hydratePage(item.getPageData());
                hydrateAlbum(item.getAlbumData());
                hydrateHistory(item.getHistoryData());
            } else if (entity instanceof ResourceEntity resource) {
                resource.attachRefResourceGroup(load(resource.refResourceGroupObjectId(),
                        ResourceGroupEntity.class));
                hydrateHistory(resource.getHistoryData());
            } else if (entity instanceof PlaylistEntity playlist) {
                hydratePlaylist(playlist.getPlaylistData());
                hydrateHistory(playlist.getHistoryData());
            } else if (entity instanceof ResourceGroupEntity group) {
                group.attachRefParentGroup(load(group.refParentGroupObjectId(),
                        ResourceGroupEntity.class));
                hydrateHistory(group.getHistoryData());
            } else if (entity instanceof BasicEntity basic) {
                hydrateHistory(basic.getHistoryData());
            }
        } finally {
            LOADING.set(Boolean.FALSE);
        }
    }

    private void hydratePage(PageData page) {
        if (page == null) {
            return;
        }
        page.attachRefImage(load(page.getRefImageId(), ResourceEntity.class));
        if (page.getComments() == null) {
            return;
        }
        for (PageData.Comment comment : page.getComments()) {
            if (comment != null) {
                comment.attachAuthor(load(comment.getAuthorId(), AccountEntity.class));
            }
        }
    }

    private void hydratePlaylist(PlaylistData playlist) {
        if (playlist == null || playlist.getTracks() == null) {
            return;
        }
        for (PlaylistTrack track : playlist.getTracks()) {
            if (track != null) {
                track.attachRefResource(load(track.getRefResourceId(), ResourceEntity.class));
            }
        }
    }

    private void hydrateAlbum(AlbumData album) {
        if (album == null) {
            return;
        }
        album.attachRefResourceGroup(load(album.getRefResourceGroupId(), ResourceGroupEntity.class));
    }

    private void hydrateHistory(HistoryData history) {
        if (history == null || history.getEvents() == null) {
            return;
        }
        for (Event event : history.getEvents()) {
            if (event != null) {
                event.attachOwner(load(event.getOwnerId(), AccountEntity.class));
            }
        }
    }

    private <T> T load(ObjectId id, Class<T> type) {
        if (id == null) {
            return null;
        }
        return mongo.findById(id, type);
    }
}

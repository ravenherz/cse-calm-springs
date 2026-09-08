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

public final class MongoRefs {

    private MongoRefs() {
    }

    public static ObjectId id(BasicEntity entity) {
        return entity == null ? null : entity.getId();
    }

    public static void sync(Object entity) {
        if (entity instanceof ItemEntity item) {
            item.setRefCategory(item.getRefCategory());
            syncPage(item.getPageData());
            syncAlbum(item.getAlbumData());
            syncHistory(item.getHistoryData());
        } else if (entity instanceof ResourceEntity resource) {
            resource.setRefResourceGroup(resource.getRefResourceGroup());
            syncHistory(resource.getHistoryData());
        } else if (entity instanceof PlaylistEntity playlist) {
            syncPlaylist(playlist.getPlaylistData());
            syncHistory(playlist.getHistoryData());
        } else if (entity instanceof ResourceGroupEntity group) {
            group.setRefParentGroup(group.getRefParentGroup());
            syncHistory(group.getHistoryData());
        } else if (entity instanceof BasicEntity basic) {
            syncHistory(basic.getHistoryData());
        }
    }

    private static void syncPage(PageData page) {
        if (page == null) {
            return;
        }
        page.setRefImage(page.getRefImage());
        if (page.getComments() == null) {
            return;
        }
        for (PageData.Comment comment : page.getComments()) {
            if (comment != null) {
                comment.setAuthor(comment.getAuthor());
            }
        }
    }

    private static void syncPlaylist(PlaylistData playlist) {
        if (playlist == null || playlist.getTracks() == null) {
            return;
        }
        for (PlaylistTrack track : playlist.getTracks()) {
            if (track != null) {
                track.setRefResource(track.getRefResource());
            }
        }
    }

    private static void syncAlbum(AlbumData album) {
        if (album == null) {
            return;
        }
        album.setRefResourceGroup(album.getRefResourceGroup());
    }

    private static void syncHistory(HistoryData history) {
        if (history == null || history.getEvents() == null) {
            return;
        }
        for (Event event : history.getEvents()) {
            if (event != null) {
                event.setOwner(event.getOwner());
            }
        }
    }
}

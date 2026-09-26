package com.ravenherz.cse.engine.scripting;

import com.ravenherz.cse.controller.ContentProtectedAndCacheController;
import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.dto.AppEntity;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.PlaylistEntity;
import com.ravenherz.cse.dal.dto.ThemeEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.ResourceGroupEntity;
import com.ravenherz.cse.dal.dto.basic.AlbumData;
import com.ravenherz.cse.dal.dto.basic.AppData;
import com.ravenherz.cse.dal.dto.basic.PageData;
import com.ravenherz.cse.dal.dto.basic.PlaylistData;
import com.ravenherz.cse.dal.dto.basic.PlaylistTrack;
import com.ravenherz.cse.dal.dto.basic.ResourceData;
import com.ravenherz.cse.dal.dto.basic.ResourceSizeHint;
import com.ravenherz.cse.dal.dto.basic.ThemeData;
import com.ravenherz.cse.present.ResourceGroupIndex;
import com.ravenherz.cse.util.video.VideoTranscodeEntity;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Removes stored rows that no longer point at a live parent.
 * A resource is an orphan when its group is missing. A track, image ref, album group,
 * or transcode row is an orphan when its resource is missing. A data chunk is an orphan
 * when no remaining resource, app, or theme lists its id.
 */
@Component
public class OrphanSweep {

    private final ServiceProvider services;
    private final ResourceGroupIndex catalog;
    private final ContentProtectedAndCacheController cache;

    public OrphanSweep(ServiceProvider services, ResourceGroupIndex catalog,
            ContentProtectedAndCacheController cache) {
        this.services = services;
        this.catalog = catalog;
        this.cache = cache;
    }

    public String deleteOrphans() {
        Set<String> groups = groupIds();
        List<ResourceSizeHint> hints = services.getResourceService().listSizeHints();
        if (hints == null) {
            hints = List.of();
        }
        Set<String> kept = new HashSet<>();
        List<ResourceSizeHint> orphans = new ArrayList<>();
        for (ResourceSizeHint hint : hints) {
            if (hint == null || hint.id() == null) {
                continue;
            }
            if (hint.groupId() != null && groups.contains(hint.groupId().toHexString())) {
                kept.add(hint.id().toHexString());
            } else {
                orphans.add(hint);
            }
        }
        int images = clearImages(kept);
        int tracks = clearTracks(kept);
        int albums = clearAlbumGroups(groups);
        int resources = deleteResources(orphans);
        int chunks = deleteOrphanChunks(kept);
        int transcodes = deleteTranscodes(kept);
        if (resources > 0) {
            catalog.structureChanged();
        }
        if (images > 0 || tracks > 0 || albums > 0) {
            catalog.contentChanged();
        }
        return "resources " + resources
                + ", tracks " + tracks
                + ", images " + images
                + ", album-groups " + albums
                + ", transcodes " + transcodes
                + ", chunks " + chunks;
    }

    private Set<String> groupIds() {
        Set<String> ids = new HashSet<>();
        List<ResourceGroupEntity> groups = services.getResourceGroupService().getAllGroups();
        if (groups == null) {
            return ids;
        }
        for (ResourceGroupEntity group : groups) {
            if (group != null && group.getId() != null) {
                ids.add(group.getId().toHexString());
            }
        }
        return ids;
    }

    private int clearImages(Set<String> kept) {
        int cleared = 0;
        List<BasicEntity> items = services.getItemService().getAll();
        if (items != null) {
            for (BasicEntity entity : items) {
                if (!(entity instanceof ItemEntity item) || item.getPageData() == null) {
                    continue;
                }
                PageData page = item.getPageData();
                if (page.getRefImageId() != null && !kept.contains(page.getRefImageId().toHexString())) {
                    page.setRefImageId(null);
                    services.getItemService().replace(item);
                    cleared++;
                }
            }
        }
        List<PlaylistEntity> playlists = services.getPlaylistService().getAllPlaylists();
        if (playlists != null) {
            for (PlaylistEntity playlist : playlists) {
                PlaylistData data = playlist.getPlaylistData();
                if (data == null || data.getRefImageId() == null) {
                    continue;
                }
                if (!kept.contains(data.getRefImageId().toHexString())) {
                    data.setRefImageId(null);
                    services.getPlaylistService().replace(playlist);
                    cleared++;
                }
            }
        }
        return cleared;
    }

    private int clearTracks(Set<String> kept) {
        int removed = 0;
        List<PlaylistEntity> playlists = services.getPlaylistService().getAllPlaylists();
        if (playlists == null) {
            return 0;
        }
        for (PlaylistEntity playlist : playlists) {
            PlaylistData data = playlist.getPlaylistData();
            if (data == null || data.getTracks() == null || data.getTracks().isEmpty()) {
                continue;
            }
            List<PlaylistTrack> keptTracks = new ArrayList<>();
            int before = data.getTracks().size();
            for (PlaylistTrack track : data.getTracks()) {
                if (track == null) {
                    continue;
                }
                EntityId resource = track.getRefResourceId();
                if (resource != null && kept.contains(resource.toHexString())) {
                    keptTracks.add(track);
                }
            }
            if (keptTracks.size() != before) {
                data.setTracks(keptTracks);
                services.getPlaylistService().replace(playlist);
                removed += before - keptTracks.size();
            }
        }
        return removed;
    }

    private int clearAlbumGroups(Set<String> groups) {
        int cleared = 0;
        List<BasicEntity> items = services.getItemService().getAll();
        if (items == null) {
            return 0;
        }
        for (BasicEntity entity : items) {
            if (!(entity instanceof ItemEntity item) || item.getAlbumData() == null) {
                continue;
            }
            AlbumData album = item.getAlbumData();
            EntityId group = album.getRefResourceGroupId();
            if (group != null && !groups.contains(group.toHexString())) {
                album.setRefResourceGroupId(null);
                services.getItemService().replace(item);
                cleared++;
            }
        }
        return cleared;
    }

    private int deleteResources(List<ResourceSizeHint> orphans) {
        int deleted = 0;
        for (ResourceSizeHint hint : orphans) {
            String path = hint.pathPublic();
            if (path != null && !path.isBlank()) {
                cache.invalidateCacheForResource(path);
                services.getResourceService().deleteByPublicPath(path);
            } else if (hint.id() != null) {
                services.getResourceService().delete(
                        services.getResourceService().getById(
                                com.ravenherz.cse.dal.dto.ResourceEntity.class,
                                com.ravenherz.cse.dal.StoredIds.entityId(hint.id())));
            }
            deleted++;
        }
        return deleted;
    }

    private int deleteOrphanChunks(Set<String> keptResources) {
        Set<String> referenced = new HashSet<>();
        List<BasicEntity> resources = services.getResourceService().getAll();
        if (resources != null) {
            for (BasicEntity entity : resources) {
                if (!(entity instanceof ResourceEntity resource) || resource.getId() == null) {
                    continue;
                }
                if (!keptResources.contains(resource.getId().toHexString())) {
                    continue;
                }
                addChunks(referenced, resource.getResourceData());
                addChunks(referenced, resource.getPreviewData());
            }
        }
        List<AppEntity> apps = services.getAppService().getAllApps();
        if (apps != null) {
            for (AppEntity app : apps) {
                AppData data = app == null ? null : app.getAppData();
                addChunkIds(referenced, data == null ? null : data.getDataChunkIds());
            }
        }
        List<ThemeEntity> themes = services.getThemeService().getAllThemes();
        if (themes != null) {
            for (ThemeEntity theme : themes) {
                ThemeData data = theme == null ? null : theme.getThemeData();
                addChunkIds(referenced, data == null ? null : data.getDataChunkIds());
            }
        }
        List<ObjectId> chunks = services.getResourceService().listDataChunkIds();
        if (chunks == null) {
            return 0;
        }
        int deleted = 0;
        for (ObjectId id : chunks) {
            if (id != null && !referenced.contains(id.toHexString())) {
                services.getResourceService().deleteDataChunk(id);
                deleted++;
            }
        }
        return deleted;
    }

    private static void addChunks(Set<String> referenced, ResourceData data) {
        addChunkIds(referenced, data == null ? null : data.getDataChunkIds());
    }

    private static void addChunkIds(Set<String> referenced, List<ObjectId> ids) {
        if (ids == null) {
            return;
        }
        for (ObjectId id : ids) {
            if (id != null) {
                referenced.add(id.toHexString());
            }
        }
    }

    private int deleteTranscodes(Set<String> kept) {
        int deleted = 0;
        List<BasicEntity> rows = services.getVideoTranscodeService().getAll();
        if (rows == null) {
            return 0;
        }
        for (BasicEntity entity : rows) {
            if (!(entity instanceof VideoTranscodeEntity row)) {
                continue;
            }
            EntityId resource = row.getResourceId();
            if (resource == null || !kept.contains(resource.toHexString())) {
                services.getVideoTranscodeService().delete(row);
                deleted++;
            }
        }
        return deleted;
    }
}

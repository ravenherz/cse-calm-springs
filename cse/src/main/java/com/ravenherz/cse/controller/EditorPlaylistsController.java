package com.ravenherz.cse.controller;

import com.ravenherz.cse.dal.StoredIds;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.PlaylistEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.basic.Event;
import com.ravenherz.cse.dal.dto.basic.HistoryData;
import com.ravenherz.cse.dal.dto.basic.PlaylistData;
import com.ravenherz.cse.dal.dto.basic.PlaylistTrack;
import com.ravenherz.cse.dal.dto.basic.enums.EventType;
import com.ravenherz.cse.dal.dto.basic.enums.ResourceType;
import com.ravenherz.cse.present.AudioResourceView;
import com.ravenherz.cse.present.EditorTree;
import com.ravenherz.cse.present.ResourceGroupIndex;
import com.ravenherz.cse.util.PlaylistIds;
import com.ravenherz.cse.util.PlaylistTracks;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/editor")
public class EditorPlaylistsController extends AbstractController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditorPlaylistsController.class);

    @Autowired
    private ResourceGroupIndex resourceGroupIndex;

    @GetMapping("/playlists")
    public String listPlaylists(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        response.sendRedirect(request.getContextPath() + EditorTree.PLAYLISTS_HREF);
        return null;
    }

    @GetMapping("/playlist/create")
    public String createPlaylistPage(Model model, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        addPlaylistForm(model, accessor, null, List.of());
        return "/admin/editor-playlist-create";
    }

    @PostMapping("/playlist/create")
    public String createPlaylistSubmit(@RequestParam(value = "playlistId", required = false) String playlistId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "resourceId", required = false) List<String> resourceIds,
            @RequestParam(value = "titleOverride", required = false) List<String> titleOverrides,
            @RequestParam(value = "artistOverride", required = false) List<String> artistOverrides,
            @RequestParam(value = "coverResourceId", required = false) String coverResourceId,
            Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        String normalizedId = PlaylistIds.normalize(playlistId);
        List<PlaylistTrack> tracks = buildTracks(resourceIds, titleOverrides, artistOverrides);
        ResourceEntity cover = loadImage(coverResourceId);
        PlaylistEntity draft = formPlaylist(normalizedId, title, description, tracks, cover);
        if (!PlaylistIds.isValid(normalizedId)) {
            model.addAttribute("error", "Playlist id must be lowercase letters, numbers, and hyphens.");
            addPlaylistForm(model, accessor, draft, tracks);
            return "/admin/editor-playlist-create";
        }
        if (title == null || title.isBlank()) {
            model.addAttribute("error", "Title is required");
            addPlaylistForm(model, accessor, draft, tracks);
            return "/admin/editor-playlist-create";
        }
        if (serviceProvider.getPlaylistService().getByPlaylistId(normalizedId) != null) {
            model.addAttribute("error", "A playlist with this id already exists");
            addPlaylistForm(model, accessor, draft, tracks);
            return "/admin/editor-playlist-create";
        }
        PlaylistData data = new PlaylistData();
        data.setTitle(title.trim());
        data.setDescription(blankToNull(description));
        data.setTracks(tracks);
        data.setRefImageId(cover == null ? null : cover.getId());
        PlaylistEntity playlist = new PlaylistEntity(normalizedId, data, accessor);
        applyAccess(request, playlist);
        try {
            serviceProvider.getPlaylistService().insert(playlist);
            resourceGroupIndex.contentChanged();
        } catch (Exception e) {
            LOGGER.error("Failed to create playlist: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to create playlist: " + e.getMessage());
            addPlaylistForm(model, accessor, playlist, tracks);
            return "/admin/editor-playlist-create";
        }
        response.sendRedirect(request.getContextPath() + EditorTree.PLAYLISTS_HREF);
        return null;
    }

    @GetMapping("/playlist/edit")
    public String editPlaylistPage(@RequestParam(value = "id", required = false) String id,
            Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        PlaylistEntity playlist = loadPlaylist(id);
        if (playlist == null) {
            error(404, request, response);
            return null;
        }
        addPlaylistForm(model, accessor, playlist, playlistTracks(playlist));
        return "/admin/editor-playlist-edit";
    }

    @PostMapping("/playlist/save")
    public String savePlaylist(@RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "playlistId", required = false) String playlistId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "resourceId", required = false) List<String> resourceIds,
            @RequestParam(value = "titleOverride", required = false) List<String> titleOverrides,
            @RequestParam(value = "artistOverride", required = false) List<String> artistOverrides,
            @RequestParam(value = "coverResourceId", required = false) String coverResourceId,
            Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        PlaylistEntity playlist = loadPlaylist(id);
        if (playlist == null) {
            error(404, request, response);
            return null;
        }
        String normalizedId = PlaylistIds.normalize(playlistId);
        List<PlaylistTrack> tracks = buildTracks(resourceIds, titleOverrides, artistOverrides);
        ResourceEntity cover = loadImage(coverResourceId);
        playlist.setPlaylistId(normalizedId);
        if (playlist.getPlaylistData() == null) {
            playlist.setPlaylistData(new PlaylistData());
        }
        playlist.getPlaylistData().setTitle(title);
        playlist.getPlaylistData().setDescription(description);
        playlist.getPlaylistData().setTracks(tracks);
        playlist.getPlaylistData().setRefImageId(cover == null ? null : cover.getId());
        if (!PlaylistIds.isValid(normalizedId)) {
            model.addAttribute("error", "Playlist id must be lowercase letters, numbers, and hyphens.");
            addPlaylistForm(model, accessor, playlist, tracks);
            return "/admin/editor-playlist-edit";
        }
        if (title == null || title.isBlank()) {
            model.addAttribute("error", "Title is required");
            addPlaylistForm(model, accessor, playlist, tracks);
            return "/admin/editor-playlist-edit";
        }
        PlaylistEntity clash = serviceProvider.getPlaylistService().getByPlaylistId(normalizedId);
        if (clash != null && playlist.getId() != null && !clash.getId().equals(playlist.getId())) {
            model.addAttribute("error", "A playlist with this id already exists");
            addPlaylistForm(model, accessor, playlist, tracks);
            return "/admin/editor-playlist-edit";
        }
        playlist.getPlaylistData().setTitle(title.trim());
        playlist.getPlaylistData().setDescription(blankToNull(description));
        HistoryData historyData = playlist.getHistoryData();
        if (historyData == null) {
            historyData = new HistoryData();
        }
        Event[] oldEvents = historyData.getEvents() == null ? new Event[0] : historyData.getEvents();
        Event[] newEvents = new Event[oldEvents.length + 1];
        System.arraycopy(oldEvents, 0, newEvents, 0, oldEvents.length);
        newEvents[oldEvents.length] = new Event(EventType.ENTITY_EDITED, LocalDateTime.now(), accessor == null ? null : accessor.getId());
        historyData.setEvents(newEvents);
        playlist.setHistoryData(historyData);
        applyAccess(request, playlist);
        serviceProvider.getPlaylistService().replace(playlist);
        resourceGroupIndex.contentChanged();
        response.sendRedirect(request.getContextPath() + EditorTree.PLAYLISTS_HREF);
        return null;
    }

    @PostMapping("/playlist/delete")
    public String deletePlaylist(@RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "returnGroup", required = false) String returnGroup,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        PlaylistEntity playlist = loadPlaylist(id);
        if (playlist != null) {
            try {
                serviceProvider.getPlaylistService().delete(playlist);
                resourceGroupIndex.contentChanged();
            } catch (Exception e) {
                LOGGER.error("Failed to delete playlist: {}", e.getMessage(), e);
            }
        }
        response.sendRedirect(request.getContextPath()
                + EditorTree.catalogReturnHref(returnGroup, EditorTree.PLAYLISTS_ID));
        return null;
    }

    private PlaylistEntity loadPlaylist(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        try {
            return (PlaylistEntity) serviceProvider.getPlaylistService()
                    .getById(PlaylistEntity.class, StoredIds.entityId(new ObjectId(id.trim())));
        } catch (Exception e) {
            return null;
        }
    }

    private void addPlaylistForm(Model model, AccountEntity accessor, PlaylistEntity playlist,
            List<PlaylistTrack> tracks) {
        model.addAttribute("playlist", playlist);
        model.addAttribute("coverImagePath", coverImagePath(playlist));
        model.addAttribute("selectedTracks", toViews(tracks));
        addEditorChrome(model, accessor);
        if (playlist != null && playlist.getId() != null) {
            EditorInline.putTreeForPlaylist(model, resourceGroupIndex, playlist);
        } else {
            EditorInline.putTreeForPlaylistCreate(model, resourceGroupIndex);
        }
        addAccessPanel(model, playlist, accessor);
    }

    private String coverImagePath(PlaylistEntity playlist) {
        if (playlist == null || playlist.getPlaylistData() == null
                || playlist.getPlaylistData().getRefImageId() == null) {
            return null;
        }
        return resourceGroupIndex.filePath(playlist.getPlaylistData().getRefImageId().toString());
    }

    private List<PlaylistTrack> playlistTracks(PlaylistEntity playlist) {
        if (playlist == null || playlist.getPlaylistData() == null) {
            return List.of();
        }
        return playlist.getPlaylistData().getTracks();
    }

    private List<AudioResourceView> toViews(List<PlaylistTrack> tracks) {
        List<AudioResourceView> views = new ArrayList<>();
        if (tracks == null) {
            return views;
        }
        int index = 0;
        for (PlaylistTrack track : tracks) {
            if (track == null) {
                continue;
            }
            ResourceEntity resource = null;
            if (track.getRefResourceId() != null) {
                try {
                    Object found = serviceProvider.getResourceService()
                            .getById(ResourceEntity.class, track.getRefResourceId());
                    if (found instanceof ResourceEntity loaded) {
                        resource = loaded;
                    }
                } catch (Exception e) {
                    LOGGER.warn("Could not load playlist track resource {}: {}",
                            track.getRefResourceId(), e.getMessage());
                }
            }
            if (resource == null) {
                continue;
            }
            views.add(toView(resource, track.getTitle(), track.getArtist(), index++));
        }
        return views;
    }

    private AudioResourceView toView(ResourceEntity resource, String titleOverride, String artistOverride,
            int index) {
        AudioResourceView view = new AudioResourceView();
        view.setId(resource.getId() == null ? "" : resource.getId().toString());
        view.setFileName(resource.getResourceData() == null ? "" : resource.getResourceData().getFileName());
        PlaylistTrack probe = new PlaylistTrack();
        probe.setTitle(titleOverride);
        probe.setArtist(artistOverride);
        view.setTitle(PlaylistTracks.title(probe, resource));
        view.setArtist(PlaylistTracks.artist(probe, resource));
        view.setDurationLabel(PlaylistTracks.durationLabel(probe, resource));
        view.setTrackNumber(PlaylistTracks.trackNumber(probe, resource, index));
        view.setTitleOverride(titleOverride);
        view.setArtistOverride(artistOverride);
        if (resource.getPreviewData() != null && resource.getPreviewData().getPathPublic() != null
                && !resource.getPreviewData().getPathPublic().isBlank()) {
            view.setPreviewPath(resource.getPreviewData().getPathPublic());
        }
        return view;
    }

    private ResourceEntity loadImage(String resourceId) {
        if (resourceId == null || resourceId.isBlank()) {
            return null;
        }
        try {
            ResourceEntity resource = (ResourceEntity) serviceProvider.getResourceService()
                    .getById(ResourceEntity.class, StoredIds.entityId(new ObjectId(resourceId.trim())));
            if (resource == null || resource.getResourceData() == null
                    || resource.getResourceData().getType() != ResourceType.IMAGE) {
                return null;
            }
            return resource;
        } catch (Exception e) {
            return null;
        }
    }

    private List<PlaylistTrack> buildTracks(List<String> resourceIds, List<String> titleOverrides,
            List<String> artistOverrides) {
        List<PlaylistTrack> tracks = new ArrayList<>();
        if (resourceIds == null) {
            return tracks;
        }
        for (int i = 0; i < resourceIds.size(); i++) {
            String resourceId = resourceIds.get(i);
            if (resourceId == null || resourceId.isBlank()) {
                continue;
            }
            ResourceEntity resource;
            try {
                resource = (ResourceEntity) serviceProvider.getResourceService()
                        .getById(ResourceEntity.class, StoredIds.entityId(new ObjectId(resourceId.trim())));
            } catch (Exception e) {
                continue;
            }
            if (resource == null || resource.getResourceData() == null
                    || resource.getResourceData().getType() != ResourceType.AUDIO) {
                continue;
            }
            PlaylistTrack track = new PlaylistTrack();
            track.setRefResourceId(resource.getId());
            track.setTitle(valueAt(titleOverrides, i));
            track.setArtist(valueAt(artistOverrides, i));
            tracks.add(track);
        }
        return tracks;
    }

    private PlaylistEntity formPlaylist(String playlistId, String title, String description,
            List<PlaylistTrack> tracks, ResourceEntity cover) {
        PlaylistData data = new PlaylistData();
        data.setTitle(title);
        data.setDescription(description);
        data.setTracks(tracks);
        data.setRefImageId(cover == null ? null : cover.getId());
        PlaylistEntity playlist = new PlaylistEntity();
        playlist.setPlaylistId(playlistId);
        playlist.setPlaylistData(data);
        return playlist;
    }

    private static String valueAt(List<String> values, int index) {
        if (values == null || index >= values.size()) {
            return null;
        }
        return values.get(index);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

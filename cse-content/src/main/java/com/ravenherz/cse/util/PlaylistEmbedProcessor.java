package com.ravenherz.cse.util;

import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.dao.PlaylistService;
import com.ravenherz.cse.dal.dao.ResourceService;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.PlaylistEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.basic.PlaylistData;
import com.ravenherz.cse.dal.dto.basic.PlaylistTrack;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
class PlaylistEmbedProcessor {

    public interface Lookup {
        PlaylistEntity findByPlaylistId(String playlistId);
    }

    private static final Pattern TAG = Pattern.compile(
            "(?is)<cse-playlist\\b([^>]*)(?:\\s*/>|>\\s*</cse-playlist>)");
    private static final Pattern ID_ATTR = Pattern.compile(
            "(?i)\\bid\\s*=\\s*[\"']([^\"']+)[\"']");
    private static final Pattern WITH_IMAGE_ATTR = Pattern.compile(
            "(?i)\\bwithImage\\s*=\\s*[\"']([^\"']*)[\"']");
    private static final Pattern WITH_IMAGE_BARE = Pattern.compile(
            "(?i)(?:^|\\s)withImage(?=\\s|/|>|$)");

    private static volatile PlaylistEmbedProcessor instance;

    interface Resources {
        ResourceEntity find(EntityId id);
    }

    private final Lookup lookup;
    private final Resources resources;

    @Autowired
    public PlaylistEmbedProcessor(ObjectProvider<PlaylistService> playlistService,
            ObjectProvider<ResourceService> resourceService) {
        PlaylistService service = playlistService == null ? null : playlistService.getIfAvailable();
        ResourceService files = resourceService == null ? null : resourceService.getIfAvailable();
        this.lookup = service == null ? id -> null : service::getByPlaylistId;
        this.resources = id -> load(files, id);
    }

    static PlaylistEmbedProcessor of(Lookup lookup) {
        return of(lookup, id -> null);
    }

    static PlaylistEmbedProcessor of(Lookup lookup, Resources resources) {
        return new PlaylistEmbedProcessor(lookup, resources);
    }

    private PlaylistEmbedProcessor(Lookup lookup, Resources resources) {
        this.lookup = lookup == null ? id -> null : lookup;
        this.resources = resources == null ? id -> null : resources;
    }

    private static ResourceEntity load(ResourceService files, EntityId id) {
        if (files == null || id == null) {
            return null;
        }
        BasicEntity found = files.getById(ResourceEntity.class, id);
        return found instanceof ResourceEntity resource ? resource : null;
    }

    @PostConstruct
    void register() {
        instance = this;
    }

    @PreDestroy
    void unregister() {
        if (instance == this) {
            instance = null;
        }
    }

    public static String expand(String html) {
        if (html == null || html.isEmpty()) {
            return html == null ? "" : html;
        }
        PlaylistEmbedProcessor processor = instance;
        if (processor == null) {
            return html;
        }
        return processor.expandHtml(html);
    }

    public String expandHtml(String html) {
        if (html == null || html.isEmpty()) {
            return html == null ? "" : html;
        }
        Matcher matcher = TAG.matcher(html);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String attrs = matcher.group(1) == null ? "" : matcher.group(1);
            Matcher idMatcher = ID_ATTR.matcher(attrs);
            String playlistId = idMatcher.find() ? PlaylistIds.normalize(idMatcher.group(1)) : "";
            matcher.appendReplacement(out, Matcher.quoteReplacement(render(playlistId, attrs)));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private String render(String playlistId, String attrs) {
        if (!PlaylistIds.isValid(playlistId)) {
            return missing(playlistId);
        }
        PlaylistEntity playlist = lookup.findByPlaylistId(playlistId);
        if (playlist == null) {
            return missing(playlistId);
        }
        PlaylistData data = playlist.getPlaylistData();
        String title = data == null || data.getTitle() == null || data.getTitle().isBlank()
                ? playlistId : data.getTitle();
        List<PlaylistTrack> tracks = data == null ? List.of() : data.getTracks();
        String cover = withImage(attrs) ? coverSrc(data) : "";
        boolean showCover = !cover.isBlank();
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"cse-playlist");
        if (showCover) {
            html.append(" cse-playlist-with-image");
        }
        html.append("\" data-playlist-id=\"")
                .append(escape(playlistId)).append("\">");
        if (showCover) {
            html.append("<div class=\"cse-playlist-cover\">");
            html.append("<img src=\"").append(escape(cover)).append("\" alt=\"")
                    .append(escape(title)).append("\">");
            html.append("</div>");
            html.append("<div class=\"cse-playlist-body\">");
        }
        html.append("<div class=\"cse-playlist-heading\">");
        html.append("<span class=\"cse-playlist-title\">").append(escape(title)).append("</span>");
        html.append("<button type=\"button\" class=\"cse-enqueue-all\"")
                .append(" aria-label=\"Add playlist to queue\" title=\"Add playlist to queue\"></button>");
        html.append("</div>");
        html.append("<ol class=\"cse-playlist-tracks\">");
        for (int i = 0; i < tracks.size(); i++) {
            PlaylistTrack track = tracks.get(i);
            if (track == null) {
                continue;
            }
            ResourceEntity resource = resources.find(track.getRefResourceId());
            String src = PlaylistTracks.src(track, resource);
            if (src.isBlank()) {
                continue;
            }
            String trackTitle = PlaylistTracks.title(track, resource);
            String artist = PlaylistTracks.artist(track, resource);
            String duration = PlaylistTracks.durationLabel(track, resource);
            String number = PlaylistTracks.trackNumber(track, resource, i);
            String waveform = PlaylistTracks.waveformBase64(track, resource);
            html.append("<li class=\"cse-track\"")
                    .append(" data-src=\"").append(escape(src)).append("\"")
                    .append(" data-title=\"").append(escape(trackTitle)).append("\"")
                    .append(" data-artist=\"").append(escape(artist)).append("\"")
                    .append(" data-duration=\"").append(escape(duration)).append("\">");
            html.append("<span class=\"cse-track-number\">").append(escape(number)).append("</span>");
            html.append("<span class=\"cse-track-info\">");
            if (!waveform.isBlank()) {
                html.append("<canvas class=\"cse-track-waveform\" aria-hidden=\"true\"")
                        .append(" data-waveform=\"").append(escape(waveform)).append("\"></canvas>");
            }
            html.append("<span class=\"cse-track-title\">").append(escape(trackTitle)).append("</span>");
            if (!artist.isBlank()) {
                html.append("<span class=\"cse-track-artist\">").append(escape(artist)).append("</span>");
            }
            html.append("</span>");
            if (!duration.isBlank()) {
                html.append("<span class=\"cse-track-duration\">").append(escape(duration)).append("</span>");
            }
            html.append("<button type=\"button\" class=\"cse-enqueue\"")
                    .append(" aria-label=\"Add to queue\" title=\"Add to queue\"></button>");
            html.append("</li>");
        }
        html.append("</ol>");
        if (showCover) {
            html.append("</div>");
        }
        html.append("</div>");
        return html.toString();
    }

    private static boolean withImage(String attrs) {
        if (attrs == null || attrs.isBlank()) {
            return false;
        }
        Matcher quoted = WITH_IMAGE_ATTR.matcher(attrs);
        if (quoted.find()) {
            String value = quoted.group(1) == null ? "" : quoted.group(1).trim().toLowerCase();
            return value.isEmpty() || value.equals("true") || value.equals("yes")
                    || value.equals("1") || value.equals("on");
        }
        return WITH_IMAGE_BARE.matcher(attrs).find();
    }

    private String coverSrc(PlaylistData data) {
        if (data == null) {
            return "";
        }
        String fromImage = imageSrc(resources.find(data.getRefImageId()));
        if (!fromImage.isBlank()) {
            return fromImage;
        }
        for (PlaylistTrack track : data.getTracks()) {
            if (track == null) {
                continue;
            }
            ResourceEntity resource = resources.find(track.getRefResourceId());
            if (resource == null || resource.getPreviewData() == null) {
                continue;
            }
            String src = CseEmbedProcessor.publicSrc(resource.getPreviewData().getPathPublic());
            if (src != null && !src.isBlank()) {
                return src;
            }
        }
        return "";
    }

    private static String imageSrc(ResourceEntity resource) {
        if (resource == null || resource.getResourceData() == null) {
            return "";
        }
        return CseEmbedProcessor.publicSrc(resource.getResourceData().getPathPublic());
    }

    private static String missing(String playlistId) {
        String id = playlistId == null ? "" : playlistId;
        return "<div class=\"cse-playlist cse-playlist-missing\" data-playlist-id=\""
                + escape(id) + "\">Playlist not found</div>";
    }

    static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

package com.ravenherz.cse.util;

import com.ravenherz.cse.dal.dao.PlaylistService;
import com.ravenherz.cse.dal.dto.PlaylistEntity;
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
public class PlaylistEmbedProcessor {

    public interface Lookup {
        PlaylistEntity findByPlaylistId(String playlistId);
    }

    private static final Pattern TAG = Pattern.compile(
            "(?is)<cse-playlist\\b([^>]*)(?:\\s*/>|>\\s*</cse-playlist>)");
    private static final Pattern ID_ATTR = Pattern.compile(
            "(?i)\\bid\\s*=\\s*[\"']([^\"']+)[\"']");

    private static volatile PlaylistEmbedProcessor instance;

    private final Lookup lookup;

    @Autowired
    public PlaylistEmbedProcessor(ObjectProvider<PlaylistService> playlistService) {
        PlaylistService service = playlistService == null ? null : playlistService.getIfAvailable();
        this.lookup = service == null ? id -> null : service::getByPlaylistId;
    }

    static PlaylistEmbedProcessor of(Lookup lookup) {
        return new PlaylistEmbedProcessor(lookup);
    }

    private PlaylistEmbedProcessor(Lookup lookup) {
        this.lookup = lookup == null ? id -> null : lookup;
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
            matcher.appendReplacement(out, Matcher.quoteReplacement(render(playlistId)));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private String render(String playlistId) {
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
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"cse-playlist\" data-playlist-id=\"")
                .append(escape(playlistId)).append("\">");
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
            String src = PlaylistTracks.src(track);
            if (src.isBlank()) {
                continue;
            }
            String trackTitle = PlaylistTracks.title(track);
            String artist = PlaylistTracks.artist(track);
            String duration = PlaylistTracks.durationLabel(track);
            String number = PlaylistTracks.trackNumber(track, i);
            String waveform = PlaylistTracks.waveformBase64(track);
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
        html.append("</ol></div>");
        return html.toString();
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

package com.ravenherz.cse.util;

import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.dto.PlaylistEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.basic.PlaylistData;
import com.ravenherz.cse.dal.dto.basic.PlaylistTrack;
import com.ravenherz.cse.dal.dto.basic.ResourceData;
import com.ravenherz.cse.dal.dto.basic.enums.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaylistEmbedProcessorTest {

    @Test
    void knownIdExpandsToMarkupWithTrackSrc() {
        PlaylistEmbedProcessor processor = processor(oceanBlue());
        String html = processor.expandHtml("<p>Intro</p><cse-playlist id=\"ocean-blue\"></cse-playlist><p>Outro</p>");
        assertTrue(html.contains("<p>Intro</p>"));
        assertTrue(html.contains("<p>Outro</p>"));
        assertTrue(html.contains("class=\"cse-playlist\""));
        assertTrue(html.contains("data-playlist-id=\"ocean-blue\""));
        assertTrue(html.contains("data-src=\"./content-protected/user/res/audio/tide.mp3\""));
        assertTrue(html.contains("data-title=\"Tide\""));
        assertTrue(html.contains("data-artist=\"Ravenherz\""));
        assertTrue(html.contains("class=\"cse-enqueue\""));
        assertTrue(html.contains("class=\"cse-enqueue-all\""));
        assertTrue(html.contains("cse-track-waveform"));
        assertTrue(html.contains("data-waveform="));
        int info = html.indexOf("cse-track-info");
        int waveform = html.indexOf("cse-track-waveform");
        int duration = html.indexOf("cse-track-duration");
        assertTrue(info >= 0 && waveform > info);
        assertTrue(duration > waveform);
        assertFalse(html.contains("Enqueue"));
        assertFalse(html.contains("<cse-playlist"));
    }

    @Test
    void withImageRendersCoverFromPlaylistImage() {
        PlaylistEmbedProcessor processor = processor(oceanBlueWithCover());
        String html = processor.expandHtml("<cse-playlist id=\"ocean-blue\" withImage=\"true\"></cse-playlist>");
        assertTrue(html.contains("cse-playlist-with-image"));
        assertTrue(html.contains("cse-playlist-cover"));
        assertTrue(html.contains("src=\"./content-protected/user/res/images/cover.jpg\""));
        assertTrue(html.contains("cse-playlist-body"));
    }

    @Test
    void withImageDefaultsToFalse() {
        PlaylistEmbedProcessor processor = processor(oceanBlueWithCover());
        String html = processor.expandHtml("<cse-playlist id=\"ocean-blue\"></cse-playlist>");
        assertFalse(html.contains("cse-playlist-with-image"));
        assertFalse(html.contains("cse-playlist-cover"));
    }

    @Test
    void withImageBareAttributeIsTrue() {
        PlaylistEmbedProcessor processor = processor(oceanBlueWithCover());
        String html = processor.expandHtml("<cse-playlist id=\"ocean-blue\" withImage></cse-playlist>");
        assertTrue(html.contains("cse-playlist-with-image"));
    }

    @Test
    void withImageFallsBackToTrackPreviewWhenPlaylistHasNoCover() {
        PlaylistEntity playlist = oceanBlue();
        ResourceData preview = new ResourceData();
        preview.setPathPublic("/user/res/images/tide-cover.jpg");
        playlist.getPlaylistData().getTracks().get(0);
        preview.setPathPublic("/user/res/images/tide-cover.jpg");
        lastTide.setPreviewData(preview);
        PlaylistEmbedProcessor processor = processor(playlist);
        String html = processor.expandHtml("<cse-playlist id=\"ocean-blue\" withImage=\"true\"></cse-playlist>");
        assertTrue(html.contains("cse-playlist-with-image"));
        assertTrue(html.contains("src=\"./content-protected/user/res/images/tide-cover.jpg\""));
    }

    @Test
    void withImageFalseOmitsCover() {
        PlaylistEmbedProcessor processor = processor(oceanBlueWithCover());
        String html = processor.expandHtml("<cse-playlist id=\"ocean-blue\" withImage=\"false\"></cse-playlist>");
        assertFalse(html.contains("cse-playlist-with-image"));
        assertFalse(html.contains("cse-playlist-cover"));
    }

    @Test
    void selfClosingTagExpands() {
        PlaylistEmbedProcessor processor = processor(oceanBlue());
        String html = processor.expandHtml("<cse-playlist id=\"ocean-blue\" />");
        assertTrue(html.contains("class=\"cse-playlist\""));
        assertTrue(html.contains("data-src=\"./content-protected/user/res/audio/tide.mp3\""));
    }

    @Test
    void unknownIdShowsFallbackAndKeepsSurroundingHtml() {
        PlaylistEmbedProcessor processor = PlaylistEmbedProcessor.of(id -> null);
        String html = processor.expandHtml("before <cse-playlist id=\"missing\"></cse-playlist> after");
        assertTrue(html.startsWith("before "));
        assertTrue(html.endsWith(" after"));
        assertTrue(html.contains("Playlist not found"));
        assertTrue(html.contains("cse-playlist-missing"));
        assertFalse(html.contains("data-src="));
    }

    @Test
    void unknownTagsAreLeftUntouched() {
        PlaylistEmbedProcessor processor = processor(oceanBlue());
        String html = processor.expandHtml("<cse-other id=\"ocean-blue\"></cse-other>");
        assertTrue(html.contains("<cse-other id=\"ocean-blue\"></cse-other>"));
    }

    private static final EntityId TIDE = EntityId.of("68b000000000000000000011");
    private static final EntityId COVER = EntityId.of("68b000000000000000000022");
    private static ResourceEntity lastTide;
    private static ResourceEntity lastCover;

    private static PlaylistEmbedProcessor processor(PlaylistEntity playlist) {
        return PlaylistEmbedProcessor.of(id -> playlist, PlaylistEmbedProcessorTest::resource);
    }

    private static ResourceEntity resource(EntityId id) {
        if (TIDE.equals(id)) {
            return lastTide;
        }
        if (COVER.equals(id)) {
            return lastCover;
        }
        return null;
    }

    private static PlaylistEntity oceanBlueWithCover() {
        PlaylistEntity playlist = oceanBlue();
        ResourceData imageData = new ResourceData();
        imageData.setType(ResourceType.IMAGE);
        imageData.setPathPublic("/user/res/images/cover.jpg");
        ResourceEntity cover = new ResourceEntity();
        cover.setId(COVER);
        cover.setResourceData(imageData);
        lastCover = cover;
        playlist.getPlaylistData().setRefImageId(COVER);
        return playlist;
    }

    private static PlaylistEntity oceanBlue() {
        ResourceData data = new ResourceData();
        data.setPathPublic("/user/res/audio/tide.mp3");
        data.addMetadata("title", "Tide");
        data.addMetadata("artist", "Ravenherz");
        data.addMetadata("duration", "185");
        data.addMetadata("trackNumber", "3");
        data.setWaveform(new byte[] {0, 64, -128});
        ResourceEntity resource = new ResourceEntity();
        resource.setId(TIDE);
        resource.setResourceData(data);
        lastTide = resource;
        lastCover = null;
        PlaylistTrack track = new PlaylistTrack();
        track.setRefResourceId(TIDE);
        PlaylistData playlistData = new PlaylistData();
        playlistData.setTitle("Ocean Blue");
        playlistData.setTracks(List.of(track));
        PlaylistEntity playlist = new PlaylistEntity();
        playlist.setPlaylistId("ocean-blue");
        playlist.setPlaylistData(playlistData);
        return playlist;
    }
}

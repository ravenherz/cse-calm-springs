package com.ravenherz.cse.dal.dto.basic.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceTypeTest {

    @Test
    void mapsKnownExtensions() {
        assertEquals(ResourceType.IMAGE, ResourceType.getByFileName("cover.jpg"));
        assertEquals(ResourceType.AUDIO, ResourceType.getByFileName("/u/res/audio/tide.mp3"));
        assertEquals(ResourceType.VIDEO, ResourceType.getByFileName("clip.mp4"));
        assertEquals(ResourceType.VIDEO, ResourceType.getByFileName("tape.MOV"));
        assertEquals(ResourceType.VIDEO, ResourceType.getByFileName("a/b/c.webm"));
        assertEquals(ResourceType.VIDEO, ResourceType.getByFileName("film.mkv"));
        assertEquals(ResourceType.VIDEO, ResourceType.getByFileName("phone.m4v"));
        assertEquals(ResourceType.INVALID, ResourceType.getByFileName("notes.txt"));
        assertEquals("video", ResourceType.VIDEO.getPath());
    }
}

package com.ravenherz.cse.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaylistIdsTest {

    @Test
    void normalizeTrimsAndLowercases() {
        assertEquals("ocean-blue", PlaylistIds.normalize("  Ocean-Blue  "));
        assertEquals("", PlaylistIds.normalize(null));
        assertEquals("", PlaylistIds.normalize("   "));
    }

    @Test
    void validIdsMatchSlugRule() {
        assertTrue(PlaylistIds.isValid("ocean-blue"));
        assertTrue(PlaylistIds.isValid("a"));
        assertTrue(PlaylistIds.isValid("track-3"));
        assertFalse(PlaylistIds.isValid("Ocean-Blue"));
        assertFalse(PlaylistIds.isValid("-leading"));
        assertFalse(PlaylistIds.isValid("has_underscore"));
        assertFalse(PlaylistIds.isValid(""));
        assertFalse(PlaylistIds.isValid(null));
    }

    @Test
    void uniquenessIsCaseInsensitiveAfterNormalize() {
        assertEquals(PlaylistIds.normalize("Ocean-Blue"), PlaylistIds.normalize("ocean-blue"));
        assertTrue(PlaylistIds.isValid(PlaylistIds.normalize("Ocean-Blue")));
    }
}

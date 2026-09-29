package com.ravenherz.cse.util;

import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.basic.ResourceData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaylistTracksTest {

    @Test
    void trackNumberPadsBelowTen() {
        assertEquals("01", PlaylistTracks.trackNumber(null, null, 0));
        assertEquals("03", PlaylistTracks.trackNumber(null, resource("3"), 0));
        assertEquals("10", PlaylistTracks.trackNumber(null, resource("10"), 9));
        assertEquals("09", PlaylistTracks.trackNumber(null, null, 8));
    }

    private static ResourceEntity resource(String trackNumber) {
        ResourceData data = new ResourceData();
        data.addMetadata("trackNumber", trackNumber);
        ResourceEntity resource = new ResourceEntity();
        resource.setResourceData(data);
        return resource;
    }
}

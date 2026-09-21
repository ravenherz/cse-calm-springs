package com.ravenherz.cse.util;

import com.ravenherz.cse.dal.dto.basic.ResourceData;
import org.jaudiotagger.tag.FieldKey;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class Mp3MetadataTest {

    @Test
    void parseTrackNumberTakesFirstInteger() {
        assertEquals("3", Mp3Metadata.parseTrackNumber("3/12"));
        assertEquals("7", Mp3Metadata.parseTrackNumber("07"));
        assertNull(Mp3Metadata.parseTrackNumber(""));
        assertNull(Mp3Metadata.parseTrackNumber(null));
        assertNull(Mp3Metadata.parseTrackNumber("none"));
    }

    @Test
    void metadataKeyMapsTrackAndCamelCase() {
        assertEquals("trackNumber", Mp3Metadata.metadataKey(FieldKey.TRACK));
        assertEquals("title", Mp3Metadata.metadataKey(FieldKey.TITLE));
        assertEquals("albumArtist", Mp3Metadata.metadataKey(FieldKey.ALBUM_ARTIST));
        assertEquals("discNo", Mp3Metadata.metadataKey(FieldKey.DISC_NO));
    }

    @Test
    void albumLineJoinsPresentParts() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("album", "Ocean Blue");
        fields.put("year", "2024");
        fields.put("trackNumber", "1");
        assertEquals("Ocean Blue 2024 #1", Mp3Metadata.albumLine(fields));
        fields.remove("trackNumber");
        assertEquals("Ocean Blue 2024", Mp3Metadata.albumLine(fields));
        fields.put("trackNumber", "1");
        fields.remove("year");
        assertEquals("Ocean Blue #1", Mp3Metadata.albumLine(fields));
        fields.remove("album");
        assertEquals("#1", Mp3Metadata.albumLine(fields));
        assertNull(Mp3Metadata.albumLine(Map.of()));
        assertNull(Mp3Metadata.albumLine(null));
    }

    @Test
    void applyToWritesAllTagFieldsAndHeaderBitrate() {
        ResourceData data = new ResourceData();
        data.addMetadata("bitrate", "540000");
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("title", "Tide");
        fields.put("artist", "Ravenherz");
        fields.put("album", "Ocean Blue");
        fields.put("trackNumber", "3");
        fields.put("genre", "Electronic");
        fields.put("year", "2024");
        Mp3Metadata.applyTo(data, new Mp3Metadata.Parsed(fields, 185, 192, null));
        assertEquals("Tide", data.getMetadata().get("title"));
        assertEquals("Ravenherz", data.getMetadata().get("artist"));
        assertEquals("Ocean Blue", data.getMetadata().get("album"));
        assertEquals("3", data.getMetadata().get("trackNumber"));
        assertEquals("Electronic", data.getMetadata().get("genre"));
        assertEquals("2024", data.getMetadata().get("year"));
        assertEquals("185", data.getMetadata().get("duration"));
        assertEquals("192", data.getMetadata().get("bitrate"));
    }
}

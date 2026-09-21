package com.ravenherz.cse.engine.video;

import com.ravenherz.cse.dal.dto.basic.ResourceData;
import com.ravenherz.cse.dal.dto.basic.enums.ResourceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VideoStatusTest {

    @Test
    void readyWhenStatusMissingOnVideo() {
        ResourceData data = new ResourceData();
        data.setType(ResourceType.VIDEO);
        assertTrue(VideoStatus.ready(data));
        assertFalse(VideoStatus.processing(data));
        VideoStatus.set(data, VideoStatus.PROCESSING);
        assertTrue(VideoStatus.processing(data));
        assertFalse(VideoStatus.ready(data));
        VideoStatus.fail(data, "boom");
        assertTrue(VideoStatus.failed(data));
        assertEquals("boom", data.getMetadata().get(VideoStatus.ERROR_KEY));
        VideoStatus.set(data, VideoStatus.READY);
        assertTrue(VideoStatus.ready(data));
        assertFalse(data.getMetadata().containsKey(VideoStatus.ERROR_KEY));
    }

    @Test
    void rememberSourceSizeKeepsOriginalBytes() {
        ResourceData data = new ResourceData();
        data.setSizeInBytes(2048);
        VideoStatus.rememberSourceSize(data);
        assertEquals("2048", data.getMetadata().get(VideoStatus.SOURCE_SIZE_KEY));
        assertEquals(2048L, VideoStatus.sourceSizeBytes(data));
        data.setSizeInBytes(99);
        VideoStatus.rememberSourceSize(data);
        assertEquals("2048", data.getMetadata().get(VideoStatus.SOURCE_SIZE_KEY));
        assertEquals("2048 bytes", VideoStatus.sizeLabel(2048L));
    }
}

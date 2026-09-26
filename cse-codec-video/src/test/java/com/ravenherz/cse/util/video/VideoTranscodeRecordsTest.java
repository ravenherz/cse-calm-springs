package com.ravenherz.cse.util.video;

import com.ravenherz.cse.dal.EntityId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VideoTranscodeRecordsTest {

    @Test
    void applyKeepsTheQueueStatuses() {
        EntityId resourceId = EntityId.generate();

        VideoTranscodeEntity queued = VideoTranscodeRecords.apply(null, resourceId,
                VideoTranscodeStatus.QUEUED, 0, null);
        assertEquals(VideoTranscodeStatus.QUEUED, queued.getStatus());
        assertEquals(1, queued.getRank());
        assertEquals(resourceId, queued.getResourceId());

        VideoTranscodeEntity running = VideoTranscodeRecords.apply(queued, resourceId,
                VideoTranscodeStatus.IN_PROGRESS, 40, null);
        assertEquals(VideoTranscodeStatus.IN_PROGRESS, running.getStatus());
        assertEquals(0, running.getRank());
        assertEquals(40, running.getPercent());

        VideoTranscodeEntity done = VideoTranscodeRecords.apply(running, resourceId,
                VideoTranscodeStatus.DONE, 100, " ");
        assertEquals(VideoTranscodeStatus.DONE, done.getStatus());
        assertEquals(3, done.getRank());
        assertNull(done.getError());
        assertEquals("done", done.getStatus().token());
        assertEquals("In progress", VideoTranscodeStatus.IN_PROGRESS.label());
    }

    @Test
    void pageSizeDefaultsToTwenty() {
        assertEquals(20, VideoTranscodePageSize.normalize(0));
        assertEquals(20, VideoTranscodePageSize.normalize("15"));
        assertEquals(20, VideoTranscodePageSize.normalize((String) null));
        assertEquals(50, VideoTranscodePageSize.normalize("50"));
        assertEquals(100, VideoTranscodePageSize.normalize(100));
        assertEquals(1, VideoTranscodePageSize.page("0"));
        assertEquals(3, VideoTranscodePageSize.page("3"));
    }
}

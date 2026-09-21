package com.ravenherz.cse.engine.video;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VideoProgressTest {

    @Test
    void queuedThenPercentThenReady() {
        VideoProgress progress = new VideoProgress();
        ObjectId id = new ObjectId();
        progress.queued(id);
        assertEquals(VideoProgress.QUEUED, progress.view(id).status());
        assertEquals(0, progress.view(id).percent());
        progress.start(id);
        progress.percent(id, 40);
        progress.percent(id, 12);
        assertEquals(VideoProgress.PROCESSING, progress.view(id).status());
        assertEquals(40, progress.view(id).percent());
        progress.ready(id, "/u/res/video/clip.poster.low-res.jpg", "1.2 MB");
        VideoProgress.View view = progress.view(id);
        assertEquals(VideoProgress.READY, view.status());
        assertEquals(100, view.percent());
        assertEquals("/u/res/video/clip.poster.low-res.jpg", view.preview());
        assertEquals("1.2 MB", view.sizeLabel());
    }

    @Test
    void viewsOrdersProcessingThenQueued() {
        VideoProgress progress = new VideoProgress();
        ObjectId ready = new ObjectId();
        ObjectId queued = new ObjectId();
        ObjectId processing = new ObjectId();
        progress.ready(ready, null, "1 MB");
        progress.queued(queued);
        progress.start(processing);
        progress.percent(processing, 40);
        List<VideoProgress.View> views = progress.views();
        assertEquals(3, views.size());
        assertEquals(VideoProgress.PROCESSING, views.get(0).status());
        assertEquals(VideoProgress.QUEUED, views.get(1).status());
        assertEquals(VideoProgress.READY, views.get(2).status());
    }

    @Test
    void failedKeepsError() {
        VideoProgress progress = new VideoProgress();
        ObjectId id = new ObjectId();
        progress.failed(id, "no encoder");
        assertEquals(VideoProgress.FAILED, progress.view(id).status());
        assertEquals("no encoder", progress.view(id).error());
        assertNull(progress.view(new ObjectId()));
    }
}

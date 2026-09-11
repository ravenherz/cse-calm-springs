package com.ravenherz.cse.util.video;

import com.ravenherz.cse.dal.dao.ResourceService;
import com.ravenherz.cse.install.SiteReady;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.concurrent.ExecutorService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VideoTranscodeQueueTest {

    @Test
    void resumeSkipsMongoWhenSiteIsNotConfigured() {
        ResourceService resources = mock(ResourceService.class);
        VideoTranscodeJob job = mock(VideoTranscodeJob.class);
        SiteReady siteReady = mock(SiteReady.class);
        when(siteReady.isConfigured()).thenReturn(false);
        VideoTranscodeQueue queue = new VideoTranscodeQueue(resources, job, mock(ExecutorService.class),
                of(siteReady));
        queue.resume();
        verify(resources, never()).listProcessingVideoIds();
    }

    @Test
    void resumeLoadsProcessingIdsWhenConfigured() {
        ResourceService resources = mock(ResourceService.class);
        VideoTranscodeJob job = mock(VideoTranscodeJob.class);
        SiteReady siteReady = mock(SiteReady.class);
        when(siteReady.isConfigured()).thenReturn(true);
        when(resources.listProcessingVideoIds()).thenReturn(java.util.List.of());
        VideoTranscodeQueue queue = new VideoTranscodeQueue(resources, job, mock(ExecutorService.class),
                of(siteReady));
        queue.resume();
        verify(resources).listProcessingVideoIds();
        verify(job, never()).run(org.mockito.ArgumentMatchers.any(ObjectId.class));
    }

    private static ObjectProvider<SiteReady> of(SiteReady value) {
        return new ObjectProvider<>() {
            @Override
            public SiteReady getObject() {
                return value;
            }

            @Override
            public SiteReady getIfAvailable() {
                return value;
            }

            @Override
            public SiteReady getIfUnique() {
                return value;
            }
        };
    }
}

package com.ravenherz.cse.controller;

import com.ravenherz.cse.dal.dao.ResourceService;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.basic.ResourceData;
import com.ravenherz.cse.dal.dto.basic.enums.ResourceType;
import com.ravenherz.cse.util.video.VideoProgress;
import com.ravenherz.cse.util.video.VideoStatus;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EditorVideoControllerTest {

    @Test
    void livePercentWinsOverStore() {
        ObjectId id = new ObjectId();
        VideoProgress progress = new VideoProgress();
        progress.start(id);
        progress.percent(id, 42);
        ResourceService resources = mock(ResourceService.class);
        EditorVideoController controller = new EditorVideoController(progress, of(resources));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/rhz-we");
        Map<String, Object> body = controller.progress(id.toHexString(), request).getBody();
        @SuppressWarnings("unchecked")
        List<VideoProgress.View> videos = (List<VideoProgress.View>) body.get("videos");
        assertEquals(1, videos.size());
        assertEquals(42, videos.get(0).percent());
        assertEquals(VideoProgress.PROCESSING, videos.get(0).status());
        assertNull(videos.get(0).preview());
    }

    @Test
    void storeFallbackAndPreviewHref() {
        ObjectId id = new ObjectId();
        ResourceData data = new ResourceData();
        data.setType(ResourceType.VIDEO);
        data.setPathPublic("/u/res/video/clip.mp4");
        data.setSizeInBytes(2048);
        VideoStatus.set(data, VideoStatus.READY);
        ResourceData preview = new ResourceData();
        preview.setPathPublic("/u/res/video/clip.poster.low-res.jpg");
        ResourceEntity entity = new ResourceEntity();
        entity.setId(id);
        entity.setResourceData(data);
        entity.setPreviewData(preview);
        ResourceService resources = mock(ResourceService.class);
        when(resources.getById(ResourceEntity.class, id)).thenReturn(entity);
        EditorVideoController controller = new EditorVideoController(new VideoProgress(), of(resources));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/rhz-we");
        Map<String, Object> body = controller.progress(id.toHexString(), request).getBody();
        @SuppressWarnings("unchecked")
        List<VideoProgress.View> videos = (List<VideoProgress.View>) body.get("videos");
        assertEquals(VideoProgress.READY, videos.get(0).status());
        assertEquals(100, videos.get(0).percent());
        assertEquals("/rhz-we/content-protected/u/res/video/clip.poster.low-res.jpg", videos.get(0).preview());
    }

    @Test
    void hrefJoinsContextAndProtectedPath() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/rhz-we");
        assertEquals("/rhz-we/content-protected/u/res/video/a.jpg",
                EditorVideoController.href(request, "/u/res/video/a.jpg"));
        assertNull(EditorVideoController.href(request, " "));
    }

    private static ObjectProvider<ResourceService> of(ResourceService value) {
        return new ObjectProvider<>() {
            @Override
            public ResourceService getObject() {
                return value;
            }

            @Override
            public ResourceService getIfAvailable() {
                return value;
            }

            @Override
            public ResourceService getIfUnique() {
                return value;
            }
        };
    }
}

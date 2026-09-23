package com.ravenherz.cse.engine.video;

import com.ravenherz.cse.util.video.VideoProbe;
import com.ravenherz.cse.util.video.VideoTranscoder;

import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.StoredIds;
import com.ravenherz.cse.dal.dao.ResourceService;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.basic.ResourceData;
import com.ravenherz.cse.dal.dto.basic.enums.ResourceType;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoTranscodeJobTest {

    @Mock
    ResourceService resources;

    @Test
    void encodePercentTracksFfmpegInsteadOfJumpingToNinetyFour() {
        assertEquals(8, VideoTranscodeJob.encodePercent(0));
        assertEquals(25, VideoTranscodeJob.encodePercent(20));
        assertEquals(52, VideoTranscodeJob.encodePercent(50));
        assertEquals(95, VideoTranscodeJob.encodePercent(100));
    }

    @Test
    void missingResourceIsNoOp() {
        ObjectId id = new ObjectId();
        when(resources.getById(ResourceEntity.class, StoredIds.entityId(id))).thenReturn(null);
        VideoTranscodeJob.of(resources, fakeTranscoder(), null, null, null, this::info).run(id);
        verify(resources, never()).replace(any());
    }

    @Test
    void successMarksReadyAndReplacesBytes() throws Exception {
        ObjectId id = new ObjectId();
        ResourceEntity entity = processingVideo(id);
        when(resources.getById(ResourceEntity.class, StoredIds.entityId(id))).thenReturn(entity);
        doAnswer(invocation -> {
            Path dest = invocation.getArgument(1);
            Files.writeString(dest, "src");
            return null;
        }).when(resources).writeToFile(any(), any());
        VideoTranscodeJob.of(resources, fakeTranscoder(), null, null, null, this::info).run(id);
        ArgumentCaptor<ResourceEntity> saved = ArgumentCaptor.forClass(ResourceEntity.class);
        verify(resources).replace(saved.capture());
        ResourceData data = saved.getValue().getResourceData();
        assertTrue(VideoStatus.ready(data));
        assertEquals("640", data.getMetadata().get("width"));
        assertEquals("360", data.getMetadata().get("height"));
        assertEquals("4096", data.getMetadata().get(VideoStatus.SOURCE_SIZE_KEY));
        verify(resources).deleteStoredContent(any());
        verify(resources).fillFromFile(any(), any());
    }

    @Test
    void failureKeepsOriginalAndMarksFailed() {
        ObjectId id = new ObjectId();
        ResourceEntity entity = processingVideo(id);
        when(resources.getById(ResourceEntity.class, StoredIds.entityId(id))).thenReturn(entity);
        VideoTranscoder boom = new VideoTranscoder() {
            @Override
            public void recode(Path input, Path output) {
                throw new IllegalStateException("no encoder");
            }

            @Override
            public void poster(Path input, Path output) {
            }
        };
        VideoTranscodeJob.of(resources, boom, null, null, null, this::info).run(id);
        ArgumentCaptor<ResourceEntity> saved = ArgumentCaptor.forClass(ResourceEntity.class);
        verify(resources).replace(saved.capture());
        assertTrue(VideoStatus.failed(saved.getValue().getResourceData()));
        verify(resources, never()).deleteStoredContent(any());
    }

    private VideoProbe.Info info(Path file) {
        return new VideoProbe.Info(640, 360, 2.0);
    }

    private static VideoTranscoder fakeTranscoder() {
        return new VideoTranscoder() {
            @Override
            public void recode(Path input, Path output) throws IOException {
                Files.writeString(output, "mp4");
            }

            @Override
            public void poster(Path input, Path output) throws IOException {
                throw new IOException("skip poster");
            }
        };
    }

    private static ResourceEntity processingVideo(ObjectId id) {
        ResourceData data = new ResourceData();
        data.setType(ResourceType.VIDEO);
        data.setPathPublic("/u/res/video/clip.mp4");
        data.setPathProtected("/abc/clip.mp4");
        VideoStatus.set(data, VideoStatus.PROCESSING);
        data.setSizeInBytes(4096);
        ResourceEntity entity = new ResourceEntity();
        entity.setId(EntityId.of(id.toHexString()));
        entity.setResourceData(data);
        return entity;
    }
}

package com.ravenherz.cse.controller;

import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.StoredIds;
import com.ravenherz.cse.dal.dao.AccountService;
import com.ravenherz.cse.dal.dao.ResourceService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.dto.basic.ResourceData;
import com.ravenherz.cse.dal.dto.basic.enums.ResourceType;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import com.ravenherz.cse.engine.video.VideoProgress;
import com.ravenherz.cse.engine.video.VideoStatus;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EditorTranscodeControllerTest {

    @Test
    void snapshotMergesLiveProgressAndStoreProcessing() {
        ObjectId live = new ObjectId();
        ObjectId stored = new ObjectId();
        VideoProgress progress = new VideoProgress();
        progress.start(live);
        progress.percent(live, 42);
        ResourceEntity entity = processingVideo(stored, "/u/res/video/clip.mp4");
        ResourceService resources = mock(ResourceService.class);
        when(resources.listProcessingVideoIds()).thenReturn(List.of(stored, live));
        when(resources.getById(ResourceEntity.class, StoredIds.entityId(stored))).thenReturn(entity);
        ResourceEntity liveEntity = processingVideo(live, "/u/res/video/live.mov");
        when(resources.getById(ResourceEntity.class, StoredIds.entityId(live))).thenReturn(liveEntity);
        EditorTranscodeController controller = new EditorTranscodeController(progress, of(resources), of(null));
        controller.serviceProvider = accounts();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/rhz-we");
        EditorTranscodeController.QueueSnapshot snapshot = controller.snapshot(request);
        assertEquals(2, snapshot.videos().size());
        assertEquals(2, snapshot.running());
        assertEquals(0, snapshot.queued());
        assertEquals("live.mov", snapshot.videos().get(0).fileName());
        assertEquals(42, snapshot.videos().get(0).percent());
        assertEquals("ada", snapshot.videos().get(0).author());
        assertEquals("2048 bytes", snapshot.videos().get(0).sizeIn());
        assertEquals(null, snapshot.videos().get(0).sizeOut());
        assertEquals("clip.mp4", snapshot.videos().get(1).fileName());
        assertEquals(0, snapshot.videos().get(1).percent());
        assertEquals("ada", snapshot.videos().get(1).author());
        assertEquals("2048 bytes", snapshot.videos().get(1).sizeIn());
        assertEquals(null, snapshot.videos().get(1).sizeOut());
    }

    @Test
    void snapshotShowsUploaderAndInputOutputSizesWhenReady() {
        ObjectId id = new ObjectId();
        VideoProgress progress = new VideoProgress();
        progress.start(id);
        progress.ready(id, "/u/res/preview.jpg", "1024 bytes");
        ResourceEntity entity = processingVideo(id, "/u/res/video/clip.mp4");
        VideoStatus.set(entity.getResourceData(), VideoStatus.READY);
        entity.getResourceData().setSizeInBytes(1024);
        ResourceService resources = mock(ResourceService.class);
        when(resources.listProcessingVideoIds()).thenReturn(List.of());
        when(resources.getById(ResourceEntity.class, StoredIds.entityId(id))).thenReturn(entity);
        EditorTranscodeController controller = new EditorTranscodeController(progress, of(resources), of(null));
        controller.serviceProvider = accounts();
        EditorTranscodeController.Item item = controller.snapshot(new MockHttpServletRequest()).videos().get(0);
        assertEquals("ada", item.author());
        assertEquals("2048 bytes", item.sizeIn());
        assertEquals("1024 bytes", item.sizeOut());
        assertEquals("ready", item.status());
    }

    private static ServiceProvider accounts() {
        AccountEntity ada = new AccountEntity(new AccountData("ada", "hash", "ada@example.com",
                SecurityLevel.ADMIN));
        AccountService accountService = mock(AccountService.class);
        when(accountService.getById(eq(AccountEntity.class), any())).thenReturn(ada);
        ServiceProvider services = mock(ServiceProvider.class);
        when(services.getAccountService()).thenReturn(accountService);
        return services;
    }

    private static ResourceEntity processingVideo(ObjectId id, String pathPublic) {
        ResourceData data = new ResourceData();
        data.setType(ResourceType.VIDEO);
        data.setPathPublic(pathPublic);
        data.setSizeInBytes(2048);
        VideoStatus.set(data, VideoStatus.PROCESSING);
        VideoStatus.rememberSourceSize(data);
        AccountEntity ada = new AccountEntity(new AccountData("ada", "hash", "ada@example.com",
                SecurityLevel.ADMIN));
        ada.setId(EntityId.generate());
        ResourceEntity entity = new ResourceEntity(data, ada.getId());
        entity.setId(EntityId.of(id.toHexString()));
        return entity;
    }

    private static <T> ObjectProvider<T> of(T value) {
        return new ObjectProvider<>() {
            @Override
            public T getObject() {
                return value;
            }

            @Override
            public T getIfAvailable() {
                return value;
            }

            @Override
            public T getIfUnique() {
                return value;
            }
        };
    }
}

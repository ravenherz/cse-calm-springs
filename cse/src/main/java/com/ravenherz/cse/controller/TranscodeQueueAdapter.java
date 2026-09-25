package com.ravenherz.cse.controller;

import com.ravenherz.cse.admin.TranscodeQueue;
import com.ravenherz.cse.dal.EntityAccess;
import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.StoredIds;
import com.ravenherz.cse.dal.dao.ResourceService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.basic.Event;
import com.ravenherz.cse.dal.dto.basic.ResourceData;
import com.ravenherz.cse.dal.dto.basic.enums.EventType;
import com.ravenherz.cse.dal.dto.basic.enums.ResourceType;
import com.ravenherz.cse.engine.video.VideoProgress;
import com.ravenherz.cse.engine.video.VideoStatus;
import com.ravenherz.cse.engine.video.VideoTranscodeQueue;
import jakarta.servlet.http.HttpServletRequest;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TranscodeQueueAdapter implements TranscodeQueue {

    private final VideoProgress progress;
    private final ResourceService resources;
    private final VideoTranscodeQueue queue;
    private final ServiceProvider serviceProvider;

    public TranscodeQueueAdapter(VideoProgress progress, ObjectProvider<ResourceService> resources,
            ObjectProvider<VideoTranscodeQueue> queue, ServiceProvider serviceProvider) {
        this.progress = progress;
        this.resources = resources == null ? null : resources.getIfAvailable();
        this.queue = queue == null ? null : queue.getIfAvailable();
        this.serviceProvider = serviceProvider;
    }

    @Override
    public QueueSnapshot snapshot(HttpServletRequest request) {
        Map<String, VideoProgress.View> byId = new LinkedHashMap<>();
        if (progress != null) {
            for (VideoProgress.View view : progress.views()) {
                byId.put(view.id(), view);
            }
        }
        if (resources != null) {
            List<ObjectId> processing = resources.listProcessingVideoIds();
            if (processing != null) {
                for (ObjectId id : processing) {
                    if (id == null || byId.containsKey(id.toHexString())) {
                        continue;
                    }
                    VideoProgress.View stored = fromStore(id);
                    if (stored != null) {
                        byId.put(stored.id(), stored);
                    }
                }
            }
        }
        List<Item> videos = new ArrayList<>();
        int running = 0;
        int queued = 0;
        int failed = 0;
        int ready = 0;
        for (VideoProgress.View view : byId.values()) {
            if (VideoProgress.PROCESSING.equals(view.status())) {
                running++;
            } else if (VideoProgress.QUEUED.equals(view.status())) {
                queued++;
            } else if (VideoProgress.FAILED.equals(view.status())) {
                failed++;
            } else if (VideoProgress.READY.equals(view.status())) {
                ready++;
            }
            videos.add(item(view, request));
        }
        videos.sort((a, b) -> {
            int rank = Integer.compare(rank(a.status()), rank(b.status()));
            if (rank != 0) {
                return rank;
            }
            if (VideoProgress.PROCESSING.equals(a.status())) {
                return Integer.compare(b.percent(), a.percent());
            }
            return a.id().compareTo(b.id());
        });
        return new QueueSnapshot(running, queued, failed, ready,
                queue == null ? 0 : queue.workerRunning(),
                queue == null ? 0 : queue.workerWaiting(),
                videos);
    }

    private Item item(VideoProgress.View view, HttpServletRequest request) {
        ObjectId id = VideoProgressAdapter.parseId(view.id());
        ResourceEntity resource = loadResource(id);
        ResourceData data = resource == null ? null : resource.getResourceData();
        String fileName = data == null ? null : blankToNull(data.getFileName());
        return new Item(view.id(), view.status(), view.percent(), view.error(),
                VideoProgressAdapter.href(request, view.preview()),
                author(resource, data), sizeIn(view.status(), data), sizeOut(view.status(), data),
                fileName);
    }

    private ResourceEntity loadResource(ObjectId id) {
        if (id == null || resources == null) {
            return null;
        }
        BasicEntity found = resources.getById(ResourceEntity.class, StoredIds.entityId(id));
        return found instanceof ResourceEntity resource ? resource : null;
    }

    private String author(ResourceEntity resource, ResourceData data) {
        if (resource != null && resource.getHistoryData() != null
                && resource.getHistoryData().getEvents() != null) {
            Event created = EntityAccess.getLastEventByType(EventType.ENTITY_CREATED, resource);
            if (created != null && created.getOwnerId() != null
                    && serviceProvider != null && serviceProvider.getAccountService() != null) {
                BasicEntity found = serviceProvider.getAccountService()
                        .getById(AccountEntity.class, created.getOwnerId());
                String login = found instanceof AccountEntity account ? loginOf(account) : null;
                if (login != null) {
                    return login;
                }
            }
        }
        return pathLogin(data == null ? null : data.getPathPublic());
    }

    private static String sizeIn(String status, ResourceData data) {
        Long source = VideoStatus.sourceSizeBytes(data);
        if (source != null) {
            return VideoStatus.sizeLabel(source);
        }
        if (outputKnown(status) || data == null || data.getSizeInBytes() <= 0) {
            return null;
        }
        return VideoStatus.sizeLabel(data.getSizeInBytes());
    }

    private static String sizeOut(String status, ResourceData data) {
        if (!outputKnown(status) || data == null || data.getSizeInBytes() <= 0) {
            return null;
        }
        return VideoStatus.sizeLabel(data.getSizeInBytes());
    }

    private static boolean outputKnown(String status) {
        return VideoProgress.READY.equals(status);
    }

    private static String loginOf(AccountEntity account) {
        if (account == null || account.getAccountData() == null) {
            return null;
        }
        return blankToNull(account.getAccountData().getLogin());
    }

    private static String pathLogin(String pathPublic) {
        if (pathPublic == null || pathPublic.isBlank()) {
            return null;
        }
        String path = pathPublic.startsWith("/") ? pathPublic.substring(1) : pathPublic;
        int slash = path.indexOf('/');
        return slash > 0 ? blankToNull(path.substring(0, slash)) : null;
    }

    private VideoProgress.View fromStore(ObjectId id) {
        if (resources == null) {
            return null;
        }
        BasicEntity found = resources.getById(ResourceEntity.class, StoredIds.entityId(id));
        if (!(found instanceof ResourceEntity resource) || resource.getResourceData() == null
                || resource.getResourceData().getType() != ResourceType.VIDEO) {
            return null;
        }
        ResourceData data = resource.getResourceData();
        String size = data.getSizeLabel();
        String preview = resource.getPreviewData() == null ? null : resource.getPreviewData().getPathPublic();
        if (VideoStatus.processing(data)) {
            return new VideoProgress.View(id.toHexString(), VideoProgress.PROCESSING, 0, null, null, size);
        }
        if (VideoStatus.failed(data)) {
            String error = data.getMetadata() == null ? null : data.getMetadata().get(VideoStatus.ERROR_KEY);
            return new VideoProgress.View(id.toHexString(), VideoProgress.FAILED, 0, error, null, size);
        }
        if (VideoStatus.ready(data)) {
            return new VideoProgress.View(id.toHexString(), VideoProgress.READY, 100, null, preview, size);
        }
        return null;
    }

    private static int rank(String status) {
        if (VideoProgress.PROCESSING.equals(status)) {
            return 0;
        }
        if (VideoProgress.QUEUED.equals(status)) {
            return 1;
        }
        if (VideoProgress.FAILED.equals(status)) {
            return 2;
        }
        return 3;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

package com.ravenherz.cse.controller;

import com.ravenherz.cse.dal.dao.ResourceService;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.basic.ResourceData;
import com.ravenherz.cse.dal.dto.basic.enums.ResourceType;
import com.ravenherz.cse.util.video.VideoProgress;
import com.ravenherz.cse.util.video.VideoStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/editor/video")
public class EditorVideoController extends AbstractController {

    private final VideoProgress progress;
    private final ResourceService resources;

    @Autowired
    public EditorVideoController(VideoProgress progress, ObjectProvider<ResourceService> resources) {
        this.progress = progress;
        this.resources = resources == null ? null : resources.getIfAvailable();
    }

    @GetMapping(value = "/progress", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> progress(@RequestParam(value = "ids", required = false) String ids,
            HttpServletRequest request) {
        List<VideoProgress.View> videos = new ArrayList<>();
        for (String raw : splitIds(ids)) {
            ObjectId id = parseId(raw);
            if (id == null) {
                continue;
            }
            VideoProgress.View view = progress == null ? null : progress.view(id);
            if (view == null) {
                view = fromStore(id);
            }
            if (view != null) {
                videos.add(view.withPreview(href(request, view.preview())));
            }
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("videos", videos);
        return ResponseEntity.ok(body);
    }

    private VideoProgress.View fromStore(ObjectId id) {
        if (resources == null) {
            return null;
        }
        BasicEntity found = resources.getById(ResourceEntity.class, id);
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

    static String href(HttpServletRequest request, String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String ctx = request == null || request.getContextPath() == null ? "" : request.getContextPath();
        if ("/".equals(ctx)) {
            ctx = "";
        }
        String relative = path.startsWith("/") ? path : "/" + path;
        return ctx + "/content-protected" + relative;
    }

    static List<String> splitIds(String ids) {
        if (ids == null || ids.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String part : ids.split(",")) {
            if (part != null && !part.isBlank()) {
                out.add(part.trim());
            }
        }
        return out;
    }

    static ObjectId parseId(String raw) {
        if (raw == null || !ObjectId.isValid(raw.trim())) {
            return null;
        }
        return new ObjectId(raw.trim());
    }
}

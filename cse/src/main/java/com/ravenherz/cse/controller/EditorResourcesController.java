package com.ravenherz.cse.controller;

import com.ravenherz.cse.dal.ResourceGroupTree;
import com.ravenherz.cse.dal.dto.*;
import com.ravenherz.cse.dal.dto.basic.*;
import com.ravenherz.cse.dal.dto.basic.enums.AccessType;
import com.ravenherz.cse.dal.dto.basic.enums.ResourceType;
import com.ravenherz.cse.dal.EntityAccess;
import com.ravenherz.cse.present.EditorTree;
import com.ravenherz.cse.present.ResourceFileNames;
import com.ravenherz.cse.present.ResourceGroupDisplayDTO;
import com.ravenherz.cse.present.ResourceGroupIndex;
import com.ravenherz.cse.present.ResourceGroupTreeView;
import com.ravenherz.cse.util.imaging.HeicJpegConverter;
import com.ravenherz.cse.util.imaging.ImageMetadata;
import com.ravenherz.cse.util.imaging.ImageUploadOptions;
import com.ravenherz.cse.util.imaging.JpegImages;
import com.ravenherz.cse.util.imaging.PdfPreviews;
import com.ravenherz.cse.engine.util.Json;
import com.ravenherz.cse.util.Mp3Metadata;
import com.ravenherz.cse.util.Mp3Waveform;
import com.ravenherz.cse.util.ResourceUploadLimits;
import com.ravenherz.cse.util.UrlTemplateIds;
import com.ravenherz.cse.engine.video.VideoStatus;
import com.ravenherz.cse.engine.video.VideoTranscodeQueue;
import com.ravenherz.cse.util.video.VideoWork;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.bson.types.ObjectId;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.ravenherz.cse.dal.dto.basic.enums.ResourceType.IMAGE;

@Controller
@RequestMapping("/editor")
public class EditorResourcesController extends AbstractController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditorResourcesController.class);

    @Autowired
    private ContentProtectedAndCacheController contentCacheController;

    @Autowired
    private ResourceGroupIndex resourceGroupIndex;

    @Autowired
    private CatalogBatchDelete catalogBatchDelete;

    @Autowired(required = false)
    private VideoTranscodeQueue videoTranscodeQueue;

    @GetMapping("/resources")
    public String resourcesPage(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "notice", required = false) String notice,
                                @RequestParam(value = "group", required = false) String group,
                                Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        ensureUnsortedName();
        if (EditorTree.isChromeId(group) && !EditorTree.browseInResources(group)) {
            response.sendRedirect(request.getContextPath() + EditorTree.hrefOf(group));
            return null;
        }
        if (ResourceGroupTreeView.isHomeAlias(group)) {
            String home = homeGroupId();
            if (!home.isEmpty()) {
                redirectResources(request, response, error, home);
                return null;
            }
        }
        fillResourcesPage(model, accessor, error, group);
        if (notice != null && !notice.isBlank() && model.getAttribute("error") == null) {
            model.addAttribute("error", notice);
        }
        return "/admin/editor-resources";
    }

    @GetMapping("/content")
    public void contentRedirect(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(request.getContextPath() + EditorTree.CONTENT_HREF);
    }

    @PostMapping("/resources/upload")
    public String uploadResource(@RequestParam("resourceId") String resourceId,
                                 @RequestParam("file") MultipartFile file,
                                 @RequestParam(value = "metadata", required = false) String metadataJson,
                                 @RequestParam(value = "imageDescription", required = false) String imageDescription,
                                 @RequestParam(value = "groupId", required = false) String groupId,
                                 Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }

        if (resourceId == null || resourceId.trim().isEmpty()) {
            model.addAttribute("error", "Resource ID is required");
            return loadResourcesWithError(model, accessor, request);
        }

        if (file == null || file.isEmpty()) {
            model.addAttribute("error", "File is required");
            return loadResourcesWithError(model, accessor, request);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            model.addAttribute("error", "File must have an extension");
            return loadResourcesWithError(model, accessor, request);
        }

        ResourceType resourceType = ResourceType.getByFileName(originalFilename);
        if (resourceType == ResourceType.INVALID) {
            model.addAttribute("error", "Invalid file type. Supported: jpg, png, heic, mp3, mp4, mov, webm, mkv, pdf");
            return loadResourcesWithError(model, accessor, request);
        }
        ResourceUploadLimits limits = ResourceUploadLimits.from(settings);
        if (file.getSize() > limits.maxBytes(resourceType)) {
            model.addAttribute("error", limits.tooLargeMessage(resourceType));
            return loadResourcesWithError(model, accessor, request);
        }
        if (resourceType == ResourceType.VIDEO) {
            return uploadVideo(resourceId.trim(), file, originalFilename, metadataJson, groupId,
                    accessor, model, request, response);
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        byte[] content = file.getBytes();
        if (resourceType == ResourceType.BINARY && !PdfPreviews.looksLikePdf(content)) {
            model.addAttribute("error", "Invalid PDF");
            return loadResourcesWithError(model, accessor, request);
        }
        ImageMetadata.Parsed imageMeta = resourceType == IMAGE ? ImageMetadata.parse(content) : null;
        ImageUploadOptions uploadOptions = ImageUploadOptions.from(settings);
        Integer convertedWidth = null;
        Integer convertedHeight = null;
        String userId = accessor.getAccountData().getLogin();
        if (HeicJpegConverter.isHeicExtension(extension) && JpegImages.looksLikeJpeg(content)) {
            LOGGER.info("Upload named .{} is a JPEG; storing as JPEG", extension);
            extension = "jpg";
        } else if (HeicJpegConverter.isHeicExtension(extension)) {
            try {
                String publicPath = String.format("/%s/res/%s/%s.jpg",
                        userId, resourceType.getPath(), resourceId.trim());
                JpegImages.Encoded jpeg = HeicJpegConverter.toJpeg(
                        content, uploadOptions.qualityFactor(), userId, publicPath);
                content = jpeg.bytes();
                extension = "jpg";
                convertedWidth = jpeg.width();
                convertedHeight = jpeg.height();
            } catch (OutOfMemoryError e) {
                LOGGER.warn("HEIC conversion ran out of memory: " + e.getMessage(), e);
                model.addAttribute("error", "Could not convert HEIC image (not enough memory). Try exporting as JPEG first.");
                return loadResourcesWithError(model, accessor, request);
            } catch (Exception e) {
                LOGGER.warn("HEIC conversion failed: " + e.getMessage(), e);
                model.addAttribute("error", "Could not convert HEIC image. Try exporting as JPEG first.");
                return loadResourcesWithError(model, accessor, request);
            }
        }

        String pathPublic = String.format("/%s/res/%s/%s.%s", userId, resourceType.getPath(), resourceId.trim(), extension);

        ResourceEntity existing = serviceProvider.getResourceService().getByPublicPath(pathPublic);
        if (existing != null) {
            model.addAttribute("error", "Resource with this ID already exists");
            return loadResourcesWithError(model, accessor, request);
        }

        String pathProtected = generateUniqueProtectedPath(resourceId.trim(), extension);

        ResourceData resourceData = new ResourceData();
        resourceData.setType(resourceType);
        resourceData.setPathPublic(pathPublic);
        resourceData.setPathProtected(pathProtected);
        resourceData.setImageDescription(imageDescription);

        if (metadataJson != null && !metadataJson.trim().isEmpty()) {
            try {
                Map<String, String> metadata = Json.stringMap(metadataJson);
                resourceData.setMetadata(metadata);
            } catch (Exception e) {
                LOGGER.warn("Failed to parse metadata: " + e.getMessage());
            }
        }
        if (imageMeta != null) {
            ImageMetadata.applyTo(resourceData, imageMeta);
        }
        if (convertedWidth != null && convertedHeight != null) {
            ImageMetadata.setDimensions(resourceData, convertedWidth, convertedHeight);
        }
        Mp3Metadata.Parsed mp3 = null;
        if (resourceType == ResourceType.AUDIO) {
            mp3 = Mp3Metadata.parse(content);
            Mp3Metadata.applyTo(resourceData, mp3);
            byte[] waveform = Mp3Waveform.extract(content);
            if (waveform != null) {
                resourceData.setWaveform(waveform);
            }
        }

        fillResourceContent(resourceData, content);

        ResourceEntity resourceEntity = new ResourceEntity(resourceData, accessor);
        ImageMetadata.applyCreationDate(resourceEntity, imageMeta);
        if (resourceType == IMAGE) {
            ResourceData previewData = buildImagePreview(resourceData, content, resourceId.trim(),
                    extension, userId, uploadOptions);
            resourceEntity.setPreviewData(previewData);
        } else if (resourceType == ResourceType.BINARY) {
            resourceEntity.setPreviewData(buildPdfPreview(resourceData, content, resourceId.trim(),
                    extension, userId, uploadOptions));
        } else if (mp3 != null && mp3.artworkBytes() != null) {
            resourceEntity.setPreviewData(buildAudioCoverPreview(mp3.artworkBytes(),
                    resourceId.trim(), userId, uploadOptions));
        }
        ResourceGroupEntity uploadGroup = loadGroup(groupId);
        if (uploadGroup == null) {
            uploadGroup = loadDefaultGroup();
        }
        if (uploadGroup != null) {
            resourceEntity.setRefResourceGroup(uploadGroup);
        }
        serviceProvider.getResourceService().insert(resourceEntity);
        resourceGroupIndex.fileAdded(resourceEntity);

        redirectResources(request, response, null);
        return null;
    }

    private String uploadVideo(String resourceId, MultipartFile file, String originalFilename,
            String metadataJson, String groupId, AccountEntity accessor, Model model,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        String userId = accessor.getAccountData().getLogin();
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
        String pathPublic = String.format("/%s/res/%s/%s.mp4",
                userId, ResourceType.VIDEO.getPath(), resourceId);
        ResourceEntity existing = serviceProvider.getResourceService().getByPublicPath(pathPublic);
        if (existing != null) {
            model.addAttribute("error", "Resource with this ID already exists");
            return loadResourcesWithError(model, accessor, request);
        }
        Path temp = null;
        try {
            temp = VideoWork.temp("upload-", "." + extension);
            file.transferTo(temp);
            ResourceData resourceData = new ResourceData();
            resourceData.setType(ResourceType.VIDEO);
            resourceData.setPathPublic(pathPublic);
            resourceData.setPathProtected(generateUniqueProtectedPath(resourceId, "mp4"));
            if (metadataJson != null && !metadataJson.trim().isEmpty()) {
                try {
                    Map<String, String> metadata = Json.stringMap(metadataJson);
                    resourceData.setMetadata(metadata);
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse metadata: {}", e.getMessage());
                }
            }
            VideoStatus.set(resourceData, VideoStatus.PROCESSING);
            resourceData.addMetadata(VideoStatus.SOURCE_EXT_KEY, extension);
            serviceProvider.getResourceService().fillFromFile(resourceData, temp);
            VideoStatus.rememberSourceSize(resourceData);
            ResourceEntity resourceEntity = new ResourceEntity(resourceData, accessor);
            ResourceGroupEntity uploadGroup = loadGroup(groupId);
            if (uploadGroup == null) {
                uploadGroup = loadDefaultGroup();
            }
            if (uploadGroup != null) {
                resourceEntity.setRefResourceGroup(uploadGroup);
            }
            serviceProvider.getResourceService().insert(resourceEntity);
            resourceGroupIndex.fileAdded(resourceEntity);
            if (videoTranscodeQueue != null && resourceEntity.getId() != null) {
                videoTranscodeQueue.enqueue(resourceEntity.getId());
            }
        } catch (Exception e) {
            LOGGER.warn("Video upload failed: {}", e.getMessage(), e);
            model.addAttribute("error", "Could not store video");
            return loadResourcesWithError(model, accessor, request);
        } finally {
            VideoWork.deleteQuietly(temp);
        }
        redirectResources(request, response, null);
        return null;
    }

    @GetMapping("/resources/download")
    public void downloadResource(@RequestParam("pathPublic") String pathPublic,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return;
        }
        if (pathPublic == null || pathPublic.isBlank()) {
            error(400, request, response);
            return;
        }
        ResourceEntity existing = serviceProvider.getResourceService().getByPublicPath(pathPublic.trim());
        if (existing == null || existing.getResourceData() == null
                || !EntityAccess.isAccessible(existing, AccessType.ACCESS_READ, accessor)) {
            error(404, request, response);
            return;
        }
        byte[] bytes = existing.getRawBytes();
        if (bytes == null && existing.getResourceData().isLargeFile()
                && existing.getResourceData().getDataChunkIds() != null) {
            bytes = serviceProvider.getResourceService().getRawBytesFromChunks(
                    existing.getResourceData().getDataChunkIds());
        }
        if (bytes == null) {
            LOGGER.error("No bytes for resource download: {}", pathPublic);
            error(500, request, response);
            return;
        }
        String fileName = existing.getResourceData().getFileName();
        if (fileName == null || fileName.isBlank()) {
            fileName = "resource";
        }
        fileName = fileName.replace("\"", "").replace("\r", "").replace("\n", "");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
    }

    @GetMapping(value = "/resources/tree-preview/{id}", produces = MediaType.IMAGE_JPEG_VALUE)
    public void treePreview(@PathVariable("id") String id,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return;
        }
        byte[] jpeg = resourceGroupIndex.treePreview(id);
        if (jpeg == null || jpeg.length == 0) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setContentType(MediaType.IMAGE_JPEG_VALUE);
        response.setContentLength(jpeg.length);
        response.setHeader("Cache-Control", "private, no-cache");
        response.getOutputStream().write(jpeg);
        response.getOutputStream().flush();
    }

    @PostMapping("/resources/delete")
    public String deleteResource(@RequestParam("pathPublic") String pathPublic,
                                 Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }

        if (pathPublic == null || pathPublic.trim().isEmpty()) {
            model.addAttribute("error", "Resource path is required");
            return loadResourcesWithError(model, accessor, request);
        }

        ResourceEntity existing = serviceProvider.getResourceService().getByPublicPath(pathPublic);
        if (existing == null) {
            model.addAttribute("error", "Resource not found");
            return loadResourcesWithError(model, accessor, request);
        }

        LOGGER.info("Deleting resource: " + pathPublic + " with id: " + existing.getId());
        try {
            List<ItemEntity> itemsWithRefImage = serviceProvider.getItemService().getAllByRefImage(existing);
            for (ItemEntity item : itemsWithRefImage) {
                item.getPageData().setRefImage(null);
                serviceProvider.getItemService().replace(item);
                LOGGER.info("Cleared refImage on item: " + item.getUniqueUriName());
            }
            List<PlaylistEntity> playlistsWithCover = serviceProvider.getPlaylistService()
                    .getAllByRefImage(existing);
            if (playlistsWithCover != null) {
                for (PlaylistEntity playlist : playlistsWithCover) {
                    if (playlist.getPlaylistData() != null) {
                        playlist.getPlaylistData().setRefImage(null);
                        serviceProvider.getPlaylistService().replace(playlist);
                    }
                }
                if (!playlistsWithCover.isEmpty()) {
                    resourceGroupIndex.contentChanged();
                }
            }

            serviceProvider.getResourceService().deleteByPublicPath(pathPublic);
            contentCacheController.invalidateCacheForResource(pathPublic);
            if (existing.getPreviewData() != null && existing.getPreviewData().getPathPublic() != null) {
                contentCacheController.invalidateCacheForResource(existing.getPreviewData().getPathPublic());
            }
            resourceGroupIndex.fileRemoved(existing);
        } catch (Exception e) {
            LOGGER.error("Failed to delete resource: " + e.getMessage(), e);
            model.addAttribute("error", "Failed to delete resource");
            return loadResourcesWithError(model, accessor, request);
        }

        redirectResources(request, response, null);
        return null;
    }

    @PostMapping("/resources/description")
    public String updateResourceDescription(@RequestParam("pathPublic") String pathPublic,
            @RequestParam(value = "imageDescription", required = false) String imageDescription,
            Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        if (pathPublic == null || pathPublic.isBlank()) {
            model.addAttribute("error", "Resource path is required");
            return loadResourcesWithError(model, accessor, request);
        }
        ResourceEntity existing = serviceProvider.getResourceService().getByPublicPath(pathPublic.trim());
        if (existing == null || existing.getResourceData() == null) {
            model.addAttribute("error", "Resource not found");
            return loadResourcesWithError(model, accessor, request);
        }
        existing.getResourceData().setImageDescription(imageDescription);
        serviceProvider.getResourceService().replace(existing);
        redirectResources(request, response, null);
        return null;
    }

    @PostMapping("/resources/access")
    public String updateResourceAccess(@RequestParam("pathPublic") String pathPublic,
            Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        if (pathPublic == null || pathPublic.isBlank()) {
            model.addAttribute("error", "Resource path is required");
            return loadResourcesWithError(model, accessor, request);
        }
        ResourceEntity existing = serviceProvider.getResourceService().getByPublicPath(pathPublic.trim());
        if (existing == null) {
            model.addAttribute("error", "Resource not found");
            return loadResourcesWithError(model, accessor, request);
        }
        if (!EntityAccess.isAccessible(existing, AccessType.ACCESS_EDIT, accessor)) {
            error(403, request, response);
            return null;
        }
        applyAccess(request, existing);
        serviceProvider.getResourceService().replace(existing);
        redirectResources(request, response, null);
        return null;
    }

    @PostMapping("/resources/group/access")
    public String updateResourceGroupAccess(@RequestParam("groupId") String groupId,
            Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        ResourceGroupEntity group = loadGroup(groupId);
        if (group == null || ResourceGroupTree.isDefault(group)) {
            redirectResources(request, response, "invalid_group");
            return null;
        }
        if (!EntityAccess.isAccessible(group, AccessType.ACCESS_EDIT, accessor)) {
            error(403, request, response);
            return null;
        }
        applyAccess(request, group);
        serviceProvider.getResourceGroupService().replace(group);
        resourceGroupIndex.structureChanged();
        redirectResources(request, response, null);
        return null;
    }

    @PostMapping("/resources/group/assign")
    public String assignResourceToGroup(@RequestParam("resourceId") List<String> resourceIds,
                                        @RequestParam(value = "groupId", required = false) String groupId,
                                        Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }

        ResourceGroupEntity group = loadGroup(groupId);
        if (group == null) {
            group = loadDefaultGroup();
        }
        for (String resourceId : uniqueIds(resourceIds)) {
            ObjectId objId = parseObjectId(resourceId);
            if (objId == null) {
                continue;
            }
            try {
                ResourceEntity resource = (ResourceEntity) serviceProvider.getResourceService()
                        .getById(ResourceEntity.class, objId);
                if (resource == null) {
                    continue;
                }
                String fromGroup = ResourceGroupTreeView.statsKey(resource);
                long bytes = ResourceGroupTreeView.byteSize(resource);
                resource.setRefResourceGroup(group);
                serviceProvider.getResourceService().replace(resource);
                resourceGroupIndex.fileMoved(fromGroup, ResourceGroupTreeView.statsKey(resource), bytes, resource);
            } catch (Exception e) {
                LOGGER.error("Failed to assign resource to group: " + e.getMessage(), e);
            }
        }

        redirectResources(request, response, null);
        return null;
    }

    @PostMapping("/resources/group/move")
    public String moveResourceGroup(@RequestParam("groupId") List<String> groupIds,
                                    @RequestParam(value = "parentId", required = false) String parentId,
                                    Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        ObjectId newParentId = null;
        if (parentId != null && !parentId.trim().isEmpty()) {
            newParentId = parseObjectId(parentId);
            if (newParentId == null) {
                redirectResources(request, response, "invalid_parent");
                return null;
            }
        }
        try {
            List<ResourceGroupEntity> all = allGroups();
            ResourceGroupEntity parent = newParentId == null ? null : ResourceGroupTree.find(all, newParentId);
            String refused = null;
            boolean moved = false;
            for (String groupId : uniqueIds(groupIds)) {
                ObjectId groupObjId = parseObjectId(groupId);
                if (groupObjId == null) {
                    refused = refused == null ? "invalid_group" : refused;
                    continue;
                }
                ResourceGroupEntity group = ResourceGroupTree.find(all, groupObjId);
                String block = ResourceGroupTree.refuseMove(all, group, newParentId);
                if (block != null) {
                    refused = refused == null ? block : refused;
                    continue;
                }
                group.setRefParentGroup(parent);
                serviceProvider.getResourceGroupService().replace(group);
                moved = true;
            }
            if (moved) {
                resourceGroupIndex.structureChanged();
            } else if (refused != null) {
                redirectResources(request, response, refused);
                return null;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to move resource group: " + e.getMessage(), e);
            redirectResources(request, response, "invalid_group");
            return null;
        }
        redirectResources(request, response, null);
        return null;
    }

    @PostMapping("/resources/group/delete")
    public String deleteResourceGroup(@RequestParam("groupId") String groupId,
                                       Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }

        if (groupId == null || groupId.trim().isEmpty() || ResourceGroupTreeView.isHomeAlias(groupId)) {
            redirectResources(request, response, null);
            return null;
        }

        try {
            ObjectId groupObjId = parseObjectId(groupId);
            if (groupObjId == null) {
                redirectResources(request, response, "invalid_group");
                return null;
            }
            List<ResourceGroupEntity> all = allGroups();
            ResourceGroupEntity group = ResourceGroupTree.find(all, groupObjId);
            String refused = ResourceGroupTree.refuseDelete(all, group);
            if (refused != null) {
                redirectResources(request, response, refused);
                return null;
            }
            String deletedId = group.getId().toString();
            ObjectId parentId = group.refParentGroupObjectId();
            for (ResourceEntity resource : serviceProvider.getResourceService().listForEditor(groupObjId)) {
                if (resource.getResourceData() == null) {
                    continue;
                }
                String pathPublic = resource.getResourceData().getPathPublic();
                if (pathPublic == null) {
                    continue;
                }
                String previewPath = resource.getPreviewData() == null
                        ? null : resource.getPreviewData().getPathPublic();
                serviceProvider.getResourceService().deleteByPublicPath(pathPublic);
                contentCacheController.invalidateCacheForResource(pathPublic);
                if (previewPath != null) {
                    contentCacheController.invalidateCacheForResource(previewPath);
                }
            }
            serviceProvider.getResourceGroupService().delete(group);
            resourceGroupIndex.structureChanged();
            String next = returnGroupId(request);
            if (deletedId.equals(next)) {
                next = parentId == null ? homeGroupId() : parentId.toString();
            }
            redirectResources(request, response, null, next);
            return null;
        } catch (Exception e) {
            LOGGER.error("Failed to delete resource group: " + e.getMessage(), e);
            redirectResources(request, response, "invalid_group");
            return null;
        }
    }

    @PostMapping("/resources/group/create")
    public String createResourceGroup(@RequestParam("humanReadableId") String humanReadableId,
                                       @RequestParam(value = "parentId", required = false) String parentId,
                                       Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }

        if (humanReadableId == null || humanReadableId.trim().isEmpty()) {
            redirectResources(request, response, null);
            return null;
        }

        ObjectId parentObjId = null;
        if (parentId != null && !parentId.trim().isEmpty()) {
            parentObjId = parseObjectId(parentId);
            if (parentObjId == null) {
                redirectResources(request, response, "invalid_parent");
                return null;
            }
        }

        try {
            List<ResourceGroupEntity> existingGroups = allGroups();
            String refused = ResourceGroupTree.refuseCreate(existingGroups, humanReadableId.trim(), parentObjId);
            if (refused != null) {
                LOGGER.warn("Refused resource group create '{}': {}", humanReadableId, refused);
                redirectResources(request, response, refused);
                return null;
            }

            ResourceGroupData groupData = new ResourceGroupData();
            groupData.setHumanReadableId(humanReadableId.trim());
            ResourceGroupEntity newGroup = new ResourceGroupEntity(groupData, accessor);
            if (parentObjId != null) {
                newGroup.setRefParentGroup(ResourceGroupTree.find(existingGroups, parentObjId));
            }
            serviceProvider.getResourceGroupService().insert(newGroup);
            resourceGroupIndex.structureChanged();
        } catch (Exception e) {
            LOGGER.error("Failed to create resource group: " + e.getMessage(), e);
            redirectResources(request, response, "invalid_group");
            return null;
        }

        redirectResources(request, response, null);
        return null;
    }

    @PostMapping("/resources/group/rename")
    public String renameResourceGroup(@RequestParam("groupId") String groupId,
                                      @RequestParam("humanReadableId") String humanReadableId,
                                      Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        return renameCatalogItem("group", groupId, humanReadableId, model, request, response);
    }

    @PostMapping("/batch-delete")
    public String batchDelete(@RequestParam(value = "kind", required = false) List<String> kinds,
            @RequestParam(value = "id", required = false) List<String> ids,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        String refused = catalogBatchDelete.deleteAll(kinds, ids, accessor);
        redirectResources(request, response, refused);
        return null;
    }

    @PostMapping("/catalog/rename")
    public String renameCatalogItem(@RequestParam("kind") String kind,
            @RequestParam("id") String id,
            @RequestParam("name") String name,
            Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        String type = kind == null ? "" : kind.trim().toLowerCase();
        String next = name == null ? "" : name.trim();
        if (next.isEmpty()) {
            redirectResources(request, response, "invalid_name");
            return null;
        }
        String refused = switch (type) {
            case "group" -> renameGroup(id, next);
            case "resource" -> renameResource(id, next);
            case "page", "album" -> renameItemId(id, next);
            case "playlist" -> renamePlaylistTitle(id, next);
            case "url-template" -> renameUrlTemplateId(id, next);
            case "category" -> renameCategoryName(id, next);
            default -> "invalid_item";
        };
        redirectResources(request, response, refused);
        return null;
    }

    private String renameGroup(String groupId, String humanReadableId) {
        ObjectId groupObjId = parseObjectId(groupId);
        if (groupObjId == null) {
            return "invalid_group";
        }
        try {
            List<ResourceGroupEntity> all = allGroups();
            ResourceGroupEntity group = ResourceGroupTree.find(all, groupObjId);
            String refused = ResourceGroupTree.refuseRename(all, group, humanReadableId);
            if (refused != null) {
                return refused;
            }
            if (group.getResourceGroupData() == null) {
                group.setResourceGroupData(new ResourceGroupData());
            }
            group.getResourceGroupData().setHumanReadableId(humanReadableId);
            serviceProvider.getResourceGroupService().replace(group);
            resourceGroupIndex.structureChanged();
            return null;
        } catch (Exception e) {
            LOGGER.error("Failed to rename resource group: " + e.getMessage(), e);
            return "invalid_group";
        }
    }

    private String renameResource(String resourceId, String fileName) {
        ObjectId id = parseObjectId(resourceId);
        if (id == null) {
            return "invalid_item";
        }
        ResourceEntity existing = (ResourceEntity) serviceProvider.getResourceService()
                .getById(ResourceEntity.class, id);
        if (existing == null || existing.getResourceData() == null) {
            return "invalid_item";
        }
        ResourceData data = existing.getResourceData();
        String nextPath = ResourceFileNames.nextPathPublic(data.getPathPublic(), fileName, data.getType());
        if (nextPath == null) {
            return "invalid_name";
        }
        if (nextPath.equals(data.getPathPublic())) {
            return null;
        }
        ResourceEntity clash = serviceProvider.getResourceService().getByPublicPath(nextPath);
        if (clash != null && !id.equals(clash.getId())) {
            return "name_taken";
        }
        String oldPath = data.getPathPublic();
        data.setPathPublic(nextPath);
        serviceProvider.getResourceService().replace(existing);
        contentCacheController.invalidateCacheForResource(oldPath);
        resourceGroupIndex.fileRenamed(existing);
        return null;
    }

    private String renameItemId(String uniqueUriName, String nextName) {
        ItemEntity item = serviceProvider.getItemService().getByName(uniqueUriName);
        if (item == null) {
            return "invalid_item";
        }
        String error = EditorPagesController.assignItemId(serviceProvider.getItemService(), item, nextName);
        if (error == null) {
            serviceProvider.getItemService().replace(item);
            resourceGroupIndex.contentChanged();
            return null;
        }
        return "An item with this ID already exists".equals(error) ? "name_taken" : "invalid_name";
    }

    private String renamePlaylistTitle(String playlistId, String title) {
        ObjectId id = parseObjectId(playlistId);
        if (id == null) {
            return "invalid_item";
        }
        PlaylistEntity playlist = (PlaylistEntity) serviceProvider.getPlaylistService()
                .getById(PlaylistEntity.class, id);
        if (playlist == null) {
            return "invalid_item";
        }
        if (playlist.getPlaylistData() == null) {
            playlist.setPlaylistData(new PlaylistData());
        }
        playlist.getPlaylistData().setTitle(title);
        serviceProvider.getPlaylistService().replace(playlist);
        resourceGroupIndex.contentChanged();
        return null;
    }

    private String renameUrlTemplateId(String templateId, String name) {
        ObjectId id = parseObjectId(templateId);
        if (id == null) {
            return "invalid_item";
        }
        String normalized = UrlTemplateIds.normalize(name);
        if (!UrlTemplateIds.isValid(normalized)) {
            return "invalid_name";
        }
        UrlTemplateEntity template = (UrlTemplateEntity) serviceProvider.getUrlTemplateService()
                .getById(UrlTemplateEntity.class, id);
        if (template == null) {
            return "invalid_item";
        }
        UrlTemplateEntity clash = serviceProvider.getUrlTemplateService().getByUrlTemplateId(normalized);
        if (clash != null && !clash.getId().equals(template.getId())) {
            return "invalid_name";
        }
        template.setUrlTemplateId(normalized);
        serviceProvider.getUrlTemplateService().replace(template);
        resourceGroupIndex.contentChanged();
        return null;
    }

    private String renameCategoryName(String categoryId, String itemName) {
        ObjectId id = parseObjectId(categoryId);
        if (id == null) {
            return "invalid_item";
        }
        CategoryEntity category = (CategoryEntity) serviceProvider.getCategoryService()
                .getById(CategoryEntity.class, id);
        if (category == null) {
            return "invalid_item";
        }
        if (category.getCategoryData() == null) {
            category.setCategoryData(new CategoryData());
        }
        category.getCategoryData().setItemName(itemName);
        serviceProvider.getCategoryService().replace(category);
        resourceGroupIndex.contentChanged();
        return null;
    }

    private String loadResourcesWithError(Model model, AccountEntity accessor, HttpServletRequest request) {
        fillResourcesPage(model, accessor, null, returnGroupId(request));
        return "/admin/editor-resources";
    }

    private void fillResourcesPage(Model model, AccountEntity accessor, String errorCode, String groupId) {
        ResourceGroupDisplayDTO emptyHome = new ResourceGroupDisplayDTO();
        emptyHome.setHumanReadableId(ResourceGroupTree.DEFAULT_NAME);
        emptyHome.setPathLabel(ResourceGroupTree.DEFAULT_NAME);
        emptyHome.setDefaultGroup(true);
        model.addAttribute("resourceGroupTree", List.of());
        model.addAttribute("assignableGroups", List.of());
        model.addAttribute("selectedGroup", emptyHome);
        model.addAttribute("defaultGroupId", "");
        model.addAttribute("selectedLeafId", null);
        try {
            ResourceGroupTreeView.Assembled tree = resourceGroupIndex.view();
            List<ResourceGroupDisplayDTO> chrome = resourceGroupIndex.editorRoots();
            String homeId = ResourceGroupTreeView.homeGroupId(tree);
            model.addAttribute("resourceGroupTree", chrome);
            model.addAttribute("assignableGroups", tree.assignableGroups());
            model.addAttribute("defaultGroupId", homeId == null ? "" : homeId);
            String selectedId = groupId == null ? "" : groupId.trim();
            if (EditorTree.browseInResources(selectedId)) {
                ResourceGroupDisplayDTO browsed = EditorTree.find(chrome, selectedId);
                model.addAttribute("selectedGroup", browsed != null ? browsed : emptyHome);
                if (browsed != null) {
                    attachPaneEntries(model, selectedId);
                }
            } else {
                ResourceGroupDisplayDTO selectedMeta = ResourceGroupTreeView.find(tree, groupId);
                if (selectedMeta != null && selectedMeta.isUngrouped()) {
                    selectedMeta = ResourceGroupTreeView.homeGroup(tree);
                }
                if (selectedMeta == null || selectedMeta.isUngrouped()) {
                    selectedMeta = emptyHome;
                }
                model.addAttribute("selectedGroup", ResourceGroupTreeView.withResources(
                        selectedMeta, resourceGroupIndex.filesFor(selectedMeta)));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load resources: " + e.getMessage(), e);
        }
        stampFolderAccess(model, accessor);
        addAccessLookups(model);
        model.addAttribute("accessor", accessor);
        if (!model.containsAttribute("error")) {
            String message = ResourceGroupTree.errorMessage(errorCode);
            if (message != null) {
                model.addAttribute("error", message);
            }
        }
        model.addAttribute("username", accessor.getAccountData().getLogin());
    }

    private void stampFolderAccess(Model model, AccountEntity accessor) {
        Object selected = model.getAttribute("selectedGroup");
        if (!(selected instanceof ResourceGroupDisplayDTO group) || !group.canEditAccess()) {
            return;
        }
        ResourceGroupEntity live = loadGroup(group.getId());
        if (live == null) {
            return;
        }
        if (live.getSecurityData() != null) {
            group.setSecurityData(live.getSecurityData());
        }
        group.setAccessCanEdit(EntityAccess.isAccessible(live, AccessType.ACCESS_EDIT, accessor));
    }

    private void attachPaneEntries(Model model, String selectedId) {
        if (EditorTree.APPS_ID.equals(selectedId)) {
            model.addAttribute("paneApps", resourceGroupIndex.apps());
        } else if (EditorTree.THEMES_ID.equals(selectedId)) {
            model.addAttribute("paneThemes", resourceGroupIndex.themes());
        } else if (EditorTree.PLAYLISTS_ID.equals(selectedId)) {
            model.addAttribute("panePlaylists", resourceGroupIndex.playlists());
        } else if (EditorTree.URL_TEMPLATES_ID.equals(selectedId)) {
            model.addAttribute("paneUrlTemplates", resourceGroupIndex.urlTemplates());
        } else if (EditorTree.CATEGORIES_ID.equals(selectedId)) {
            model.addAttribute("paneCategories", resourceGroupIndex.categories());
        } else if (selectedId.startsWith(EditorTree.CATEGORY_PREFIX)) {
            model.addAttribute("panePages", resourceGroupIndex.itemsInCategory(
                    selectedId.substring(EditorTree.CATEGORY_PREFIX.length())));
        }
    }

    private List<ResourceGroupEntity> allGroups() {
        List<ResourceGroupEntity> groups = serviceProvider.getResourceGroupService().getAllGroups();
        return groups == null ? List.of() : groups;
    }

    private ResourceGroupEntity loadGroup(String groupId) {
        ObjectId id = parseObjectId(groupId);
        if (id == null) {
            return null;
        }
        try {
            return (ResourceGroupEntity) serviceProvider.getResourceGroupService()
                    .getById(ResourceGroupEntity.class, id);
        } catch (Exception e) {
            LOGGER.warn("Invalid resource group ID: " + groupId);
            return null;
        }
    }

    private void ensureUnsortedName() {
        try {
            List<ResourceGroupEntity> all = allGroups();
            boolean hasUnsorted = false;
            ResourceGroupEntity legacy = null;
            for (ResourceGroupEntity group : all) {
                String name = ResourceGroupTree.nameOf(group);
                if (ResourceGroupTree.DEFAULT_NAME.equalsIgnoreCase(name)) {
                    hasUnsorted = true;
                } else if (ResourceGroupTree.LEGACY_DEFAULT_NAME.equalsIgnoreCase(name)) {
                    legacy = group;
                }
            }
            if (legacy == null || hasUnsorted) {
                return;
            }
            if (legacy.getResourceGroupData() == null) {
                legacy.setResourceGroupData(new ResourceGroupData());
            }
            legacy.getResourceGroupData().setHumanReadableId(ResourceGroupTree.DEFAULT_NAME);
            serviceProvider.getResourceGroupService().replace(legacy);
            resourceGroupIndex.structureChanged();
        } catch (Exception e) {
            LOGGER.warn("Could not rename Default group to Unsorted: {}", e.getMessage());
        }
    }

    private ResourceGroupEntity loadDefaultGroup() {
        for (ResourceGroupEntity group : allGroups()) {
            if (ResourceGroupTree.isDefault(group)) {
                return group;
            }
        }
        return null;
    }

    private String homeGroupId() {
        try {
            String id = ResourceGroupTreeView.homeGroupId(resourceGroupIndex.view());
            return id == null ? "" : id;
        } catch (Exception e) {
            return "";
        }
    }

    private void redirectResources(HttpServletRequest request, HttpServletResponse response, String errorCode)
            throws IOException {
        redirectResources(request, response, errorCode, returnGroupId(request));
    }

    private void redirectResources(HttpServletRequest request, HttpServletResponse response,
            String errorCode, String groupId) throws IOException {
        String group = ResourceGroupTreeView.isHomeAlias(groupId) ? homeGroupId() : groupId.trim();
        String url = request.getContextPath() + "/editor/resources";
        boolean hasGroup = group != null && !group.isBlank();
        if (hasGroup) {
            url += "?group=" + java.net.URLEncoder.encode(group, java.nio.charset.StandardCharsets.UTF_8);
        }
        if (errorCode != null && !errorCode.isBlank()) {
            url += (hasGroup ? "&" : "?") + "error="
                    + java.net.URLEncoder.encode(errorCode, java.nio.charset.StandardCharsets.UTF_8);
        }
        response.sendRedirect(url);
    }

    private String returnGroupId(HttpServletRequest request) {
        String fromForm = request.getParameter("returnGroup");
        if (fromForm != null && !fromForm.isBlank()) {
            return fromForm.trim();
        }
        String fromQuery = request.getParameter("group");
        if (fromQuery != null && !fromQuery.isBlank()) {
            return fromQuery.trim();
        }
        return homeGroupId();
    }

    private static ObjectId parseObjectId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new ObjectId(raw.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static List<String> uniqueIds(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<String> ids = new java.util.ArrayList<>();
        for (String value : raw) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (trimmed.isEmpty() || ids.contains(trimmed)) {
                continue;
            }
            ids.add(trimmed);
        }
        return ids;
    }

    private String generateProtectedPrefix() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        java.util.Random random = new java.util.Random();
        int numSlashes = 3 + random.nextInt(2);
        StringBuilder prefix = new StringBuilder();
        int position = 0;
        while (position < 32) {
            if (prefix.length() > 0 && prefix.charAt(prefix.length() - 1) != '/' && numSlashes > 0 && random.nextInt(10) < 2) {
                prefix.append('/');
                numSlashes--;
            } else {
                prefix.append(chars.charAt(random.nextInt(chars.length())));
                position++;
            }
        }
        while (numSlashes > 0) {
            int idx = 1 + random.nextInt(prefix.length() - 2);
            if (prefix.charAt(idx) != '/' && prefix.charAt(idx - 1) != '/' && prefix.charAt(idx + 1) != '/') {
                prefix.setCharAt(idx, '/');
                numSlashes--;
            }
        }
        return prefix.toString();
    }

    private ResourceData buildImagePreview(ResourceData original, byte[] content, String resourceId,
            String originalExtension, String userId, ImageUploadOptions options) {
        try {
            var preview = JpegImages.scaledPreview(content, options.previewMaxWidth(), options.qualityFactor());
            if (preview.isEmpty()) {
                return null;
            }
            JpegImages.Encoded jpeg = preview.get();
            String previewPublic = String.format("/%s/res/%s/%s.%s.low-res.jpg",
                    userId, original.getType().getPath(), resourceId, originalExtension);
            if (serviceProvider.getResourceService().getByPublicPath(previewPublic) != null) {
                LOGGER.warn("Preview public path already exists, skipping preview: " + previewPublic);
                return null;
            }
            ResourceData previewData = new ResourceData();
            previewData.setType(IMAGE);
            previewData.setPathPublic(previewPublic);
            previewData.setPathProtected(generateUniqueProtectedPath(resourceId + "." + originalExtension + ".low-res", "jpg"));
            previewData.addMetadata("width", String.valueOf(jpeg.width()));
            previewData.addMetadata("height", String.valueOf(jpeg.height()));
            fillResourceContent(previewData, jpeg.bytes());
            return previewData;
        } catch (Exception e) {
            LOGGER.warn("Could not create image preview: " + e.getMessage(), e);
            return null;
        }
    }

    private ResourceData buildPdfPreview(ResourceData original, byte[] content, String resourceId,
            String originalExtension, String userId, ImageUploadOptions options) {
        try {
            JpegImages.Encoded jpeg = PdfPreviews.firstPage(content, options.previewMaxWidth(),
                    options.qualityFactor());
            String previewPublic = String.format("/%s/res/%s/%s.%s.low-res.jpg",
                    userId, original.getType().getPath(), resourceId, originalExtension);
            if (serviceProvider.getResourceService().getByPublicPath(previewPublic) != null) {
                LOGGER.warn("Preview public path already exists, skipping preview: " + previewPublic);
                return null;
            }
            ResourceData previewData = new ResourceData();
            previewData.setType(IMAGE);
            previewData.setPathPublic(previewPublic);
            previewData.setPathProtected(generateUniqueProtectedPath(
                    resourceId + "." + originalExtension + ".low-res", "jpg"));
            previewData.addMetadata("width", String.valueOf(jpeg.width()));
            previewData.addMetadata("height", String.valueOf(jpeg.height()));
            fillResourceContent(previewData, jpeg.bytes());
            return previewData;
        } catch (Exception e) {
            LOGGER.warn("Could not create PDF preview: " + e.getMessage(), e);
            return null;
        }
    }

    private ResourceData buildAudioCoverPreview(byte[] artwork, String resourceId, String userId,
            ImageUploadOptions options) {
        try {
            JpegImages.Encoded jpeg = JpegImages.toJpeg(artwork, options.previewMaxWidth(),
                    options.qualityFactor());
            String previewPublic = String.format("/%s/res/%s/%s.cover.low-res.jpg",
                    userId, ResourceType.AUDIO.getPath(), resourceId);
            if (serviceProvider.getResourceService().getByPublicPath(previewPublic) != null) {
                LOGGER.warn("Cover public path already exists, skipping preview: " + previewPublic);
                return null;
            }
            ResourceData previewData = new ResourceData();
            previewData.setType(IMAGE);
            previewData.setPathPublic(previewPublic);
            previewData.setPathProtected(generateUniqueProtectedPath(resourceId + ".cover.low-res", "jpg"));
            previewData.addMetadata("width", String.valueOf(jpeg.width()));
            previewData.addMetadata("height", String.valueOf(jpeg.height()));
            fillResourceContent(previewData, jpeg.bytes());
            return previewData;
        } catch (Exception e) {
            LOGGER.warn("Could not create MP3 cover preview: " + e.getMessage(), e);
            return null;
        }
    }

    private void fillResourceContent(ResourceData resourceData, byte[] content) {
        resourceData.setSizeInBytes(content.length);
        String base64Content = java.util.Base64.getEncoder().encodeToString(content);
        if (base64Content.length() > 7000000) {
            resourceData.setLargeFile(true);
            int chunkSize = 7000000;
            int totalChunks = (int) Math.ceil((double) base64Content.length() / chunkSize);
            for (int i = 0; i < totalChunks; i++) {
                int start = i * chunkSize;
                int end = Math.min(start + chunkSize, base64Content.length());
                String chunkData = base64Content.substring(start, end);
                DataChunkEntity chunk = new DataChunkEntity(chunkData);
                serviceProvider.getResourceService().saveDataChunk(chunk);
                resourceData.addDataChunkId(chunk.getId());
            }
            resourceData.setContentRaw(null);
        } else {
            resourceData.setLargeFile(false);
            resourceData.setContentRaw(base64Content);
        }
    }

    private String generateUniqueProtectedPath(String resourceId, String extension) {
        String pathProtected = null;
        int maxAttempts = 10;
        for (int i = 0; i < maxAttempts; i++) {
            pathProtected = "/" + generateProtectedPrefix() + "/" + resourceId + "." + extension;
            ResourceEntity existing = serviceProvider.getResourceService().getByProtectedPath(pathProtected);
            if (existing == null) {
                return pathProtected;
            }
        }
        LOGGER.error("Failed to generate unique protected path after " + maxAttempts + " attempts");
        return pathProtected;
    }
}

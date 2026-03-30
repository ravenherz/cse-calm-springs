package com.ravenherz.rhzwe.controller;

import com.ravenherz.rhzwe.constants.Strings;
import com.ravenherz.rhzwe.dal.EntityUtils;
import com.ravenherz.rhzwe.dal.dto.AccountEntity;
import com.ravenherz.rhzwe.dal.dto.BasicEntity;
import com.ravenherz.rhzwe.dal.dto.ItemEntity;
import com.ravenherz.rhzwe.dal.dto.ResourceEntity;
import com.ravenherz.rhzwe.dal.dto.basic.PageData;
import com.ravenherz.rhzwe.dal.dto.basic.ResourceData;
import com.ravenherz.rhzwe.dal.dto.basic.enums.AccessType;
import com.ravenherz.rhzwe.dal.dto.basic.enums.EventType;
import com.ravenherz.rhzwe.dal.dto.basic.enums.ResourceType;
import com.ravenherz.rhzwe.dal.dto.basic.HistoryData;
import com.ravenherz.rhzwe.dal.dto.basic.Event;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/editor")
public class EditorController extends AbstractController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditorController.class);

    @GetMapping
    public String listPages(Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            AccountEntity accessor = getAccessor(request, response);
            if (accessor == null) {
                response.sendRedirect(request.getContextPath() + "/?error=401");
                return null;
            }

            List<ItemEntity> editableItems = new ArrayList<>();
            try {
                List<? extends BasicEntity> allEntities = serviceProvider.getItemService().getAll();
                if (allEntities != null) {
                    editableItems = allEntities.stream()
                            .filter(item -> item instanceof ItemEntity)
                            .map(item -> (ItemEntity) item)
                            .filter(item -> {
                                try {
                                    return EntityUtils.isAccessible(item, AccessType.ACCESS_READ, accessor);
                                } catch (Exception e) {
                                    LOGGER.warn("Access check failed for item: " + e.getMessage());
                                    return false;
                                }
                            })
                            .collect(Collectors.toList());
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load items: " + e.getMessage(), e);
            }

            model.addAttribute("items", editableItems);
            model.addAttribute("username", accessor.getAccountData().getLogin());
            return "/2000s/editor-list";
        } catch (Exception e) {
            LOGGER.error("Editor list error: " + e.getMessage(), e);
            response.sendRedirect(request.getContextPath() + "/?error=500");
            return null;
        }
    }

    @GetMapping("/edit")
    public String editPage(@RequestParam(value = "name", required = false) String name,
                            Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            response.sendRedirect(request.getContextPath() + "/?error=401");
            return null;
        }

        if (name == null || name.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/editor");
            return null;
        }

        ItemEntity item = serviceProvider.getItemService().getByName(name);
        if (item == null) {
            response.sendRedirect(request.getContextPath() + "/?error=404");
            return null;
        }

        if (!EntityUtils.isAccessible(item, AccessType.ACCESS_READ, accessor)) {
            response.sendRedirect(request.getContextPath() + "/?error=403");
            return null;
        }

        model.addAttribute("item", item);
        model.addAttribute("username", accessor.getAccountData().getLogin());

        String theme = settings.getValue(Strings.CONTEXT_DATASOURCE_VIEW, Strings.KEY_STYLES_THEME);
        String schema = settings.getValue(Strings.CONTEXT_DATASOURCE_VIEW, Strings.KEY_STYLES_SCHEMA);
        model.addAttribute("stylesTheme", theme);
        model.addAttribute("stylesSchema", schema);

        return "/2000s/editor-edit";
    }

    @PostMapping("/save")
    public String savePage(@RequestParam(value = "name", required = false) String name,
                           @RequestParam(value = "title", required = false) String title,
                           @RequestParam(value = "header", required = false) String header,
                           @RequestParam(value = "subHeader", required = false) String subHeader,
                           @RequestParam(value = "description", required = false) String description,
                           @RequestParam(value = "tags", required = false) String tags,
                           Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            response.sendRedirect(request.getContextPath() + "/?error=401");
            return null;
        }

        if (name == null || name.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/editor");
            return null;
        }

        ItemEntity item = serviceProvider.getItemService().getByName(name);
        if (item == null) {
            response.sendRedirect(request.getContextPath() + "/?error=404");
            return null;
        }

        if (!EntityUtils.isAccessible(item, AccessType.ACCESS_READ, accessor)) {
            response.sendRedirect(request.getContextPath() + "/?error=403");
            return null;
        }

        if (item.getPageData() == null) {
            item.setPageData(new PageData());
        }

        PageData pageData = item.getPageData();

        if (title != null) pageData.setTitle(title);
        if (header != null) pageData.setHeader(header);
        if (subHeader != null) pageData.setSubHeader(subHeader);
        if (description != null) pageData.setDescription(description);
        if (tags != null) {
            List<String> tagList = Arrays.stream(tags.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            pageData.setTags(tagList);
        }

        HistoryData historyData = item.getHistoryData();
        if (historyData == null) {
            historyData = new HistoryData();
        }
        Event[] oldEvents = historyData.getEvents();
        Event[] newEvents = new Event[oldEvents.length + 1];
        System.arraycopy(oldEvents, 0, newEvents, 0, oldEvents.length);
        newEvents[oldEvents.length] = new Event(EventType.ENTITY_EDITED, LocalDateTime.now(), accessor);
        historyData.setEvents(newEvents);
        item.setHistoryData(historyData);

        serviceProvider.getItemService().replace(item);

        response.sendRedirect(request.getContextPath() + "/editor/edit?name=" + name);
        return null;
    }

    @GetMapping("/resources")
    public String resourcesPage(Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            response.sendRedirect(request.getContextPath() + "/?error=401");
            return null;
        }

        try {
            List<BasicEntity> allResources = serviceProvider.getResourceService().getAll();
            if (allResources != null) {
                List<ResourceEntity> resources = allResources.stream()
                        .filter(r -> r instanceof ResourceEntity)
                        .map(r -> (ResourceEntity) r)
                        .collect(Collectors.toList());
                model.addAttribute("resources", resources);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load resources: " + e.getMessage(), e);
        }

        model.addAttribute("username", accessor.getAccountData().getLogin());
        return "/2000s/editor-resources";
    }

    @PostMapping("/resources/upload")
    public String uploadResource(@RequestParam("resourceId") String resourceId,
                                 @RequestParam("file") MultipartFile file,
                                 Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            response.sendRedirect(request.getContextPath() + "/?error=401");
            return null;
        }

        if (resourceId == null || resourceId.trim().isEmpty()) {
            model.addAttribute("error", "Resource ID is required");
            return loadResourcesWithError(model, accessor);
        }

        if (file == null || file.isEmpty()) {
            model.addAttribute("error", "File is required");
            return loadResourcesWithError(model, accessor);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            model.addAttribute("error", "File must have an extension");
            return loadResourcesWithError(model, accessor);
        }

        ResourceType resourceType = ResourceType.getByFileName(originalFilename);
        if (resourceType == ResourceType.INVALID) {
            model.addAttribute("error", "Invalid file type. Supported: jpg, png, mp3");
            return loadResourcesWithError(model, accessor);
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();

        String userId = accessor.getAccountData().getLogin();
        String pathPublic = String.format("/%s/res/%s/%s.%s", userId, resourceType.getPath(), resourceId.trim(), extension);

        ResourceEntity existing = serviceProvider.getResourceService().getByPublicPath(pathPublic);
        if (existing != null) {
            model.addAttribute("error", "Resource with this ID already exists");
            return loadResourcesWithError(model, accessor);
        }

        byte[] content = file.getBytes();
        long sizeInBytes = content.length;

        String pathProtected = generateUniqueProtectedPath(resourceId.trim(), extension);

        ResourceData resourceData = new ResourceData();
        resourceData.setType(resourceType);
        resourceData.setSizeInBytes(sizeInBytes);
        resourceData.setPathPublic(pathPublic);
        resourceData.setPathProtected(pathProtected);
        resourceData.setContentRaw(java.util.Base64.getEncoder().encodeToString(content));

        ResourceEntity resourceEntity = new ResourceEntity(resourceData, accessor);
        serviceProvider.getResourceService().insert(resourceEntity);

        response.sendRedirect(request.getContextPath() + "/editor/resources");
        return null;
    }

    @PostMapping("/resources/delete")
    public String deleteResource(@RequestParam("pathPublic") String pathPublic,
                                 Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            response.sendRedirect(request.getContextPath() + "/?error=401");
            return null;
        }

        if (pathPublic == null || pathPublic.trim().isEmpty()) {
            model.addAttribute("error", "Resource path is required");
            return loadResourcesWithError(model, accessor);
        }

        ResourceEntity existing = serviceProvider.getResourceService().getByPublicPath(pathPublic);
        if (existing == null) {
            model.addAttribute("error", "Resource not found");
            return loadResourcesWithError(model, accessor);
        }

        LOGGER.info("Deleting resource: " + pathPublic + " with id: " + existing.getId());
        try {
            serviceProvider.getResourceService().deleteByPublicPath(pathPublic);
        } catch (Exception e) {
            LOGGER.error("Failed to delete resource: " + e.getMessage(), e);
            model.addAttribute("error", "Failed to delete resource");
            return loadResourcesWithError(model, accessor);
        }

        response.sendRedirect(request.getContextPath() + "/editor/resources");
        return null;
    }

    private String loadResourcesWithError(Model model, AccountEntity accessor) {
        try {
            List<BasicEntity> allResources = serviceProvider.getResourceService().getAll();
            if (allResources != null) {
                List<ResourceEntity> resources = allResources.stream()
                        .filter(r -> r instanceof ResourceEntity)
                        .map(r -> (ResourceEntity) r)
                        .collect(Collectors.toList());
                model.addAttribute("resources", resources);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load resources: " + e.getMessage(), e);
        }
        model.addAttribute("username", accessor.getAccountData().getLogin());
        return "/2000s/editor-resources";
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

package com.ravenherz.rhzwe.controller;

import com.ravenherz.rhzwe.constants.Strings;
import com.ravenherz.rhzwe.dal.EntityUtils;
import com.ravenherz.rhzwe.dal.dto.AccountEntity;
import com.ravenherz.rhzwe.dal.dto.BasicEntity;
import com.ravenherz.rhzwe.dal.dto.DataChunkEntity;
import com.ravenherz.rhzwe.dal.dto.ItemEntity;
import com.ravenherz.rhzwe.dal.dto.CategoryEntity;
import com.ravenherz.rhzwe.dal.dto.ResourceEntity;
import com.ravenherz.rhzwe.dal.dto.basic.CategoryData;
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
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import static com.ravenherz.rhzwe.dal.dto.basic.enums.ResourceType.IMAGE;

@Controller
@RequestMapping("/editor")
public class EditorController extends AbstractController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditorController.class);

    @GetMapping
    public String listPagesRedirect(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(request.getContextPath() + "/editor/pages");
        return null;
    }

    @GetMapping("/pages")
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
            return "/2000s/editor-pages-list";
        } catch (Exception e) {
            LOGGER.error("Editor list error: " + e.getMessage(), e);
            response.sendRedirect(request.getContextPath() + "/?error=500");
            return null;
        }
    }

    @GetMapping("/create")
    public String createPage(Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            response.sendRedirect(request.getContextPath() + "/?error=401");
            return null;
        }

        List<CategoryEntity> categories = serviceProvider.getCategoryService().getAllCategories();
        model.addAttribute("categories", categories);
        model.addAttribute("username", accessor.getAccountData().getLogin());

        String theme = settings.getValue(Strings.CONTEXT_DATASOURCE_VIEW, Strings.KEY_STYLES_THEME);
        String schema = settings.getValue(Strings.CONTEXT_DATASOURCE_VIEW, Strings.KEY_STYLES_SCHEMA);
        model.addAttribute("stylesTheme", theme);
        model.addAttribute("stylesSchema", schema);

        return "/2000s/editor-page-create";
    }

    @GetMapping("/data")
    @ResponseBody
    public String getEditorData(HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }

        List<CategoryEntity> categories = serviceProvider.getCategoryService().getAllCategories();
        
        List<BasicEntity> allResources = serviceProvider.getResourceService().getAll();
        List<ResourceEntity> images = allResources.stream()
                .filter(r -> r instanceof ResourceEntity)
                .map(r -> (ResourceEntity) r)
                .filter(r -> r.getResourceData().getType() == IMAGE)
                .collect(Collectors.toList());

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"categories\":[");
        if (categories != null) {
            for (int i = 0; i < categories.size(); i++) {
                CategoryEntity cat = categories.get(i);
                if (i > 0) json.append(",");
                json.append("{\"id\":\"").append(cat.getId()).append("\",\"name\":\"");
                String itemName = cat.getCategoryData().getItemName();
                json.append(itemName != null ? itemName.replace("\"", "\\\"") : "").append("\"}");
            }
        }
        json.append("],");
        json.append("\"images\":[");
        if (images != null) {
            for (int i = 0; i < images.size(); i++) {
                ResourceEntity img = images.get(i);
                if (i > 0) json.append(",");
                json.append("{\"id\":\"").append(img.getId()).append("\",\"name\":\"");
                String name = img.getResourceData().getPathPublic();
                if (name != null) {
                    name = name.substring(name.lastIndexOf("/") + 1);
                }
                json.append(name != null ? name.replace("\"", "\\\"") : "").append("\"}");
            }
        }
        json.append("]");
        json.append("}");
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        return json.toString();
    }

    @PostMapping("/create")
    public String createPageSubmit(@RequestParam(value = "name", required = false) String name,
                                   @RequestParam(value = "title", required = false) String title,
                                   @RequestParam(value = "header", required = false) String header,
                                   @RequestParam(value = "subHeader", required = false) String subHeader,
                                   @RequestParam(value = "description", required = false) String description,
                                   @RequestParam(value = "tags", required = false) String tags,
                                   @RequestParam(value = "categoryId", required = false) String categoryId,
                                   Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            response.sendRedirect(request.getContextPath() + "/?error=401");
            return null;
        }

        if (name == null || name.trim().isEmpty()) {
            model.addAttribute("error", "Page ID is required");
            return loadCreatePageWithError(model, accessor, categoryId);
        }

        ItemEntity existing = serviceProvider.getItemService().getByName(name);
        if (existing != null) {
            model.addAttribute("error", "A page with this ID already exists");
            return loadCreatePageWithError(model, accessor, categoryId);
        }

        PageData pageData = new PageData();
        pageData.setTitle(title != null ? title : "");
        pageData.setHeader(header != null ? header : "");
        pageData.setSubHeader(subHeader != null ? subHeader : "");
        pageData.setDescription(description != null ? description : "");
        if (tags != null && !tags.trim().isEmpty()) {
            List<String> tagList = Arrays.stream(tags.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            pageData.setTags(tagList);
        }

        ItemEntity newItem = new ItemEntity(name.trim(), pageData, accessor);
        newItem.setItemType(ItemEntity.ItemType.PAGE);

        if (categoryId != null && !categoryId.trim().isEmpty()) {
            try {
                org.bson.types.ObjectId catObjId = new org.bson.types.ObjectId(categoryId.trim());
                CategoryEntity category = (CategoryEntity) serviceProvider.getCategoryService().getById(CategoryEntity.class, catObjId);
                newItem.setRefCategory(category);
            } catch (Exception e) {
                LOGGER.warn("Invalid category ID: " + categoryId);
            }
        }

        newItem.setPageData(pageData);

        try {
            serviceProvider.getItemService().insert(newItem);
        } catch (Exception e) {
            LOGGER.error("Failed to create page: " + e.getMessage(), e);
            model.addAttribute("error", "Failed to create page: " + e.getMessage());
            return loadCreatePageWithError(model, accessor, categoryId);
        }

        response.sendRedirect(request.getContextPath() + "/editor/edit?name=" + name);
        return null;
    }

    private String loadCreatePageWithError(Model model, AccountEntity accessor, String categoryId) {
        List<CategoryEntity> categories = serviceProvider.getCategoryService().getAllCategories();
        model.addAttribute("categories", categories);
        model.addAttribute("username", accessor.getAccountData().getLogin());

        String theme = settings.getValue(Strings.CONTEXT_DATASOURCE_VIEW, Strings.KEY_STYLES_THEME);
        String schema = settings.getValue(Strings.CONTEXT_DATASOURCE_VIEW, Strings.KEY_STYLES_SCHEMA);
        model.addAttribute("stylesTheme", theme);
        model.addAttribute("stylesSchema", schema);

        return "/2000s/editor-page-create";
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

        List<CategoryEntity> categories = serviceProvider.getCategoryService().getAllCategories();
        model.addAttribute("categories", categories);

        List<BasicEntity> allResources = serviceProvider.getResourceService().getAll();
        List<ResourceEntity> images = allResources.stream()
                .filter(r -> r instanceof ResourceEntity)
                .map(r -> (ResourceEntity) r)
                .filter(r -> r.getResourceData().getType() == IMAGE)
                .collect(Collectors.toList());
        model.addAttribute("images", images);

        String theme = settings.getValue(Strings.CONTEXT_DATASOURCE_VIEW, Strings.KEY_STYLES_THEME);
        String schema = settings.getValue(Strings.CONTEXT_DATASOURCE_VIEW, Strings.KEY_STYLES_SCHEMA);
        model.addAttribute("stylesTheme", theme);
        model.addAttribute("stylesSchema", schema);

        return "/2000s/editor-page-edit";
    }

    @PostMapping("/save")
    public String savePage(@RequestParam(value = "name", required = false) String name,
                           @RequestParam(value = "title", required = false) String title,
                           @RequestParam(value = "header", required = false) String header,
                           @RequestParam(value = "subHeader", required = false) String subHeader,
                           @RequestParam(value = "description", required = false) String description,
                           @RequestParam(value = "tags", required = false) String tags,
                           @RequestParam(value = "categoryId", required = false) String categoryId,
                           @RequestParam(value = "imageId", required = false) String imageId,
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

        if (categoryId != null && !categoryId.trim().isEmpty()) {
            try {
                org.bson.types.ObjectId catObjId = new org.bson.types.ObjectId(categoryId.trim());
                CategoryEntity category = (CategoryEntity) serviceProvider.getCategoryService().getById(CategoryEntity.class, catObjId);
                item.setRefCategory(category);
            } catch (Exception e) {
                LOGGER.warn("Invalid category ID: " + categoryId);
            }
        } else {
            item.setRefCategory(null);
        }

        if (imageId != null && !imageId.trim().isEmpty()) {
            try {
                org.bson.types.ObjectId imgObjId = new org.bson.types.ObjectId(imageId.trim());
                ResourceEntity image = (ResourceEntity) serviceProvider.getResourceService().getById(ResourceEntity.class, imgObjId);
                pageData.setRefImage(image);
            } catch (Exception e) {
                LOGGER.warn("Invalid image ID: " + imageId);
            }
        } else {
            pageData.setRefImage(null);
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

    @PostMapping("/delete")
    public String deletePage(@RequestParam(value = "name", required = false) String name,
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

        try {
            serviceProvider.getItemService().delete(item);
        } catch (Exception e) {
            LOGGER.error("Failed to delete page: " + e.getMessage(), e);
        }

        response.sendRedirect(request.getContextPath() + "/editor");
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
                                 @RequestParam(value = "metadata", required = false) String metadataJson,
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
        String base64Content = java.util.Base64.getEncoder().encodeToString(content);

        String pathProtected = generateUniqueProtectedPath(resourceId.trim(), extension);

        ResourceData resourceData = new ResourceData();
        resourceData.setType(resourceType);
        resourceData.setSizeInBytes(sizeInBytes);
        resourceData.setPathPublic(pathPublic);
        resourceData.setPathProtected(pathProtected);

        if (metadataJson != null && !metadataJson.trim().isEmpty()) {
            try {
                Gson gson = new Gson();
                Map<String, String> metadata = gson.fromJson(metadataJson, new TypeToken<Map<String, String>>() {}.getType());
                resourceData.setMetadata(metadata);
            } catch (Exception e) {
                LOGGER.warn("Failed to parse metadata: " + e.getMessage());
            }
        }

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

    @GetMapping("/categories")
    public String listCategories(Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            response.sendRedirect(request.getContextPath() + "/?error=401");
            return null;
        }

        List<CategoryEntity> categories = serviceProvider.getCategoryService().getAllCategories();
        model.addAttribute("categories", categories);
        model.addAttribute("username", accessor.getAccountData().getLogin());

        String theme = settings.getValue(Strings.CONTEXT_DATASOURCE_VIEW, Strings.KEY_STYLES_THEME);
        String schema = settings.getValue(Strings.CONTEXT_DATASOURCE_VIEW, Strings.KEY_STYLES_SCHEMA);
        model.addAttribute("stylesTheme", theme);
        model.addAttribute("stylesSchema", schema);

        return "/2000s/editor-categories-list";
    }

    @GetMapping("/category/create")
    public String createCategoryPage(Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            response.sendRedirect(request.getContextPath() + "/?error=401");
            return null;
        }

        model.addAttribute("username", accessor.getAccountData().getLogin());

        String theme = settings.getValue(Strings.CONTEXT_DATASOURCE_VIEW, Strings.KEY_STYLES_THEME);
        String schema = settings.getValue(Strings.CONTEXT_DATASOURCE_VIEW, Strings.KEY_STYLES_SCHEMA);
        model.addAttribute("stylesTheme", theme);
        model.addAttribute("stylesSchema", schema);

        return "/2000s/editor-category-create";
    }

    @PostMapping("/category/create")
    public String createCategorySubmit(@RequestParam(value = "itemName", required = false) String itemName,
                                       @RequestParam(value = "navigationTitle", required = false) String navigationTitle,
                                       @RequestParam(value = "navigationDescription", required = false) String navigationDescription,
                                       @RequestParam(value = "isVisible", required = false) String isVisible,
                                       @RequestParam(value = "isActive", required = false) String isActive,
                                       Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            response.sendRedirect(request.getContextPath() + "/?error=401");
            return null;
        }

        if (itemName == null || itemName.trim().isEmpty()) {
            model.addAttribute("error", "Category Name is required");
            loadCategoryCreatePageWithError(model, accessor);
            return "/2000s/editor-category-create";
        }

        CategoryData categoryData = new CategoryData();
        categoryData.setItemName(itemName.trim());
        categoryData.setNavigationTitle(navigationTitle != null ? navigationTitle : "");
        categoryData.setNavigationDescription(navigationDescription != null ? navigationDescription : "");
        categoryData.setVisible(isVisible != null && isVisible.equals("on"));
        categoryData.setActive(isActive != null && isActive.equals("on"));

        CategoryEntity newCategory = new CategoryEntity(categoryData, accessor);

        try {
            serviceProvider.getCategoryService().insert(newCategory);
        } catch (Exception e) {
            LOGGER.error("Failed to create category: " + e.getMessage(), e);
            model.addAttribute("error", "Failed to create category: " + e.getMessage());
            loadCategoryCreatePageWithError(model, accessor);
            return "/2000s/editor-category-create";
        }

        response.sendRedirect(request.getContextPath() + "/editor/categories");
        return null;
    }

    private void loadCategoryCreatePageWithError(Model model, AccountEntity accessor) {
        model.addAttribute("username", accessor.getAccountData().getLogin());
        String theme = settings.getValue(Strings.CONTEXT_DATASOURCE_VIEW, Strings.KEY_STYLES_THEME);
        String schema = settings.getValue(Strings.CONTEXT_DATASOURCE_VIEW, Strings.KEY_STYLES_SCHEMA);
        model.addAttribute("stylesTheme", theme);
        model.addAttribute("stylesSchema", schema);
    }

    @GetMapping("/category/edit")
    public String editCategoryPage(@RequestParam(value = "id", required = false) String id,
                                   Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            response.sendRedirect(request.getContextPath() + "/?error=401");
            return null;
        }

        if (id == null || id.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/editor/categories");
            return null;
        }

        CategoryEntity category;
        try {
            org.bson.types.ObjectId objId = new org.bson.types.ObjectId(id.trim());
            category = (CategoryEntity) serviceProvider.getCategoryService().getById(CategoryEntity.class, objId);
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/?error=404");
            return null;
        }

        if (category == null) {
            response.sendRedirect(request.getContextPath() + "/?error=404");
            return null;
        }

        model.addAttribute("category", category);
        model.addAttribute("username", accessor.getAccountData().getLogin());

        String theme = settings.getValue(Strings.CONTEXT_DATASOURCE_VIEW, Strings.KEY_STYLES_THEME);
        String schema = settings.getValue(Strings.CONTEXT_DATASOURCE_VIEW, Strings.KEY_STYLES_SCHEMA);
        model.addAttribute("stylesTheme", theme);
        model.addAttribute("stylesSchema", schema);

        return "/2000s/editor-category-edit";
    }

    @PostMapping("/category/save")
    public String saveCategory(@RequestParam(value = "id", required = false) String id,
                               @RequestParam(value = "itemName", required = false) String itemName,
                               @RequestParam(value = "navigationTitle", required = false) String navigationTitle,
                               @RequestParam(value = "navigationDescription", required = false) String navigationDescription,
                               @RequestParam(value = "isVisible", required = false) String isVisible,
                               @RequestParam(value = "isActive", required = false) String isActive,
                               Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            response.sendRedirect(request.getContextPath() + "/?error=401");
            return null;
        }

        if (id == null || id.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/editor/categories");
            return null;
        }

        CategoryEntity category;
        try {
            org.bson.types.ObjectId objId = new org.bson.types.ObjectId(id.trim());
            category = (CategoryEntity) serviceProvider.getCategoryService().getById(CategoryEntity.class, objId);
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/?error=404");
            return null;
        }

        if (category == null) {
            response.sendRedirect(request.getContextPath() + "/?error=404");
            return null;
        }

        if (category.getCategoryData() == null) {
            category.setCategoryData(new CategoryData());
        }

        CategoryData categoryData = category.getCategoryData();
        if (itemName != null) categoryData.setItemName(itemName);
        if (navigationTitle != null) categoryData.setNavigationTitle(navigationTitle);
        if (navigationDescription != null) categoryData.setNavigationDescription(navigationDescription);
        categoryData.setVisible(isVisible != null && isVisible.equals("on"));
        categoryData.setActive(isActive != null && isActive.equals("on"));

        HistoryData historyData = category.getHistoryData();
        if (historyData == null) {
            historyData = new HistoryData();
        }
        Event[] oldEvents = historyData.getEvents();
        Event[] newEvents = new Event[oldEvents.length + 1];
        System.arraycopy(oldEvents, 0, newEvents, 0, oldEvents.length);
        newEvents[oldEvents.length] = new Event(EventType.ENTITY_EDITED, LocalDateTime.now(), accessor);
        historyData.setEvents(newEvents);
        category.setHistoryData(historyData);

        serviceProvider.getCategoryService().replace(category);

        response.sendRedirect(request.getContextPath() + "/editor/categories");
        return null;
    }

    @PostMapping("/category/delete")
    public String deleteCategory(@RequestParam(value = "id", required = false) String id,
                                 Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            response.sendRedirect(request.getContextPath() + "/?error=401");
            return null;
        }

        if (id == null || id.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/editor/categories");
            return null;
        }

        CategoryEntity category;
        try {
            org.bson.types.ObjectId objId = new org.bson.types.ObjectId(id.trim());
            category = (CategoryEntity) serviceProvider.getCategoryService().getById(CategoryEntity.class, objId);
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/?error=404");
            return null;
        }

        try {
            org.bson.types.ObjectId objId = new org.bson.types.ObjectId(id.trim());
            serviceProvider.getCategoryService().delete(category);
        } catch (Exception e) {
            LOGGER.error("Failed to delete category: " + e.getMessage(), e);
        }

        response.sendRedirect(request.getContextPath() + "/editor/categories");
        return null;
    }
}

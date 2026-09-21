package com.ravenherz.cse.controller;

import com.ravenherz.cse.dal.dto.*;
import com.ravenherz.cse.dal.dto.basic.*;
import com.ravenherz.cse.dal.dto.basic.enums.AccessType;
import com.ravenherz.cse.dal.dto.basic.enums.EventType;
import com.ravenherz.cse.dal.EntityAccess;
import com.ravenherz.cse.present.EditorTree;
import com.ravenherz.cse.present.ResourceGroupIndex;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.ravenherz.cse.dal.dto.basic.enums.ResourceType.IMAGE;

@Controller
@RequestMapping("/editor")
public class EditorPagesController extends AbstractController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditorPagesController.class);

    @Autowired
    private EditorAlbumsController editorAlbumsController;

    @Autowired
    private ResourceGroupIndex resourceGroupIndex;

    @GetMapping
    public String listPagesRedirect(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(request.getContextPath() + "/editor/resources");
        return null;
    }

    @GetMapping("/pages")
    public String listPages(@RequestParam(value = "category", required = false) String category,
                            HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        String dest = (category != null && !category.isBlank())
                ? EditorTree.hrefOf(EditorTree.categoryNodeId(category))
                : EditorTree.CATEGORIES_HREF;
        response.sendRedirect(request.getContextPath() + dest);
        return null;
    }

    @GetMapping("/create")
    public String createPage(@RequestParam(value = "categoryId", required = false) String categoryId,
                             Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }

        List<CategoryEntity> categories = serviceProvider.getCategoryService().getAllCategories();
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategoryId", categoryId);
        addEditorChrome(model, accessor);
        EditorInline.putTreeForCreate(model, resourceGroupIndex, categoryId);
        addAccessPanel(model, null, accessor);

        return "/admin/editor-page-create";
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
                                   @RequestParam(value = "header", required = false) String header,
                                   @RequestParam(value = "subHeader", required = false) String subHeader,
                                   @RequestParam(value = "description", required = false) String description,
                                   @RequestParam(value = "tags", required = false) String tags,
                                   @RequestParam(value = "categoryId", required = false) String categoryId,
                                   @RequestParam(value = "noTopDisplayImage", defaultValue = "false") boolean noTopDisplayImage,
                                   @RequestParam(value = "exportPdf", defaultValue = "false") boolean exportPdf,
                                   Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
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
        pageData.setHeader(header != null ? header : "");
        pageData.setSubHeader(subHeader != null ? subHeader : "");
        pageData.setDescription(description != null ? description : "");
        pageData.setNoTopDisplayImage(noTopDisplayImage);
        pageData.setExportPdf(exportPdf);
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
        applyAccess(request, newItem);

        try {
            serviceProvider.getItemService().insert(newItem);
            resourceGroupIndex.contentChanged();
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
        model.addAttribute("selectedCategoryId", categoryId);
        addEditorChrome(model, accessor);
        EditorInline.putTreeForCreate(model, resourceGroupIndex, categoryId);

        return "/admin/editor-page-create";
    }

    @GetMapping("/edit")
    public String editPage(@RequestParam(value = "name", required = false) String name,
                            Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }

        if (name == null || name.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/editor");
            return null;
        }

        ItemEntity item = serviceProvider.getItemService().getByName(name);
        if (item == null) {
            error(404, request, response);
            return null;
        }

        if (!EntityAccess.isAccessible(item, AccessType.ACCESS_READ, accessor)) {
            error(403, request, response);
            return null;
        }

        model.addAttribute("item", item);

        if (item.isAlbum()) {
            editorAlbumsController.fillAlbumFormLookups(model, accessor);
            addEditorChrome(model, accessor);
            addAccessPanel(model, item, accessor);
            return "/admin/editor-album-edit";
        }

        addEditorChrome(model, accessor);
        EditorInline.putTreeForItem(model, resourceGroupIndex, item);
        addAccessPanel(model, item, accessor);

        return "/admin/editor-page-edit";
    }

    @PostMapping("/save")
    public String savePage(@RequestParam(value = "name", required = false) String name,
                           @RequestParam(value = "originalName", required = false) String originalName,
                           @RequestParam(value = "header", required = false) String header,
                           @RequestParam(value = "subHeader", required = false) String subHeader,
                           @RequestParam(value = "description", required = false) String description,
                           @RequestParam(value = "tags", required = false) String tags,
                           @RequestParam(value = "imageId", required = false) String imageId,
                           @RequestParam(value = "resourceGroupId", required = false) String resourceGroupId,
                           @RequestParam(value = "noTopDisplayImage", defaultValue = "false") boolean noTopDisplayImage,
                           @RequestParam(value = "exportPdf", defaultValue = "false") boolean exportPdf,
                           Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }

        String persistedName = originalName != null && !originalName.isBlank()
                ? originalName.trim()
                : (name == null ? "" : name.trim());
        if (persistedName.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/editor");
            return null;
        }

        ItemEntity item = serviceProvider.getItemService().getByName(persistedName);
        if (item == null) {
            error(404, request, response);
            return null;
        }

        if (!EntityAccess.isAccessible(item, AccessType.ACCESS_EDIT, accessor)) {
            error(403, request, response);
            return null;
        }

        String idError = assignItemId(serviceProvider.getItemService(), item, name);
        if (idError != null) {
            model.addAttribute("error", idError);
            model.addAttribute("item", item);
            model.addAttribute("originalName", persistedName);
            model.addAttribute("postedName", name);
            if (item.isAlbum()) {
                editorAlbumsController.fillAlbumFormLookups(model, accessor);
                return "/admin/editor-album-edit";
            }
            addEditorChrome(model, accessor);
            EditorInline.putTreeForItem(model, resourceGroupIndex, item);
            addAccessPanel(model, item, accessor);
            return "/admin/editor-page-edit";
        }

        if (item.isAlbum()) {
            return editorAlbumsController.saveAlbum(item, header, subHeader, description, tags,
                    resourceGroupId, accessor, model, request, response);
        }

        if (item.getPageData() == null) {
            item.setPageData(new PageData());
        }

        PageData pageData = item.getPageData();

        if (header != null) pageData.setHeader(header);
        if (subHeader != null) pageData.setSubHeader(subHeader);
        if (description != null) pageData.setDescription(description);
        pageData.setNoTopDisplayImage(noTopDisplayImage);
        pageData.setExportPdf(exportPdf);
        if (tags != null) {
            List<String> tagList = Arrays.stream(tags.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            pageData.setTags(tagList);
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
        applyAccess(request, item);

        serviceProvider.getItemService().replace(item);
        resourceGroupIndex.contentChanged();

        response.sendRedirect(request.getContextPath() + "/editor/edit?name=" + item.getUniqueUriName());
        return null;
    }

    static String assignItemId(com.ravenherz.cse.dal.dao.ItemService items, ItemEntity item, String nextName) {
        if (nextName == null || nextName.isBlank()) {
            return "Item ID is required";
        }
        String next = nextName.trim();
        String current = item.getUniqueUriName() == null ? "" : item.getUniqueUriName().trim();
        if (next.equals(current)) {
            return null;
        }
        ItemEntity clash = items.getByName(next);
        if (clash != null && (item.getId() == null || !item.getId().equals(clash.getId()))) {
            return "An item with this ID already exists";
        }
        item.setUniqueUriName(next);
        return null;
    }

    @PostMapping("/item/category")
    public String moveItemCategory(@RequestParam(value = "name", required = false) List<String> names,
                                   @RequestParam(value = "categoryId", required = false) String categoryId,
                                   @RequestParam(value = "returnGroup", required = false) String returnGroup,
                                   HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }

        CategoryEntity category = null;
        String rawCategory = categoryId == null ? "" : categoryId.trim();
        if (!rawCategory.isEmpty() && !EditorTree.CATEGORIES_ID.equals(rawCategory)) {
            try {
                org.bson.types.ObjectId catObjId = new org.bson.types.ObjectId(rawCategory);
                category = (CategoryEntity) serviceProvider.getCategoryService()
                        .getById(CategoryEntity.class, catObjId);
            } catch (Exception e) {
                LOGGER.warn("Invalid category ID: " + rawCategory);
            }
            if (category == null) {
                redirectCatalog(request, response, returnGroup);
                return null;
            }
        }

        boolean changed = false;
        if (names != null) {
            java.util.LinkedHashSet<String> unique = new java.util.LinkedHashSet<>();
            for (String itemName : names) {
                if (itemName != null && !itemName.trim().isEmpty()) {
                    unique.add(itemName.trim());
                }
            }
            for (String itemName : unique) {
                ItemEntity item = serviceProvider.getItemService().getByName(itemName);
                if (item == null || !EntityAccess.isAccessible(item, AccessType.ACCESS_EDIT, accessor)) {
                    continue;
                }
                item.setRefCategory(category);
                serviceProvider.getItemService().replace(item);
                changed = true;
            }
        }
        if (changed) {
            resourceGroupIndex.contentChanged();
        }
        redirectCatalog(request, response, returnGroup);
        return null;
    }

    private void redirectCatalog(HttpServletRequest request, HttpServletResponse response, String returnGroup)
            throws IOException {
        String group = returnGroup == null || returnGroup.isBlank()
                ? EditorTree.CATEGORIES_ID : returnGroup.trim();
        response.sendRedirect(request.getContextPath() + "/editor/resources?group="
                + java.net.URLEncoder.encode(group, java.nio.charset.StandardCharsets.UTF_8));
    }

    @PostMapping("/delete")
    public String deletePage(@RequestParam(value = "name", required = false) String name,
                             @RequestParam(value = "returnGroup", required = false) String returnGroup,
                             Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }

        if (name == null || name.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath()
                    + EditorTree.pageDeleteReturnHref(returnGroup, null));
            return null;
        }

        ItemEntity item = serviceProvider.getItemService().getByName(name);
        if (item == null) {
            error(404, request, response);
            return null;
        }

        if (!EntityAccess.isAccessible(item, AccessType.ACCESS_DELETE, accessor)) {
            error(403, request, response);
            return null;
        }

        String categoryId = item.getRefCategoryId() == null ? null : item.getRefCategoryId().toString();
        try {
            serviceProvider.getItemService().delete(item);
            resourceGroupIndex.contentChanged();
        } catch (Exception e) {
            LOGGER.error("Failed to delete page: " + e.getMessage(), e);
        }

        response.sendRedirect(request.getContextPath()
                + EditorTree.pageDeleteReturnHref(returnGroup, categoryId));
        return null;
    }
}

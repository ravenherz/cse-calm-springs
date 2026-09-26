package com.ravenherz.cse.controller;

import com.ravenherz.cse.admin.EditorChrome;
import com.ravenherz.cse.admin.PageDesk;
import com.ravenherz.cse.dal.EntityAccess;
import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.StoredIds;
import com.ravenherz.cse.dal.dto.*;
import com.ravenherz.cse.dal.dto.basic.*;
import com.ravenherz.cse.dal.dto.basic.enums.AccessType;
import com.ravenherz.cse.dal.dto.basic.enums.EventType;
import com.ravenherz.cse.present.EditorTree;
import com.ravenherz.cse.present.ResourceGroupIndex;
import com.ravenherz.cse.security.AccessForms;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.ravenherz.cse.dal.dto.basic.enums.ResourceType.IMAGE;

@Component
public class PageAdapter implements PageDesk {

    private static final Logger LOGGER = LoggerFactory.getLogger(PageAdapter.class);

    private final AuthSupport authSupport;
    private final EditorChrome editorChrome;
    private final ServiceProvider serviceProvider;
    private final ResourceGroupIndex resourceGroupIndex;
    private final AlbumAdapter albums;

    public PageAdapter(AuthSupport authSupport, EditorChrome editorChrome, ServiceProvider serviceProvider,
            ResourceGroupIndex resourceGroupIndex, AlbumAdapter albums) {
        this.authSupport = authSupport;
        this.editorChrome = editorChrome;
        this.serviceProvider = serviceProvider;
        this.resourceGroupIndex = resourceGroupIndex;
        this.albums = albums;
    }

    @Override
    public String list(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(request.getContextPath() + "/editor/resources");
        return null;
    }

    @Override
    public String listInCategory(String category, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        String dest = (category != null && !category.isBlank())
                ? EditorTree.hrefOf(EditorTree.categoryNodeId(category))
                : EditorTree.CATEGORIES_HREF;
        response.sendRedirect(request.getContextPath() + dest);
        return null;
    }

    @Override
    public String createPage(String categoryId, Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
            return null;
        }

        List<CategoryEntity> categories = serviceProvider.getCategoryService().getAllCategories();
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategoryId", categoryId);
        editorChrome.apply(model, accessor);
        EditorInline.putTreeForCreate(model, resourceGroupIndex, categoryId);
        addAccessPanel(model, null, accessor);

        return "/admin/editor-page-create";
    }

    @Override
    public String editorData(HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
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

    @Override
    public String create(String name, String header, String subHeader, String description, String tags,
            String categoryId, boolean noTopDisplayImage, boolean exportPdf, Model model, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
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
                CategoryEntity category = (CategoryEntity) serviceProvider.getCategoryService().getById(CategoryEntity.class, StoredIds.entityId(catObjId));
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
        editorChrome.apply(model, accessor);
        EditorInline.putTreeForCreate(model, resourceGroupIndex, categoryId);

        return "/admin/editor-page-create";
    }

    @Override
    public String edit(String name, Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
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

        if (!EntityAccess.isAccessible(item, AccessType.ACCESS_READ, accessor)) {
            response.sendRedirect(request.getContextPath() + "/?error=403");
            return null;
        }

        model.addAttribute("item", item);

        if (item.isAlbum()) {
            albums.fillAlbumFormLookups(model, accessor);
            editorChrome.apply(model, accessor);
            addAccessPanel(model, item, accessor);
            return "/admin/editor-album-edit";
        }

        editorChrome.apply(model, accessor);
        EditorInline.putTreeForItem(model, resourceGroupIndex, item);
        model.addAttribute("featuredImagePath", featuredImagePath(item));
        addAccessPanel(model, item, accessor);

        return "/admin/editor-page-edit";
    }

    @Override
    public String save(String name, String originalName, String header, String subHeader, String description,
            String tags, String imageId, String resourceGroupId, boolean noTopDisplayImage, boolean exportPdf,
            Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
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
            response.sendRedirect(request.getContextPath() + "/?error=404");
            return null;
        }

        if (!EntityAccess.isAccessible(item, AccessType.ACCESS_EDIT, accessor)) {
            response.sendRedirect(request.getContextPath() + "/?error=403");
            return null;
        }

        String idError = assignItemId(serviceProvider.getItemService(), item, name);
        if (idError != null) {
            model.addAttribute("error", idError);
            model.addAttribute("item", item);
            model.addAttribute("originalName", persistedName);
            model.addAttribute("postedName", name);
            if (item.isAlbum()) {
                albums.fillAlbumFormLookups(model, accessor);
                return "/admin/editor-album-edit";
            }
            editorChrome.apply(model, accessor);
            EditorInline.putTreeForItem(model, resourceGroupIndex, item);
            model.addAttribute("featuredImagePath", featuredImagePath(item));
            addAccessPanel(model, item, accessor);
            return "/admin/editor-page-edit";
        }

        if (item.isAlbum()) {
            return albums.saveAlbum(item, header, subHeader, description, tags,
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
                ResourceEntity image = (ResourceEntity) serviceProvider.getResourceService().getById(ResourceEntity.class, StoredIds.entityId(imgObjId));
                pageData.setRefImageId(image.getId());
            } catch (Exception e) {
                LOGGER.warn("Invalid image ID: " + imageId);
            }
        } else {
            pageData.setRefImageId(null);
        }

        HistoryData historyData = item.getHistoryData();
        if (historyData == null) {
            historyData = new HistoryData();
        }
        Event[] oldEvents = historyData.getEvents();
        Event[] newEvents = new Event[oldEvents.length + 1];
        System.arraycopy(oldEvents, 0, newEvents, 0, oldEvents.length);
        newEvents[oldEvents.length] = new Event(EventType.ENTITY_EDITED, LocalDateTime.now(), accessor == null ? null : accessor.getId());
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

    @Override
    public String moveCategory(List<String> names, String categoryId, String returnGroup, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
            return null;
        }

        CategoryEntity category = null;
        String rawCategory = categoryId == null ? "" : categoryId.trim();
        if (!rawCategory.isEmpty() && !EditorTree.CATEGORIES_ID.equals(rawCategory)) {
            try {
                org.bson.types.ObjectId catObjId = new org.bson.types.ObjectId(rawCategory);
                category = (CategoryEntity) serviceProvider.getCategoryService()
                        .getById(CategoryEntity.class, StoredIds.entityId(catObjId));
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

    private String featuredImagePath(ItemEntity item) {
        EntityId imageId = item == null || item.getPageData() == null ? null : item.getPageData().getRefImageId();
        return imageId == null ? null : resourceGroupIndex.filePath(imageId.toString());
    }

    @Override
    public String delete(String name, String returnGroup, Model model, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
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
            response.sendRedirect(request.getContextPath() + "/?error=404");
            return null;
        }

        if (!EntityAccess.isAccessible(item, AccessType.ACCESS_DELETE, accessor)) {
            response.sendRedirect(request.getContextPath() + "/?error=403");
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

    private void addAccessPanel(Model model, BasicEntity entity, AccountEntity accessor) {
        if (serviceProvider == null || serviceProvider.getRoleService() == null) {
            return;
        }
        boolean canEdit = entity == null
                || EntityAccess.isAccessible(entity, AccessType.ACCESS_EDIT, accessor);
        AccessForms.addToModel(model, entity, serviceProvider.getRoleService(),
                serviceProvider.getAccountService().getAllAccounts(), canEdit);
    }

    private void applyAccess(HttpServletRequest request, BasicEntity entity) {
        if (serviceProvider == null || serviceProvider.getRoleService() == null) {
            return;
        }
        AccessForms.apply(request, entity, serviceProvider.getRoleService());
    }
}

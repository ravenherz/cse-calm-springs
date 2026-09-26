package com.ravenherz.cse.controller;

import com.ravenherz.cse.admin.AlbumDesk;
import com.ravenherz.cse.admin.EditorChrome;
import com.ravenherz.cse.dal.EntityAccess;
import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.StoredIds;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.CategoryEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.ResourceGroupEntity;
import com.ravenherz.cse.dal.dto.basic.AlbumData;
import com.ravenherz.cse.dal.dto.basic.Event;
import com.ravenherz.cse.dal.dto.basic.HistoryData;
import com.ravenherz.cse.dal.dto.basic.enums.AccessType;
import com.ravenherz.cse.dal.dto.basic.enums.EventType;
import com.ravenherz.cse.present.ResourceGroupDisplayDTO;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AlbumAdapter implements AlbumDesk {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlbumAdapter.class);

    private final AuthSupport authSupport;
    private final EditorChrome editorChrome;
    private final ServiceProvider serviceProvider;
    private final ResourceGroupIndex resourceGroupIndex;

    public AlbumAdapter(AuthSupport authSupport, EditorChrome editorChrome, ServiceProvider serviceProvider,
            ResourceGroupIndex resourceGroupIndex) {
        this.authSupport = authSupport;
        this.editorChrome = editorChrome;
        this.serviceProvider = serviceProvider;
        this.resourceGroupIndex = resourceGroupIndex;
    }

    @Override
    public String createPage(String categoryId, Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        return loadAlbumCreate(model, accessor, categoryId);
    }

    @Override
    public String create(String name, String header, String subHeader, String description, String tags,
            String categoryId, String resourceGroupId, Model model, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
            return null;
        }

        if (name == null || name.trim().isEmpty()) {
            model.addAttribute("error", "Album ID is required");
            return loadAlbumCreate(model, accessor, categoryId);
        }
        if (resourceGroupId == null || resourceGroupId.trim().isEmpty()) {
            model.addAttribute("error", "A resource group is required");
            return loadAlbumCreate(model, accessor, categoryId);
        }

        ItemEntity existing = serviceProvider.getItemService().getByName(name);
        if (existing != null) {
            model.addAttribute("error", "An item with this ID already exists");
            return loadAlbumCreate(model, accessor, categoryId);
        }

        AlbumData albumData = new AlbumData();
        albumData.setHeader(header != null ? header : "");
        albumData.setSubHeader(subHeader != null ? subHeader : "");
        albumData.setDescription(description != null ? description : "");
        albumData.setTags(parseTags(tags));
        ResourceGroupEntity group = loadResourceGroup(resourceGroupId);
        if (group == null) {
            model.addAttribute("error", "Resource group not found");
            return loadAlbumCreate(model, accessor, categoryId);
        }
        albumData.setRefResourceGroupId(group.getId());

        ItemEntity newItem = new ItemEntity(name.trim(), albumData, accessor);
        applyAccess(request, newItem);
        if (categoryId != null && !categoryId.trim().isEmpty()) {
            try {
                org.bson.types.ObjectId catObjId = new org.bson.types.ObjectId(categoryId.trim());
                CategoryEntity category = (CategoryEntity) serviceProvider.getCategoryService()
                        .getById(CategoryEntity.class, StoredIds.entityId(catObjId));
                newItem.setRefCategory(category);
            } catch (Exception e) {
                LOGGER.warn("Invalid category ID: " + categoryId);
            }
        }

        try {
            serviceProvider.getItemService().insert(newItem);
            resourceGroupIndex.contentChanged();
        } catch (Exception e) {
            LOGGER.error("Failed to create album: " + e.getMessage(), e);
            model.addAttribute("error", "Failed to create album: " + e.getMessage());
            return loadAlbumCreate(model, accessor, categoryId);
        }

        response.sendRedirect(request.getContextPath() + "/editor/edit?name=" + name.trim());
        return null;
    }

    public String saveAlbum(ItemEntity item, String header, String subHeader,
            String description, String tags, String resourceGroupId,
            AccountEntity accessor, Model model, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (item.getAlbumData() == null) {
            item.setAlbumData(new AlbumData());
        }
        AlbumData albumData = item.getAlbumData();
        if (header != null) albumData.setHeader(header);
        if (subHeader != null) albumData.setSubHeader(subHeader);
        if (description != null) albumData.setDescription(description);
        if (tags != null) albumData.setTags(parseTags(tags));

        if (resourceGroupId == null || resourceGroupId.trim().isEmpty()) {
            model.addAttribute("error", "A resource group is required");
            model.addAttribute("item", item);
            fillAlbumFormLookups(model, accessor);
            return "/admin/editor-album-edit";
        }
        ResourceGroupEntity group = loadResourceGroup(resourceGroupId);
        if (group == null) {
            model.addAttribute("error", "Resource group not found");
            model.addAttribute("item", item);
            fillAlbumFormLookups(model, accessor);
            return "/admin/editor-album-edit";
        }
        albumData.setRefResourceGroupId(group.getId());

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

    private String loadAlbumCreate(Model model, AccountEntity accessor, String categoryId) {
        fillAlbumFormLookups(model, accessor);
        model.addAttribute("selectedCategoryId", categoryId);
        EditorInline.putTreeForAlbumCreate(model, resourceGroupIndex, categoryId);
        addAccessPanel(model, null, accessor);
        return "/admin/editor-album-create";
    }

    public void fillAlbumFormLookups(Model model, AccountEntity accessor) {
        model.addAttribute("categories", serviceProvider.getCategoryService().getAllCategories());
        List<ResourceGroupDisplayDTO> groups = resourceGroupIndex.view().assignableGroups();
        model.addAttribute("resourceGroups", groups);
        editorChrome.apply(model, accessor);
        if (model.getAttribute("item") instanceof ItemEntity item) {
            EditorInline.putTreeForItem(model, resourceGroupIndex, item);
            addAccessPanel(model, item, accessor);
            model.addAttribute("albumGroupPath", albumGroupPath(item, groups));
        }
    }

    private static String albumGroupPath(ItemEntity item, List<ResourceGroupDisplayDTO> groups) {
        if (item.getAlbumData() == null || item.getAlbumData().getRefResourceGroupId() == null) {
            return null;
        }
        String id = item.getAlbumData().getRefResourceGroupId().toString();
        if (groups != null) {
            for (ResourceGroupDisplayDTO group : groups) {
                if (group != null && id.equals(group.getId()) && group.getPathLabel() != null
                        && !group.getPathLabel().isBlank()) {
                    return group.getPathLabel();
                }
            }
        }
        return null;
    }

    private ResourceGroupEntity loadResourceGroup(String resourceGroupId) {
        try {
            org.bson.types.ObjectId groupId = new org.bson.types.ObjectId(resourceGroupId.trim());
            return (ResourceGroupEntity) serviceProvider.getResourceGroupService()
                    .getById(ResourceGroupEntity.class, StoredIds.entityId(groupId));
        } catch (Exception e) {
            LOGGER.warn("Invalid resource group ID: " + resourceGroupId);
            return null;
        }
    }

    private List<String> parseTags(String tags) {
        if (tags == null || tags.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
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

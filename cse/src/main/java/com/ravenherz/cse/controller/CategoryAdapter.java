package com.ravenherz.cse.controller;

import com.ravenherz.cse.admin.CategoryDesk;
import com.ravenherz.cse.admin.EditorChrome;
import com.ravenherz.cse.dal.EntityAccess;
import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.StoredIds;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.CategoryEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.basic.CategoryData;
import com.ravenherz.cse.dal.dto.basic.Event;
import com.ravenherz.cse.dal.dto.basic.HistoryData;
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
import java.util.List;

@Component
public class CategoryAdapter implements CategoryDesk {

    private static final Logger LOGGER = LoggerFactory.getLogger(CategoryAdapter.class);

    private final AuthSupport authSupport;
    private final EditorChrome editorChrome;
    private final ServiceProvider serviceProvider;
    private final ResourceGroupIndex resourceGroupIndex;

    public CategoryAdapter(AuthSupport authSupport, EditorChrome editorChrome, ServiceProvider serviceProvider,
            ResourceGroupIndex resourceGroupIndex) {
        this.authSupport = authSupport;
        this.editorChrome = editorChrome;
        this.serviceProvider = serviceProvider;
        this.resourceGroupIndex = resourceGroupIndex;
    }

    @Override
    public String list(HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        response.sendRedirect(request.getContextPath() + EditorTree.CATEGORIES_HREF);
        return null;
    }

    @Override
    public String createPage(Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
            return null;
        }

        editorChrome.apply(model, accessor);
        EditorInline.putTreeForCategoryCreate(model, resourceGroupIndex);
        addAccessPanel(model, null, accessor);

        return "/admin/editor-category-create";
    }

    @Override
    public String create(String itemName, String navigationTitle, String navigationDescription, String displayCount,
            String displayPriority, String isVisible, String isActive, Model model, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
            return null;
        }

        if (itemName == null || itemName.trim().isEmpty()) {
            model.addAttribute("error", "Category Name is required");
            loadCategoryCreatePageWithError(model, accessor);
            return "/admin/editor-category-create";
        }

        CategoryData categoryData = new CategoryData();
        categoryData.setItemName(itemName.trim());
        categoryData.setNavigationTitle(navigationTitle != null ? navigationTitle : "");
        categoryData.setNavigationDescription(navigationDescription != null ? navigationDescription : "");
        categoryData.setVisible(isVisible != null && isVisible.equals("on"));
        categoryData.setActive(isActive != null && isActive.equals("on"));
        categoryData.setDisplayCount(parseIntOrZero(displayCount));
        categoryData.setDisplayPriority(parseIntOrZero(displayPriority));

        CategoryEntity newCategory = new CategoryEntity(categoryData, accessor);
        applyAccess(request, newCategory);

        try {
            serviceProvider.getCategoryService().insert(newCategory);
            resourceGroupIndex.contentChanged();
        } catch (Exception e) {
            LOGGER.error("Failed to create category: " + e.getMessage(), e);
            model.addAttribute("error", "Failed to create category: " + e.getMessage());
            loadCategoryCreatePageWithError(model, accessor);
            return "/admin/editor-category-create";
        }

        response.sendRedirect(request.getContextPath() + EditorTree.CATEGORIES_HREF);
        return null;
    }

    private void loadCategoryCreatePageWithError(Model model, AccountEntity accessor) {
        editorChrome.apply(model, accessor);
        EditorInline.putTreeForCategoryCreate(model, resourceGroupIndex);
    }

    @Override
    public String editPage(String id, Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
            return null;
        }

        if (id == null || id.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + EditorTree.CATEGORIES_HREF);
            return null;
        }

        CategoryEntity category;
        try {
            org.bson.types.ObjectId objId = new org.bson.types.ObjectId(id.trim());
            category = (CategoryEntity) serviceProvider.getCategoryService().getById(CategoryEntity.class, StoredIds.entityId(objId));
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/?error=404");
            return null;
        }

        if (category == null) {
            response.sendRedirect(request.getContextPath() + "/?error=404");
            return null;
        }

        model.addAttribute("category", category);
        editorChrome.apply(model, accessor);
        EditorInline.putTreeForCategory(model, resourceGroupIndex, category);
        addAccessPanel(model, category, accessor);

        return "/admin/editor-category-edit";
    }

    @Override
    public String save(String id, String itemName, String navigationTitle, String navigationDescription,
            String displayCount, String displayPriority, String isVisible, String isActive, Model model,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
            return null;
        }

        if (id == null || id.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + EditorTree.CATEGORIES_HREF);
            return null;
        }

        CategoryEntity category;
        try {
            org.bson.types.ObjectId objId = new org.bson.types.ObjectId(id.trim());
            category = (CategoryEntity) serviceProvider.getCategoryService().getById(CategoryEntity.class, StoredIds.entityId(objId));
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
        categoryData.setDisplayCount(parseIntOrZero(displayCount));
        categoryData.setDisplayPriority(parseIntOrZero(displayPriority));

        HistoryData historyData = category.getHistoryData();
        if (historyData == null) {
            historyData = new HistoryData();
        }
        Event[] oldEvents = historyData.getEvents();
        Event[] newEvents = new Event[oldEvents.length + 1];
        System.arraycopy(oldEvents, 0, newEvents, 0, oldEvents.length);
        newEvents[oldEvents.length] = new Event(EventType.ENTITY_EDITED, LocalDateTime.now(), accessor == null ? null : accessor.getId());
        historyData.setEvents(newEvents);
        category.setHistoryData(historyData);
        applyAccess(request, category);

        serviceProvider.getCategoryService().replace(category);
        resourceGroupIndex.contentChanged();

        response.sendRedirect(request.getContextPath() + "/editor/category/edit?id=" + id.trim());
        return null;
    }

    @Override
    public String delete(String id, String returnGroup, Model model, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
            return null;
        }

        String dest = request.getContextPath() + EditorTree.categoryDeleteReturnHref(returnGroup);
        if (id == null || id.trim().isEmpty()) {
            response.sendRedirect(dest);
            return null;
        }

        CategoryEntity category;
        try {
            org.bson.types.ObjectId objId = new org.bson.types.ObjectId(id.trim());
            category = (CategoryEntity) serviceProvider.getCategoryService().getById(CategoryEntity.class, StoredIds.entityId(objId));
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/?error=404");
            return null;
        }

        if (category == null) {
            response.sendRedirect(request.getContextPath() + "/?error=404");
            return null;
        }

        try {
            List<ItemEntity> pages = serviceProvider.getItemService().getAllByCategory(category);
            if (pages != null) {
                for (ItemEntity item : pages) {
                    if (item != null) {
                        serviceProvider.getItemService().delete(item);
                    }
                }
            }
            serviceProvider.getCategoryService().delete(category);
            resourceGroupIndex.contentChanged();
        } catch (Exception e) {
            LOGGER.error("Failed to delete category: " + e.getMessage(), e);
        }

        response.sendRedirect(dest);
        return null;
    }

    private int parseIntOrZero(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
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

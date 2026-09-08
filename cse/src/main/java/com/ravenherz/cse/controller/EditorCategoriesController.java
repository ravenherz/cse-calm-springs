package com.ravenherz.cse.controller;

import com.ravenherz.cse.present.EditorTree;
import com.ravenherz.cse.present.ResourceGroupIndex;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.CategoryEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.basic.CategoryData;
import com.ravenherz.cse.dal.dto.basic.Event;
import com.ravenherz.cse.dal.dto.basic.HistoryData;
import com.ravenherz.cse.dal.dto.basic.enums.EventType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/editor")
public class EditorCategoriesController extends AbstractController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditorCategoriesController.class);

    @Autowired
    private ResourceGroupIndex resourceGroupIndex;

    @GetMapping("/categories")
    public String listCategories(HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        response.sendRedirect(request.getContextPath() + EditorTree.CATEGORIES_HREF);
        return null;
    }

    @GetMapping("/category/create")
    public String createCategoryPage(Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }

        addEditorChrome(model, accessor);
        EditorInline.putTreeForCategoryCreate(model, resourceGroupIndex);

        return "/admin/editor-category-create";
    }

    @PostMapping("/category/create")
    public String createCategorySubmit(@RequestParam(value = "itemName", required = false) String itemName,
                                       @RequestParam(value = "navigationTitle", required = false) String navigationTitle,
                                       @RequestParam(value = "navigationDescription", required = false) String navigationDescription,
                                       @RequestParam(value = "displayCount", required = false) String displayCount,
                                       @RequestParam(value = "displayPriority", required = false) String displayPriority,
                                       @RequestParam(value = "isVisible", required = false) String isVisible,
                                       @RequestParam(value = "isActive", required = false) String isActive,
                                       Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
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
        addEditorChrome(model, accessor);
        EditorInline.putTreeForCategoryCreate(model, resourceGroupIndex);
    }

    @GetMapping("/category/edit")
    public String editCategoryPage(@RequestParam(value = "id", required = false) String id,
                                   Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
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
            category = (CategoryEntity) serviceProvider.getCategoryService().getById(CategoryEntity.class, objId);
        } catch (Exception e) {
            error(404, request, response);
            return null;
        }

        if (category == null) {
            error(404, request, response);
            return null;
        }

        model.addAttribute("category", category);
        addEditorChrome(model, accessor);
        EditorInline.putTreeForCategory(model, resourceGroupIndex, category);

        return "/admin/editor-category-edit";
    }

    @PostMapping("/category/save")
    public String saveCategory(@RequestParam(value = "id", required = false) String id,
                               @RequestParam(value = "itemName", required = false) String itemName,
                               @RequestParam(value = "navigationTitle", required = false) String navigationTitle,
                               @RequestParam(value = "navigationDescription", required = false) String navigationDescription,
                               @RequestParam(value = "displayCount", required = false) String displayCount,
                               @RequestParam(value = "displayPriority", required = false) String displayPriority,
                               @RequestParam(value = "isVisible", required = false) String isVisible,
                               @RequestParam(value = "isActive", required = false) String isActive,
                               Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
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
            category = (CategoryEntity) serviceProvider.getCategoryService().getById(CategoryEntity.class, objId);
        } catch (Exception e) {
            error(404, request, response);
            return null;
        }

        if (category == null) {
            error(404, request, response);
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
        newEvents[oldEvents.length] = new Event(EventType.ENTITY_EDITED, LocalDateTime.now(), accessor);
        historyData.setEvents(newEvents);
        category.setHistoryData(historyData);

        serviceProvider.getCategoryService().replace(category);
        resourceGroupIndex.contentChanged();

        response.sendRedirect(request.getContextPath() + "/editor/category/edit?id=" + id.trim());
        return null;
    }

    @PostMapping("/category/delete")
    public String deleteCategory(@RequestParam(value = "id", required = false) String id,
                                 @RequestParam(value = "returnGroup", required = false) String returnGroup,
                                 Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
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
            category = (CategoryEntity) serviceProvider.getCategoryService().getById(CategoryEntity.class, objId);
        } catch (Exception e) {
            error(404, request, response);
            return null;
        }

        if (category == null) {
            error(404, request, response);
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
}

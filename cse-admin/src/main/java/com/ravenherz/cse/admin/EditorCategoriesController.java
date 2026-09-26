package com.ravenherz.cse.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Controller
@RequestMapping("/editor")
public class EditorCategoriesController {

    private final CategoryDesk desk;

    public EditorCategoriesController(CategoryDesk desk) {
        this.desk = desk;
    }

    @GetMapping("/categories")
    public String listCategories(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return desk.list(request, response);
    }

    @GetMapping("/category/create")
    public String createCategoryPage(Model model, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        return desk.createPage(model, request, response);
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
        return desk.create(itemName, navigationTitle, navigationDescription, displayCount, displayPriority,
                isVisible, isActive, model, request, response);
    }

    @GetMapping("/category/edit")
    public String editCategoryPage(@RequestParam(value = "id", required = false) String id, Model model,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        return desk.editPage(id, model, request, response);
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
        return desk.save(id, itemName, navigationTitle, navigationDescription, displayCount, displayPriority,
                isVisible, isActive, model, request, response);
    }

    @PostMapping("/category/delete")
    public String deleteCategory(@RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "returnGroup", required = false) String returnGroup, Model model,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        return desk.delete(id, returnGroup, model, request, response);
    }
}

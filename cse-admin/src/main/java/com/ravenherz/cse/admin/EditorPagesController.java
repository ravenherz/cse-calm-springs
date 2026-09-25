package com.ravenherz.cse.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/editor")
public class EditorPagesController {

    private final PageDesk desk;

    public EditorPagesController(PageDesk desk) {
        this.desk = desk;
    }

    @GetMapping
    public String listPagesRedirect(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return desk.list(request, response);
    }

    @GetMapping("/pages")
    public String listPages(@RequestParam(value = "category", required = false) String category,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        return desk.listInCategory(category, request, response);
    }

    @GetMapping("/create")
    public String createPage(@RequestParam(value = "categoryId", required = false) String categoryId, Model model,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        return desk.createPage(categoryId, model, request, response);
    }

    @GetMapping("/data")
    @ResponseBody
    public String getEditorData(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return desk.editorData(request, response);
    }

    @PostMapping("/create")
    public String createPageSubmit(@RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "header", required = false) String header,
            @RequestParam(value = "subHeader", required = false) String subHeader,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "categoryId", required = false) String categoryId,
            @RequestParam(value = "noTopDisplayImage", defaultValue = "false") boolean noTopDisplayImage,
            @RequestParam(value = "exportPdf", defaultValue = "false") boolean exportPdf, Model model,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        return desk.create(name, header, subHeader, description, tags, categoryId, noTopDisplayImage, exportPdf,
                model, request, response);
    }

    @GetMapping("/edit")
    public String editPage(@RequestParam(value = "name", required = false) String name, Model model,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        return desk.edit(name, model, request, response);
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
            @RequestParam(value = "exportPdf", defaultValue = "false") boolean exportPdf, Model model,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        return desk.save(name, originalName, header, subHeader, description, tags, imageId, resourceGroupId,
                noTopDisplayImage, exportPdf, model, request, response);
    }

    @PostMapping("/item/category")
    public String moveItemCategory(@RequestParam(value = "name", required = false) List<String> names,
            @RequestParam(value = "categoryId", required = false) String categoryId,
            @RequestParam(value = "returnGroup", required = false) String returnGroup, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        return desk.moveCategory(names, categoryId, returnGroup, request, response);
    }

    @PostMapping("/delete")
    public String deletePage(@RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "returnGroup", required = false) String returnGroup, Model model,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        return desk.delete(name, returnGroup, model, request, response);
    }
}

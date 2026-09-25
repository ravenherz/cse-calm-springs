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
public class EditorAlbumsController {

    private final AlbumDesk desk;

    public EditorAlbumsController(AlbumDesk desk) {
        this.desk = desk;
    }

    @GetMapping("/album/create")
    public String createAlbum(@RequestParam(value = "categoryId", required = false) String categoryId, Model model,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        return desk.createPage(categoryId, model, request, response);
    }

    @PostMapping("/album/create")
    public String createAlbumSubmit(@RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "header", required = false) String header,
            @RequestParam(value = "subHeader", required = false) String subHeader,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "categoryId", required = false) String categoryId,
            @RequestParam(value = "resourceGroupId", required = false) String resourceGroupId, Model model,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        return desk.create(name, header, subHeader, description, tags, categoryId, resourceGroupId, model, request,
                response);
    }
}

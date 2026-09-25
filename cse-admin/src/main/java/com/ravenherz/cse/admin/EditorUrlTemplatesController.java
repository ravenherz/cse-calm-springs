package com.ravenherz.cse.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
@RequestMapping("/editor")
public class EditorUrlTemplatesController {

    private final UrlTemplateDesk desk;

    public EditorUrlTemplatesController(UrlTemplateDesk desk) {
        this.desk = desk;
    }

    @GetMapping("/url-templates")
    public String listUrlTemplates(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return desk.list(request, response);
    }

    @GetMapping("/url-template/create")
    public String createPage(Model model, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        return desk.createPage(model, request, response);
    }

    @PostMapping("/url-template/create")
    public String createSubmit(@RequestParam(value = "urlTemplateId", required = false) String urlTemplateId,
            @RequestParam(value = "urlDefaultText", required = false) String urlDefaultText,
            @RequestParam(value = "urlPattern", required = false) String urlPattern,
            @RequestParam(value = "urlImage", required = false) String existingImage,
            @RequestParam(value = "image", required = false) MultipartFile image,
            Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        return desk.create(urlTemplateId, urlDefaultText, urlPattern, existingImage, image, model, request, response);
    }

    @GetMapping("/url-template/edit")
    public String editPage(@RequestParam(value = "id", required = false) String id,
            Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        return desk.editPage(id, model, request, response);
    }

    @PostMapping("/url-template/save")
    public String save(@RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "urlTemplateId", required = false) String urlTemplateId,
            @RequestParam(value = "urlDefaultText", required = false) String urlDefaultText,
            @RequestParam(value = "urlPattern", required = false) String urlPattern,
            @RequestParam(value = "urlImage", required = false) String existingImage,
            @RequestParam(value = "image", required = false) MultipartFile image,
            Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        return desk.save(id, urlTemplateId, urlDefaultText, urlPattern, existingImage, image, model, request,
                response);
    }

    @PostMapping("/url-template/delete")
    public String delete(@RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "returnGroup", required = false) String returnGroup,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        return desk.delete(id, returnGroup, request, response);
    }
}

package com.ravenherz.cse.admin;

import com.ravenherz.cse.core.admin.AdminSectionRecords;
import com.ravenherz.cse.core.admin.AdminSectionSource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/editor/sections")
public class AdminSectionController {

    static final String VIEW = "/admin/editor-section";

    private final AdminSectionCatalog catalog;

    public AdminSectionController(List<AdminSectionSource> sources, List<AdminSectionRecords> records) {
        this.catalog = new AdminSectionCatalog(sources, records);
    }

    @GetMapping("/{sectionId}")
    public String list(@PathVariable("sectionId") String sectionId, HttpServletResponse response, Model model)
            throws IOException {
        return show(catalog.list(sectionId), response, model);
    }

    @GetMapping("/{sectionId}/create")
    public String createForm(@PathVariable("sectionId") String sectionId, HttpServletResponse response, Model model)
            throws IOException {
        return show(catalog.createForm(sectionId), response, model);
    }

    @GetMapping("/{sectionId}/edit/{id}")
    public String editForm(@PathVariable("sectionId") String sectionId, @PathVariable("id") String id,
            HttpServletResponse response, Model model) throws IOException {
        return show(catalog.editForm(sectionId, id), response, model);
    }

    @PostMapping("/{sectionId}/create")
    public String create(@PathVariable("sectionId") String sectionId, @RequestParam Map<String, String> fields,
            HttpServletRequest request, HttpServletResponse response, Model model) throws IOException {
        return finish(catalog.create(sectionId, fields), request, response, model);
    }

    @PostMapping("/{sectionId}/edit/{id}")
    public String update(@PathVariable("sectionId") String sectionId, @PathVariable("id") String id,
            @RequestParam Map<String, String> fields, HttpServletRequest request,
            HttpServletResponse response, Model model) throws IOException {
        return finish(catalog.update(sectionId, id, fields), request, response, model);
    }

    @PostMapping("/{sectionId}/delete/{id}")
    public void delete(@PathVariable("sectionId") String sectionId, @PathVariable("id") String id,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!catalog.delete(sectionId, id)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.sendRedirect(context(request) + "/editor/sections/" + sectionId);
    }

    private static String finish(AdminSectionPage page, HttpServletRequest request,
            HttpServletResponse response, Model model) throws IOException {
        if (page.notFound()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        if (page.errors().isEmpty()) {
            response.sendRedirect(context(request) + "/editor/sections/" + page.section().id());
            return null;
        }
        model.addAttribute("adminSection", page);
        return VIEW;
    }

    private static String show(AdminSectionPage page, HttpServletResponse response, Model model)
            throws IOException {
        if (page.notFound()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        model.addAttribute("adminSection", page);
        return VIEW;
    }

    private static String context(HttpServletRequest request) {
        String context = request.getContextPath();
        return context == null ? "" : context;
    }
}

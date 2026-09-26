package com.ravenherz.cse.controller;

import com.ravenherz.cse.core.admin.AdminSection;
import com.ravenherz.cse.core.admin.AdminSectionRecords;
import com.ravenherz.cse.core.admin.AdminSectionSource;
import com.ravenherz.cse.present.EditorTree;
import com.ravenherz.cse.present.ResourceGroupDisplayDTO;
import com.ravenherz.cse.present.ResourceGroupIndex;
import com.ravenherz.cse.present.ResourceTreeFile;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Section screens are catalog folders. Rows hang on the folder using the section's tree label.
 */
@ControllerAdvice
public class EditorSectionTreeAdvice {

    private final ResourceGroupIndex resourceGroupIndex;
    private final List<AdminSectionSource> sources;
    private final List<AdminSectionRecords> records;

    public EditorSectionTreeAdvice(ResourceGroupIndex resourceGroupIndex, List<AdminSectionSource> sources,
            List<AdminSectionRecords> records) {
        this.resourceGroupIndex = resourceGroupIndex;
        this.sources = sources == null ? List.of() : sources;
        this.records = records == null ? List.of() : records;
    }

    @ModelAttribute
    public void sectionTree(HttpServletRequest request, Model model) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && uri.startsWith(context)) {
            uri = uri.substring(context.length());
        }
        if (uri == null || uri.isBlank()) {
            return;
        }
        if (uri.contains("/editor/scripting/runs")) {
            EditorInline.putTree(model, resourceGroupIndex, EditorTree.SCRIPTS_ID, null);
            return;
        }
        if (!uri.contains("/editor/sections/")) {
            return;
        }
        String rest = uri.substring(uri.indexOf("/editor/sections/") + "/editor/sections/".length());
        String sectionId = rest.split("/", 2)[0];
        if (sectionId.isBlank()) {
            return;
        }
        String leafId = null;
        String marker = "/edit/";
        int edit = rest.indexOf(marker);
        if (edit >= 0) {
            String id = rest.substring(edit + marker.length()).split("/", 2)[0];
            if (!id.isBlank()) {
                leafId = "section-" + id;
            }
        }
        String folderId = "content-" + sectionId;
        EditorInline.putTree(model, resourceGroupIndex, folderId, leafId);
        Object roots = model.getAttribute("resourceGroupTree");
        if (!(roots instanceof List<?> nodes) || nodes.isEmpty()) {
            return;
        }
        @SuppressWarnings("unchecked")
        ResourceGroupDisplayDTO folder = EditorTree.find((List<ResourceGroupDisplayDTO>) nodes, folderId);
        if (folder == null) {
            return;
        }
        AdminSection section = section(sectionId);
        AdminSectionRecords store = records(sectionId);
        if (section == null || store == null) {
            return;
        }
        List<ResourceTreeFile> files = new ArrayList<>();
        for (Map<String, String> row : store.list()) {
            String id = row.get("id");
            if (id == null || id.isBlank()) {
                continue;
            }
            files.add(new ResourceTreeFile("section-" + id, section.treeLabel(row),
                    "/editor/sections/" + sectionId + "/edit/" + id)
                    .withGlyph(section.presentation().treeGlyph().token()));
        }
        folder.setTreeFiles(files);
    }

    private AdminSection section(String sectionId) {
        for (AdminSectionSource source : sources) {
            AdminSection section = source.section();
            if (section != null && sectionId.equals(section.id())) {
                return section;
            }
        }
        return null;
    }

    private AdminSectionRecords records(String sectionId) {
        for (AdminSectionRecords store : records) {
            if (sectionId.equals(store.sectionId())) {
                return store;
            }
        }
        return null;
    }
}

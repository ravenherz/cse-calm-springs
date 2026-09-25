package com.ravenherz.cse.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/editor")
public class EditorResourcesController {

    private final ResourceDesk desk;

    public EditorResourcesController(ResourceDesk desk) {
        this.desk = desk;
    }

    @GetMapping("/resources")
    public String resourcesPage(@RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "notice", required = false) String notice,
            @RequestParam(value = "group", required = false) String group, Model model, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        return desk.page(error, notice, group, model, request, response);
    }

    @GetMapping("/content")
    public void contentRedirect(HttpServletRequest request, HttpServletResponse response) throws IOException {
        desk.contentRedirect(request, response);
    }

    @PostMapping("/resources/upload")
    public String uploadResource(@RequestParam("resourceId") String resourceId, @RequestParam("file") MultipartFile file,
            @RequestParam(value = "metadata", required = false) String metadataJson,
            @RequestParam(value = "imageDescription", required = false) String imageDescription,
            @RequestParam(value = "groupId", required = false) String groupId, Model model, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        return desk.upload(resourceId, file, metadataJson, imageDescription, groupId, model, request, response);
    }

    @GetMapping("/resources/download")
    public void downloadResource(@RequestParam("pathPublic") String pathPublic, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        desk.download(pathPublic, request, response);
    }

    @GetMapping(value = "/resources/tree-preview/{id}", produces = MediaType.IMAGE_JPEG_VALUE)
    public void treePreview(@PathVariable("id") String id, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        desk.treePreview(id, request, response);
    }

    @PostMapping("/resources/delete")
    public String deleteResource(@RequestParam("pathPublic") String pathPublic, Model model, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        return desk.delete(pathPublic, model, request, response);
    }

    @PostMapping("/resources/description")
    public String updateResourceDescription(@RequestParam("pathPublic") String pathPublic,
            @RequestParam(value = "imageDescription", required = false) String imageDescription, Model model,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        return desk.updateDescription(pathPublic, imageDescription, model, request, response);
    }

    @PostMapping("/resources/access")
    public String updateResourceAccess(@RequestParam("pathPublic") String pathPublic, Model model,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        return desk.updateAccess(pathPublic, model, request, response);
    }

    @PostMapping("/resources/group/access")
    public String updateResourceGroupAccess(@RequestParam("groupId") String groupId, Model model,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        return desk.updateGroupAccess(groupId, model, request, response);
    }

    @PostMapping("/resources/group/assign")
    public String assignResourceToGroup(@RequestParam("resourceId") List<String> resourceIds,
            @RequestParam(value = "groupId", required = false) String groupId, Model model, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        return desk.assignGroup(resourceIds, groupId, model, request, response);
    }

    @PostMapping("/resources/group/move")
    public String moveResourceGroup(@RequestParam("groupId") List<String> groupIds,
            @RequestParam(value = "parentId", required = false) String parentId, Model model, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        return desk.moveGroup(groupIds, parentId, model, request, response);
    }

    @PostMapping("/resources/group/delete")
    public String deleteResourceGroup(@RequestParam("groupId") String groupId, Model model, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        return desk.deleteGroup(groupId, model, request, response);
    }

    @PostMapping("/resources/group/create")
    public String createResourceGroup(@RequestParam("humanReadableId") String humanReadableId,
            @RequestParam(value = "parentId", required = false) String parentId, Model model, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        return desk.createGroup(humanReadableId, parentId, model, request, response);
    }

    @PostMapping("/resources/group/rename")
    public String renameResourceGroup(@RequestParam("groupId") String groupId,
            @RequestParam("humanReadableId") String humanReadableId, Model model, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        return desk.renameFolder(groupId, humanReadableId, model, request, response);
    }

    @PostMapping("/batch-delete")
    public String batchDelete(@RequestParam(value = "kind", required = false) List<String> kinds,
            @RequestParam(value = "id", required = false) List<String> ids, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        return desk.batchDelete(kinds, ids, request, response);
    }

    @PostMapping("/catalog/rename")
    public String renameCatalogItem(@RequestParam("kind") String kind, @RequestParam("id") String id,
            @RequestParam("name") String name, Model model, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        return desk.rename(kind, id, name, model, request, response);
    }
}

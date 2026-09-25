package com.ravenherz.cse.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Resource tree, upload, and catalog edits. The WAR stores the files.
 */
public interface ResourceDesk {

    String page(String error, String notice, String group, Model model, HttpServletRequest request,
            HttpServletResponse response) throws IOException;

    void contentRedirect(HttpServletRequest request, HttpServletResponse response) throws IOException;

    String upload(String resourceId, MultipartFile file, String metadataJson, String imageDescription, String groupId,
            Model model, HttpServletRequest request, HttpServletResponse response) throws IOException;

    void download(String pathPublic, HttpServletRequest request, HttpServletResponse response) throws IOException;

    void treePreview(String id, HttpServletRequest request, HttpServletResponse response) throws IOException;

    String delete(String pathPublic, Model model, HttpServletRequest request, HttpServletResponse response)
            throws IOException;

    String updateDescription(String pathPublic, String imageDescription, Model model, HttpServletRequest request,
            HttpServletResponse response) throws IOException;

    String updateAccess(String pathPublic, Model model, HttpServletRequest request, HttpServletResponse response)
            throws IOException;

    String updateGroupAccess(String groupId, Model model, HttpServletRequest request, HttpServletResponse response)
            throws IOException;

    String assignGroup(List<String> resourceIds, String groupId, Model model, HttpServletRequest request,
            HttpServletResponse response) throws IOException;

    String moveGroup(List<String> groupIds, String parentId, Model model, HttpServletRequest request,
            HttpServletResponse response) throws IOException;

    String deleteGroup(String groupId, Model model, HttpServletRequest request, HttpServletResponse response)
            throws IOException;

    String createGroup(String humanReadableId, String parentId, Model model, HttpServletRequest request,
            HttpServletResponse response) throws IOException;

    String renameFolder(String groupId, String humanReadableId, Model model, HttpServletRequest request,
            HttpServletResponse response) throws IOException;

    String batchDelete(List<String> kinds, List<String> ids, HttpServletRequest request, HttpServletResponse response)
            throws IOException;

    String rename(String kind, String id, String name, Model model, HttpServletRequest request,
            HttpServletResponse response) throws IOException;
}

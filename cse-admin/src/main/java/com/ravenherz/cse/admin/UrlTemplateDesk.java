package com.ravenherz.cse.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * URL template create, edit, and delete. The WAR stores the rows.
 */
public interface UrlTemplateDesk {

    String list(HttpServletRequest request, HttpServletResponse response) throws IOException;

    String createPage(Model model, HttpServletRequest request, HttpServletResponse response) throws IOException;

    String create(String urlTemplateId, String urlDefaultText, String urlPattern, String existingImage,
            MultipartFile image, Model model, HttpServletRequest request, HttpServletResponse response)
            throws IOException;

    String editPage(String id, Model model, HttpServletRequest request, HttpServletResponse response)
            throws IOException;

    String save(String id, String urlTemplateId, String urlDefaultText, String urlPattern, String existingImage,
            MultipartFile image, Model model, HttpServletRequest request, HttpServletResponse response)
            throws IOException;

    String delete(String id, String returnGroup, HttpServletRequest request, HttpServletResponse response)
            throws IOException;
}

package com.ravenherz.cse.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;

import java.io.IOException;

/**
 * Album create. The WAR stores the item and fills the edit form.
 */
public interface AlbumDesk {

    String createPage(String categoryId, Model model, HttpServletRequest request, HttpServletResponse response)
            throws IOException;

    String create(String name, String header, String subHeader, String description, String tags, String categoryId,
            String resourceGroupId, Model model, HttpServletRequest request, HttpServletResponse response)
            throws IOException;
}

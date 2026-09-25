package com.ravenherz.cse.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;

import java.io.IOException;
import java.util.List;

/**
 * Page and album edit routes. The WAR stores the items.
 */
public interface PageDesk {

    String list(HttpServletRequest request, HttpServletResponse response) throws IOException;

    String listInCategory(String category, HttpServletRequest request, HttpServletResponse response)
            throws IOException;

    String createPage(String categoryId, Model model, HttpServletRequest request, HttpServletResponse response)
            throws IOException;

    String editorData(HttpServletRequest request, HttpServletResponse response) throws IOException;

    String create(String name, String header, String subHeader, String description, String tags, String categoryId,
            boolean noTopDisplayImage, boolean exportPdf, Model model, HttpServletRequest request,
            HttpServletResponse response) throws IOException;

    String edit(String name, Model model, HttpServletRequest request, HttpServletResponse response) throws IOException;

    String save(String name, String originalName, String header, String subHeader, String description, String tags,
            String imageId, String resourceGroupId, boolean noTopDisplayImage, boolean exportPdf, Model model,
            HttpServletRequest request, HttpServletResponse response) throws IOException;

    String moveCategory(List<String> names, String categoryId, String returnGroup, HttpServletRequest request,
            HttpServletResponse response) throws IOException;

    String delete(String name, String returnGroup, Model model, HttpServletRequest request,
            HttpServletResponse response) throws IOException;
}

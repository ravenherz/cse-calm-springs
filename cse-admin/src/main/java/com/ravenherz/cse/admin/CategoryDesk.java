package com.ravenherz.cse.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;

import java.io.IOException;

/**
 * Category create, edit, and delete. The WAR stores the rows and their pages.
 */
public interface CategoryDesk {

    String list(HttpServletRequest request, HttpServletResponse response) throws IOException;

    String createPage(Model model, HttpServletRequest request, HttpServletResponse response) throws IOException;

    String create(String itemName, String navigationTitle, String navigationDescription, String displayCount,
            String displayPriority, String isVisible, String isActive, Model model, HttpServletRequest request,
            HttpServletResponse response) throws IOException;

    String editPage(String id, Model model, HttpServletRequest request, HttpServletResponse response)
            throws IOException;

    String save(String id, String itemName, String navigationTitle, String navigationDescription,
            String displayCount, String displayPriority, String isVisible, String isActive, Model model,
            HttpServletRequest request, HttpServletResponse response) throws IOException;

    String delete(String id, String returnGroup, Model model, HttpServletRequest request,
            HttpServletResponse response) throws IOException;
}

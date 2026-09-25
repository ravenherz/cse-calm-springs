package com.ravenherz.cse.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;

import java.io.IOException;
import java.util.List;

/**
 * Playlist create, edit, and delete. The WAR stores the rows and their tracks.
 */
public interface PlaylistDesk {

    String list(HttpServletRequest request, HttpServletResponse response) throws IOException;

    String createPage(Model model, HttpServletRequest request, HttpServletResponse response) throws IOException;

    String create(String playlistId, String title, String description, List<String> resourceIds,
            List<String> titleOverrides, List<String> artistOverrides, String coverResourceId, Model model,
            HttpServletRequest request, HttpServletResponse response) throws IOException;

    String editPage(String id, Model model, HttpServletRequest request, HttpServletResponse response)
            throws IOException;

    String save(String id, String playlistId, String title, String description, List<String> resourceIds,
            List<String> titleOverrides, List<String> artistOverrides, String coverResourceId, Model model,
            HttpServletRequest request, HttpServletResponse response) throws IOException;

    String delete(String id, String returnGroup, HttpServletRequest request, HttpServletResponse response)
            throws IOException;
}

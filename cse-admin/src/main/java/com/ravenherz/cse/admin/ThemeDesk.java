package com.ravenherz.cse.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Theme pack upload, activate, reorder, and delete. The WAR installs the pack.
 */
public interface ThemeDesk {

    String list(HttpServletRequest request, HttpServletResponse response) throws IOException;

    String upload(MultipartFile file, String returnGroup, HttpServletRequest request, HttpServletResponse response)
            throws IOException;

    String activate(String themeId, String returnGroup, HttpServletRequest request, HttpServletResponse response)
            throws IOException;

    String reorder(List<String> themeIds, String returnGroup, HttpServletRequest request, HttpServletResponse response)
            throws IOException;

    String delete(String themeId, String returnGroup, HttpServletRequest request, HttpServletResponse response)
            throws IOException;
}

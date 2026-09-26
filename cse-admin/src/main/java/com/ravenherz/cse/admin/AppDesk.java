package com.ravenherz.cse.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * App pack upload, delete, and store limits. The WAR installs the pack.
 */
public interface AppDesk {

    String list(HttpServletRequest request, HttpServletResponse response) throws IOException;

    String upload(String slug, String returnGroup, MultipartFile file, HttpServletRequest request,
            HttpServletResponse response) throws IOException;

    String delete(String slug, String returnGroup, HttpServletRequest request, HttpServletResponse response)
            throws IOException;

    String store(String slug, boolean storeEnabled, boolean storeOpen, Integer maxDataKb, Integer maxDocs,
            Integer maxBytesMb, String returnGroup, HttpServletRequest request, HttpServletResponse response)
            throws IOException;
}

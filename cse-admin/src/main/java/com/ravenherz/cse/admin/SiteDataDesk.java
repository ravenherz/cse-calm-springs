package com.ravenherz.cse.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Site archive export and replace. The WAR reads Mongo and writes the zip.
 */
public interface SiteDataDesk {

    String open(Model model, HttpServletRequest request, HttpServletResponse response, String error)
            throws IOException;

    void export(HttpServletRequest request, HttpServletResponse response) throws IOException;

    String importArchive(MultipartFile file, String confirm, Model model, HttpServletRequest request,
            HttpServletResponse response) throws IOException;
}

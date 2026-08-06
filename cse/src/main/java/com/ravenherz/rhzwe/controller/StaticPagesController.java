package com.ravenherz.rhzwe.controller;

import com.ravenherz.rhzwe.constants.Strings;
import com.ravenherz.rhzwe.util.io.ServletFile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Controller
@Scope(value = "singleton")
@RequestMapping("/static-pages")
public class StaticPagesController extends AbstractController {

    private static final Logger LOGGER = LoggerFactory.getLogger(StaticPagesController.class);

    private static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
            Map.entry("html", "text/html; charset=UTF-8"),
            Map.entry("css", "text/css; charset=UTF-8"),
            Map.entry("js", "application/javascript; charset=UTF-8"),
            Map.entry("json", "application/json; charset=UTF-8"),
            Map.entry("txt", "text/plain; charset=UTF-8"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"),
            Map.entry("svg", "image/svg+xml"),
            Map.entry("webp", "image/webp"),
            Map.entry("ico", "image/x-icon"),
            Map.entry("woff", "font/woff"),
            Map.entry("woff2", "font/woff2"),
            Map.entry("ttf", "font/ttf"),
            Map.entry("eot", "application/vnd.ms-fontobject")
    );

    @GetMapping("/**")
    public @ResponseBody byte[] getResource(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        String relativePath = request.getRequestURI()
                .replaceFirst(request.getContextPath(), "")
                .replaceFirst("^/static-pages", "");
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        if (relativePath.contains("..")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        if (relativePath.isEmpty() || relativePath.endsWith("/")) {
            relativePath += "index";
        }

        String path = Strings.PATH_STATIC_PAGES + relativePath;
        String extension = getExtension(path);
        if (extension == null) {
            path += Strings.EXTENSION_HTML;
            extension = "html";
        }

        File file = ServletFile.classPathFile(path);
        if (file == null || !file.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        response.setContentType(CONTENT_TYPES.getOrDefault(extension, "application/octet-stream"));
        return FileUtils.readFileToByteArray(file);
    }

    private String getExtension(String path) {
        int lastSlash = path.lastIndexOf('/');
        int lastDot = path.lastIndexOf('.');
        if (lastDot <= lastSlash) {
            return null;
        }
        return path.substring(lastDot + 1);
    }
}

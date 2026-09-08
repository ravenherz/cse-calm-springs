package com.ravenherz.cse.controller;

import com.ravenherz.cse.constants.Strings;
import com.ravenherz.cse.util.io.ServletFile;
import com.ravenherz.cse.util.staticapps.StaticAppDeployer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Controller
@Scope(value = "singleton")
@RequestMapping("/static-pages")
public class StaticPagesController extends AbstractController {

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
            Map.entry("eot", "application/vnd.ms-fontobject"),
            Map.entry("webmanifest", "application/manifest+json")
    );

    @Autowired
    private StaticAppDeployer staticAppDeployer;

    @GetMapping("/**")
    public @ResponseBody byte[] getResource(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        String relativePath = request.getRequestURI()
                .replaceFirst(request.getContextPath(), "")
                .replaceFirst("^/static-pages", "");
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        if (siteReady.isConfigured() && isInstallerPath(relativePath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
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
            relativePath += Strings.EXTENSION_HTML;
            extension = "html";
        }

        Path diskFile = staticAppDeployer.resolvePublicFile(relativePath);
        if (diskFile != null) {
            response.setContentType(CONTENT_TYPES.getOrDefault(extension, "application/octet-stream"));
            if (isProductLogo(relativePath)) {
                response.setHeader("Cache-Control", "private, no-cache");
            }
            return Files.readAllBytes(diskFile);
        }

        byte[] classpathBytes = ServletFile.readBytes(path);
        if (classpathBytes == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        response.setContentType(CONTENT_TYPES.getOrDefault(extension, "application/octet-stream"));
        return classpathBytes;
    }

    private static boolean isProductLogo(String relativePath) {
        if (relativePath == null) {
            return false;
        }
        String lower = relativePath.replace('\\', '/').toLowerCase();
        return lower.equals(StaticAppDeployer.PRODUCT_LOGO)
                || lower.endsWith("/" + StaticAppDeployer.PRODUCT_LOGO);
    }

    private static boolean isInstallerPath(String relativePath) {
        String path = relativePath == null ? "" : relativePath;
        return path.equals(StaticAppDeployer.INSTALLER_SLUG)
                || path.startsWith(StaticAppDeployer.INSTALLER_SLUG + "/");
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

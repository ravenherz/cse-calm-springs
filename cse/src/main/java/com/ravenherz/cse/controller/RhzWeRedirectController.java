package com.ravenherz.cse.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

/**
 * Legacy context path. Instances used to be deployed at {@code /rhz-we}.
 * {@code {context}/rhz-we/**} moves to {@code {context}/**}.
 */
@Controller
@RequestMapping("/rhz-we")
public class RhzWeRedirectController {

    static final String OLD_PREFIX = "/rhz-we";

    @RequestMapping({"", "/", "/**"})
    public void redirect(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String rest = suffixAfterPrefix(request);
        if (rest.contains("..") || rest.startsWith("//") || rest.indexOf('\\') >= 0) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String target = contextPath(request) + rest;
        if (target.isEmpty()) {
            target = "/";
        }
        String query = request.getQueryString();
        if (query != null && !query.isBlank()) {
            target += "?" + query;
        }
        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        response.setHeader("Location", target);
    }

    static String suffixAfterPrefix(HttpServletRequest request) {
        String context = contextPath(request);
        String uri = request.getRequestURI();
        if (uri == null) {
            return "";
        }
        String marker = context + OLD_PREFIX;
        if (uri.startsWith(marker)) {
            return uri.substring(marker.length());
        }
        int at = uri.indexOf(OLD_PREFIX);
        if (at >= 0) {
            return uri.substring(at + OLD_PREFIX.length());
        }
        return "";
    }

    private static String contextPath(HttpServletRequest request) {
        String context = request.getContextPath();
        return context == null ? "" : context;
    }
}

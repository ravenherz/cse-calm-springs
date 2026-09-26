package com.ravenherz.cse.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Host robots file. Google fetches {@code /robots.txt} on the host, which is this
 * controller when the WAR is deployed at ROOT. Private areas of a sibling context
 * are listed with a {@code /*} prefix so one host file covers every instance.
 */
@Controller
public class RobotsController {

    static final String[] PRIVATE = {
            "/editor",
            "/install",
            "/account",
            "/rest",
            "/app-data",
            "/content-private",
            "/content-public/admin",
            "/error",
            "/apps/login",
            "/apps/setup",
            "/apps/admin",
            "/static-pages"
    };

    @GetMapping(value = "/robots.txt", produces = "text/plain")
    public void robots(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/plain; charset=UTF-8");
        response.getWriter().write(body(request.getContextPath()));
    }

    static String body(String contextPath) {
        String context = contextPath == null || contextPath.isBlank() || "/".equals(contextPath)
                ? ""
                : (contextPath.endsWith("/") ? contextPath.substring(0, contextPath.length() - 1) : contextPath);
        StringBuilder text = new StringBuilder();
        text.append("User-agent: *\n");
        text.append("Allow: ").append(context).append("/\n");
        for (String path : PRIVATE) {
            text.append("Disallow: ").append(context).append(path).append('\n');
        }
        if (context.isEmpty()) {
            for (String path : PRIVATE) {
                text.append("Disallow: /*").append(path).append('\n');
            }
        }
        return text.toString();
    }
}

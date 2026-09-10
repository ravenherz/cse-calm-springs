package com.ravenherz.cse.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;

final class CseSecurityResponses {

    private CseSecurityResponses() {
    }

    static void unauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        unauthorized(request, response, "Sign in required");
    }

    static void unauthorized(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        if (wantsJson(request)) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, message);
            return;
        }
        response.sendRedirect(request.getContextPath() + "/?error=401");
    }

    static void forbidden(HttpServletRequest request, HttpServletResponse response) throws IOException {
        forbidden(request, response, "Not allowed");
    }

    static void forbidden(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        if (wantsJson(request)) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, message);
            return;
        }
        response.sendRedirect(request.getContextPath() + "/?error=403");
    }

    static boolean wantsJson(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri != null && uri.contains("/editor/logs/tail")) {
            return true;
        }
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE);
    }

    private static void writeJson(HttpServletResponse response, int status, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":" + status + ",\"message\":\"" + jsonEscape(message) + "\"}");
    }

    private static String jsonEscape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

package com.ravenherz.cse.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;

final class CseSecurityResponses {

    private CseSecurityResponses() {
    }

    static void unauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (wantsJson(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        response.sendRedirect(request.getContextPath() + "/?error=401");
    }

    static void forbidden(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (wantsJson(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
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
}

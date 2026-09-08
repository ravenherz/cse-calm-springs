package com.ravenherz.cse.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;

import java.io.IOException;

final class CseAccessDeniedHandler implements AccessDeniedHandler {

    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger(CseAccessDeniedHandler.class);

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        if (accessDeniedException instanceof InvalidCsrfTokenException
                || accessDeniedException instanceof MissingCsrfTokenException) {
            String[] parameters = request.getParameterValues(CseCookieCsrfTokenRepository.PARAMETER_NAME);
            LOGGER.warn("CSRF rejected {} {} cookie={} header={} params={} names={} ({})",
                    request.getMethod(),
                    request.getRequestURI(),
                    hasCsrfCookie(request),
                    request.getHeader(CseCookieCsrfTokenRepository.HEADER_NAME) != null,
                    parameters == null ? 0 : parameters.length,
                    cookieNames(request),
                    accessDeniedException.getClass().getSimpleName());
        } else {
            LOGGER.warn("Access denied {} {} ({})",
                    request.getMethod(),
                    request.getRequestURI(),
                    accessDeniedException.getClass().getSimpleName());
        }
        CseSecurityResponses.forbidden(request, response);
    }

    private static boolean hasCsrfCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return false;
        }
        for (Cookie cookie : cookies) {
            if (CseCookieCsrfTokenRepository.COOKIE_NAME.equals(cookie.getName())
                    && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return true;
            }
        }
        return false;
    }

    private static String cookieNames(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return "";
        }
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < cookies.length; i++) {
            if (i > 0) {
                names.append(',');
            }
            names.append(cookies[i].getName());
        }
        return names.toString();
    }
}

package com.ravenherz.cse.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import java.util.UUID;

/**
 * Same cookie flags as {@code AuthSupport}: context-path, {@code Secure} when
 * {@code X-Forwarded-Proto} is https, {@code SameSite=Lax}, 30-day max-age.
 * HttpOnly is false so {@code cse-csrf.js} can read the value.
 * A leftover {@code Path=/} cookie is expired only when clearing, never in the
 * same {@code Set-Cookie} batch as a new token (Chrome treats {@code Max-Age=0}
 * {@code Path=/} as deleting every cookie with that name).
 */
final class CseCookieCsrfTokenRepository implements CsrfTokenRepository {

    static final String COOKIE_NAME = "XSRF-TOKEN";
    static final String HEADER_NAME = "X-XSRF-TOKEN";
    static final String PARAMETER_NAME = "_csrf";
    static final int COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 30;

    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        return new DefaultCsrfToken(HEADER_NAME, PARAMETER_NAME, UUID.randomUUID().toString());
    }

    @Override
    public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
        String path = cookiePath(request);
        if (token == null || token.getToken() == null || token.getToken().isEmpty()) {
            addCookie(request, response, "", 0, path);
            if (!"/".equals(path)) {
                addCookie(request, response, "", 0, "/");
            }
            return;
        }
        addCookie(request, response, token.getToken(), COOKIE_MAX_AGE_SECONDS, path);
    }

    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName()) && cookie.getValue() != null
                    && !cookie.getValue().isBlank()) {
                return new DefaultCsrfToken(HEADER_NAME, PARAMETER_NAME, cookie.getValue());
            }
        }
        return null;
    }

    private static void addCookie(HttpServletRequest request, HttpServletResponse response,
            String value, int maxAge, String path) {
        Cookie cookie = new Cookie(COOKIE_NAME, value == null ? "" : value);
        cookie.setHttpOnly(false);
        cookie.setSecure(isHttps(request));
        cookie.setPath(path);
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private static boolean isHttps(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }
        String proto = request.getHeader("X-Forwarded-Proto");
        return proto != null && proto.toLowerCase().startsWith("https");
    }

    private static String cookiePath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        if (contextPath == null || contextPath.isBlank()) {
            return "/";
        }
        return contextPath;
    }
}

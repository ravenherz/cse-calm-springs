package com.ravenherz.cse.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * CSRF cookie is {@code Path=/} so {@code /app-data} receives it when the page is
 * under {@code /static-pages} (bootRun at {@code /}) or the WAR context is omitted.
 * HttpOnly is false so JS can echo the value as {@code X-XSRF-TOKEN}.
 * A leftover context-path cookie is expired only when clearing, never in the
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
        if (token == null || token.getToken() == null || token.getToken().isEmpty()) {
            addCookie(request, response, "", 0, "/");
            String contextPath = cookiePath(request);
            if (!"/".equals(contextPath)) {
                addCookie(request, response, "", 0, contextPath);
            }
            return;
        }
        addCookie(request, response, token.getToken(), COOKIE_MAX_AGE_SECONDS, "/");
    }

    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        List<String> values = cookieValues(request);
        if (values.isEmpty()) {
            return null;
        }
        String submitted = submittedToken(request);
        if (submitted != null) {
            for (String value : values) {
                if (value.equals(submitted) || value.equals(decode(submitted))) {
                    return new DefaultCsrfToken(HEADER_NAME, PARAMETER_NAME, value);
                }
            }
        }
        return new DefaultCsrfToken(HEADER_NAME, PARAMETER_NAME, values.get(0));
    }

    /**
     * A leftover {@code Path=/} cookie and the context-path cookie both arrive as
     * {@code XSRF-TOKEN}. {@code document.cookie} and the {@code Cookie} header do not
     * always list them in the same order, so the header must pick the value.
     */
    private static List<String> cookieValues(HttpServletRequest request) {
        List<String> values = new ArrayList<>();
        if (request.getCookies() == null) {
            return values;
        }
        for (Cookie cookie : request.getCookies()) {
            if (!COOKIE_NAME.equals(cookie.getName())) {
                continue;
            }
            String value = decode(cookie.getValue());
            if (value != null && !value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private static String submittedToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER_NAME);
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        String[] parameters = request.getParameterValues(PARAMETER_NAME);
        if (parameters == null) {
            return null;
        }
        for (String parameter : parameters) {
            if (parameter != null && !parameter.isBlank()) {
                return parameter.trim();
            }
        }
        return null;
    }

    private static String decode(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return URLDecoder.decode(trimmed, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return trimmed;
        }
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

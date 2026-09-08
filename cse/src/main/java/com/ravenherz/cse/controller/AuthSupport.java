package com.ravenherz.cse.controller;

import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData.AccountSession;
import com.ravenherz.cse.util.StringUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.logging.Logger;

@Component
public class AuthSupport {

    private static final Logger LOGGER = Logger.getLogger(AuthSupport.class.getName());
    private static final int AUTH_COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 30;

    private final ServiceProvider serviceProvider;

    public AuthSupport(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    public AccountEntity getAccessor(HttpServletRequest request, HttpServletResponse response) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if ("auth-login".equals(cookie.getName()) && cookie.getValue() != null) {
                AccountEntity accountEntity;
                try {
                    accountEntity = serviceProvider.getAccountService()
                            .getByLogin(cookie.getValue());
                } catch (RuntimeException ex) {
                    LOGGER.warning("Session lookup failed: " + ex.getMessage());
                    return null;
                }
                if (accountEntity == null) {
                    deleteAuthCookies(request, response);
                    LOGGER.warning(String.format("Someone (%s) is attempting to get access illegally",
                            clientIp(request)));
                    return null;
                }
                if (accountEntity.getAccountData() == null
                        || !accountEntity.getAccountData().isLoginable()) {
                    deleteAuthCookies(request, response);
                    return null;
                }
                try {
                    String remoteAddress = clientIp(request);
                    String userAgent = request.getHeader("User-Agent");
                    String sessionToken = Objects.requireNonNull(
                            getCookieByKey(request, "auth-session"));
                    AccountSession fakeAccountSession = new AccountSession(remoteAddress, userAgent,
                            sessionToken, null);
                    boolean matched = accountEntity.getAccountData().getSessions() != null
                            && accountEntity.getAccountData().getSessions().stream()
                            .anyMatch(accountSession -> !Boolean.TRUE.equals(accountSession.getFinished())
                                    && accountSession.checkFakeEquality(fakeAccountSession));
                    if (matched) {
                        return accountEntity;
                    }
                    deleteAuthCookies(request, response);
                } catch (NullPointerException ex) {
                    LOGGER.warning(ex.getMessage());
                    return null;
                }
            }
        }
        return null;
    }

    public boolean issueSession(AccountEntity entity, HttpServletRequest request,
            HttpServletResponse response) {
        if (entity == null || entity.getAccountData() == null) {
            return false;
        }
        String sessionToken = StringUtils.generateRandomToken();
        AccountSession accountSession = new AccountSession(clientIp(request),
                request.getHeader("User-Agent"), sessionToken, LocalDateTime.now());
        if (entity.getAccountData().getSessions() == null) {
            entity.getAccountData().setSessions(new HashSet<>());
        }
        entity.getAccountData().getSessions().add(accountSession);
        if (!serviceProvider.getAccountService().replace(entity)) {
            return false;
        }
        addAuthCookies(request, response, entity.getAccountData().getLogin(), sessionToken);
        return true;
    }

    public String getCookieByKey(HttpServletRequest request, String key) {
        if (request.getCookies() != null && key != null) {
            for (Cookie cookie : request.getCookies()) {
                if (key.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public void deleteAuthCookies(HttpServletRequest request, HttpServletResponse response) {
        expireAuthCookie(request, response, "auth-login", cookiePath(request));
        expireAuthCookie(request, response, "auth-session", cookiePath(request));
        expireAuthCookie(request, response, "auth-login", "/");
        expireAuthCookie(request, response, "auth-session", "/");
    }

    public void addAuthCookies(HttpServletRequest request, HttpServletResponse response,
            String login, String sessionToken) {
        addAuthCookie(request, response, "auth-login", login, AUTH_COOKIE_MAX_AGE_SECONDS,
                cookiePath(request));
        addAuthCookie(request, response, "auth-session", sessionToken, AUTH_COOKIE_MAX_AGE_SECONDS,
                cookiePath(request));
    }

    public String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma < 0 ? forwarded : forwarded.substring(0, comma)).trim();
        }
        return request.getRemoteAddr();
    }

    public void redirectError(int code, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendRedirect(request.getContextPath() + String.format("/?error=%s", code));
    }

    private void expireAuthCookie(HttpServletRequest request, HttpServletResponse response,
            String name, String path) {
        addAuthCookie(request, response, name, "", 0, path);
    }

    private void addAuthCookie(HttpServletRequest request, HttpServletResponse response,
            String name, String value, int maxAge, String path) {
        Cookie cookie = new Cookie(name, value == null ? "" : value);
        cookie.setHttpOnly(true);
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

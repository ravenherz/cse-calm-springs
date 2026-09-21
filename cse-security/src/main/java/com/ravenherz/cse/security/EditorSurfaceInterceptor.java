package com.ravenherz.cse.security;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.role.CapabilityIds;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public final class EditorSurfaceInterceptor implements HandlerInterceptor {

    private final AccountAccessor authSupport;
    private final CapabilityService capabilities;

    public EditorSurfaceInterceptor(AccountAccessor authSupport, CapabilityService capabilities) {
        this.authSupport = authSupport;
        this.capabilities = capabilities;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String capability = capabilityFor(request);
        if (capability == null || CapabilityIds.EDITOR_ACCESS.equals(capability)) {
            return true;
        }
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (capabilities.allows(accessor, capability)) {
            return true;
        }
        CseSecurityResponses.forbidden(request, response);
        return false;
    }

    static String capabilityFor(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath() == null ? "" : request.getContextPath();
        String path = uri == null ? "" : uri.substring(context.length());
        if (!path.startsWith("/editor")) {
            return null;
        }
        if (path.startsWith("/editor/roles")) {
            return CapabilityIds.EDITOR_ROLES;
        }
        if (path.startsWith("/editor/accounts") || path.startsWith("/editor/account/")) {
            return CapabilityIds.EDITOR_ACCOUNTS;
        }
        if (path.startsWith("/editor/settings")) {
            return CapabilityIds.EDITOR_SETTINGS;
        }
        if (path.startsWith("/editor/site-data")) {
            return CapabilityIds.EDITOR_SITE_DATA;
        }
        if (path.startsWith("/editor/logs")) {
            return CapabilityIds.EDITOR_LOGS;
        }
        if (path.startsWith("/editor/instance")) {
            return CapabilityIds.EDITOR_INSTANCE;
        }
        if (path.startsWith("/editor/apps")) {
            return CapabilityIds.EDITOR_APPS;
        }
        if (path.startsWith("/editor/themes")) {
            return CapabilityIds.EDITOR_THEMES;
        }
        if (path.startsWith("/editor/playlist")) {
            return CapabilityIds.EDITOR_PLAYLISTS;
        }
        if (path.startsWith("/editor/url-template")) {
            return CapabilityIds.EDITOR_URL_TEMPLATES;
        }
        if (path.startsWith("/editor/category") || path.startsWith("/editor/categories")) {
            return CapabilityIds.EDITOR_CATEGORIES;
        }
        if (path.startsWith("/editor/resources/") || path.startsWith("/editor/catalog")) {
            return CapabilityIds.EDITOR_FILES;
        }
        if (path.startsWith("/editor/video") || path.startsWith("/editor/transcode")
                || path.equals("/editor/resources")
                || path.equals("/editor") || path.equals("/editor/")
                || path.startsWith("/editor/content")) {
            return null;
        }
        if (path.startsWith("/editor/create") || path.startsWith("/editor/edit")
                || path.startsWith("/editor/save") || path.startsWith("/editor/delete")
                || path.startsWith("/editor/pages") || path.startsWith("/editor/data")
                || path.startsWith("/editor/item")
                || path.startsWith("/editor/album")) {
            return CapabilityIds.EDITOR_PAGES;
        }
        return CapabilityIds.EDITOR_ACCESS;
    }
}

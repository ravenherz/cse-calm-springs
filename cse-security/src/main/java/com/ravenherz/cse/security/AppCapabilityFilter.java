package com.ravenherz.cse.security;

import com.ravenherz.cse.dal.dto.AccountEntity;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;

final class AppCapabilityFilter extends OncePerRequestFilter {

    private final AccountAccessor authSupport;
    private final CapabilityService capabilities;

    AppCapabilityFilter(AccountAccessor authSupport, CapabilityService capabilities) {
        this.authSupport = authSupport;
        this.capabilities = capabilities;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String path = pathWithinApp(request);
        if (path == null) {
            filterChain.doFilter(request, response);
            return;
        }
        String slug = slugOf(path);
        if (slug == null || slug.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (capabilities.allowsApp(accessor, slug)) {
            filterChain.doFilter(request, response);
            return;
        }
        CseSecurityResponses.forbidden(request, response);
    }

    private static String pathWithinApp(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath() == null ? "" : request.getContextPath();
        if (uri == null) {
            return null;
        }
        String rest = uri.substring(context.length());
        if (rest.startsWith("/apps/") || rest.equals("/apps")) {
            return rest.substring("/apps".length());
        }
        if (rest.startsWith("/static-pages/") || rest.equals("/static-pages")) {
            return rest.substring("/static-pages".length());
        }
        return null;
    }

    private static String slugOf(String path) {
        String trimmed = path.startsWith("/") ? path.substring(1) : path;
        int slash = trimmed.indexOf('/');
        String slug = slash < 0 ? trimmed : trimmed.substring(0, slash);
        if (slug.isBlank()) {
            return null;
        }
        return slug.toLowerCase(Locale.ROOT);
    }
}

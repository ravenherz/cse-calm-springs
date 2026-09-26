package com.ravenherz.cse.web;

import com.ravenherz.cse.core.route.PathTarget;
import com.ravenherz.cse.core.route.ReservedPaths;
import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.redirect.RedirectIndex;
import com.ravenherz.cse.redirect.RedirectReload;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Public redirect lookup. A hit sets {@code Location}. A miss or a reserved path falls through.
 */
@Component
public class RedirectFilter extends OncePerRequestFilter implements RedirectReload {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedirectFilter.class);

    private final ServiceProvider services;
    private final RedirectIndex index = new RedirectIndex();
    private volatile boolean loaded;

    public RedirectFilter(ServiceProvider services) {
        this.services = services;
    }

    public void reload() {
        index.load(services.getResourceRedirectService());
        loaded = true;
    }

    @Override
    public void reloadRedirects() {
        reload();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String path = contextRelative(request);
        if (leaveToControllers(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        ensureLoaded();
        String query = request.getQueryString();
        String lookup = query == null || query.isBlank() ? path : path + "?" + query;
        Optional<PathTarget> target = index.resolve(lookup);
        if (target.isEmpty() && !lookup.equals(path)) {
            target = index.resolve(path);
        }
        if (target.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        PathTarget hit = target.get();
        response.setStatus(hit.status());
        response.setHeader("Location", location(request, hit));
    }

    private void ensureLoaded() {
        if (loaded) {
            return;
        }
        try {
            reload();
        } catch (RuntimeException ex) {
            LOGGER.warn("Redirect map was not loaded: {}", ex.getMessage());
        }
    }

    static String contextRelative(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null || uri.isBlank()) {
            return "/";
        }
        String context = request.getContextPath() == null ? "" : request.getContextPath();
        String path = uri.startsWith(context) ? uri.substring(context.length()) : uri;
        return path.isEmpty() ? "/" : path;
    }

    static boolean leaveToControllers(String path) {
        return ReservedPaths.isReserved(path);
    }

    static String location(HttpServletRequest request, PathTarget target) {
        String context = request.getContextPath() == null ? "" : request.getContextPath();
        String location = context + target.targetPath();
        String query = request.getQueryString();
        if (target.preserveQuery() && query != null && !query.isBlank()) {
            location += (target.targetPath().contains("?") ? "&" : "?") + query;
        }
        return location;
    }
}

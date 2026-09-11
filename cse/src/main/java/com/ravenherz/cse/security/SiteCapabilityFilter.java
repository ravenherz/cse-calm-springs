package com.ravenherz.cse.security;

import com.ravenherz.cse.controller.AuthSupport;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.role.CapabilityIds;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

final class SiteCapabilityFilter extends OncePerRequestFilter {

    private final AuthSupport authSupport;
    private final CapabilityService capabilities;

    SiteCapabilityFilter(AuthSupport authSupport, CapabilityService capabilities) {
        this.authSupport = authSupport;
        this.capabilities = capabilities;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String capability = capabilityFor(request);
        if (capability == null) {
            filterChain.doFilter(request, response);
            return;
        }
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (capabilities.allows(accessor, capability)) {
            filterChain.doFilter(request, response);
            return;
        }
        CseSecurityResponses.forbidden(request, response);
    }

    private static String capabilityFor(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath() == null ? "" : request.getContextPath();
        String path = uri == null ? "" : uri.substring(context.length());
        String method = request.getMethod();
        if (HttpMethod.GET.matches(method) && ("/".equals(path) || path.isEmpty())) {
            String error = request.getParameter("error");
            if (error != null && !error.isBlank()) {
                return null;
            }
            return CapabilityIds.SITE_READ;
        }
        if (HttpMethod.GET.matches(method) && "/rest/site".equals(path)) {
            return CapabilityIds.SITE_READ;
        }
        if (HttpMethod.POST.matches(method) && "/account/auth".equals(path)) {
            return CapabilityIds.ACCOUNT_AUTH;
        }
        if (HttpMethod.POST.matches(method) && "/account/register".equals(path)) {
            return CapabilityIds.ACCOUNT_REGISTER;
        }
        if (HttpMethod.GET.matches(method) && "/account/activate".equals(path)) {
            return CapabilityIds.ACCOUNT_ACTIVATE;
        }
        if (path.startsWith("/content-protected")) {
            return CapabilityIds.CONTENT_PROTECTED;
        }
        return null;
    }
}

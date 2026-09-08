package com.ravenherz.cse.security;

import com.ravenherz.cse.controller.AuthSupport;
import com.ravenherz.cse.dal.dto.AccountEntity;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Copies the Mongo cookie session into {@link SecurityContextHolder}.
 * Does not mint sessions; {@link AuthSupport} stays the source of truth.
 */
final class CseCookieAuthenticationFilter extends OncePerRequestFilter {

    private final AuthSupport authSupport;

    CseCookieAuthenticationFilter(AuthSupport authSupport) {
        this.authSupport = authSupport;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor != null && accessor.getAccountData() != null
                && accessor.getAccountData().getLogin() != null) {
            SecurityContext context = SecurityContextHolder.getContext();
            context.setAuthentication(CseAuthentication.authenticated(accessor));
        }
        filterChain.doFilter(request, response);
    }
}

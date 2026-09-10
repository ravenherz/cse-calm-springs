package com.ravenherz.cse.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring Security 6 defers writing the CSRF cookie until the token is read.
 * Force a read on every request so login / setup / error POSTs have {@code XSRF-TOKEN}.
 */
final class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            String value = csrfToken.getToken();
            if (value != null && !value.isBlank() && !response.isCommitted()) {
                response.setHeader(CseCookieCsrfTokenRepository.HEADER_NAME, value);
            }
        }
        filterChain.doFilter(request, response);
    }
}

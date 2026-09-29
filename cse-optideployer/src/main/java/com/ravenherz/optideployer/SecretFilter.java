package com.ravenherz.optideployer;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class SecretFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Cse-Deploy-Secret";

    private final OptiConfig config;

    public SecretFilter(OptiConfig config) {
        this.config = config;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        boolean upload = uri.endsWith("/upload-and-deploy") || uri.endsWith("/upload-and-deploy/");
        return !upload || !"POST".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!request.isSecure() || !config.matches(request.getHeader(HEADER))) {
            response.setStatus(403);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("forbidden");
            return;
        }
        chain.doFilter(request, response);
    }
}

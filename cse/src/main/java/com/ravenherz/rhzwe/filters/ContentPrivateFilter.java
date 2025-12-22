package com.ravenherz.rhzwe.filters;

import jakarta.servlet.*;


public class ContentPrivateFilter extends PublicFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws ServletException {
        throw new ServletException("Error");
    }
}

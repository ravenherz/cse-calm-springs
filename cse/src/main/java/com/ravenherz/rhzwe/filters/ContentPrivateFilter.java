package com.ravenherz.rhzwe.filters;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;


public class ContentPrivateFilter extends PublicFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.sendRedirect("/?error=404");
        return;
    }
}

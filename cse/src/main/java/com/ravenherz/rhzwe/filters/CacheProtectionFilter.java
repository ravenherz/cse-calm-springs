package com.ravenherz.rhzwe.filters;

import jakarta.servlet.*;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;


public class CacheProtectionFilter extends PublicFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // todo make special logic preventing people from bruteforcing urls (some sort of an address cache)
        super.doFilter(request,response,chain);
    }
}

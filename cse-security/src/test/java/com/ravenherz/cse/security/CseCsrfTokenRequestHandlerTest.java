package com.ravenherz.cse.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CseCsrfTokenRequestHandlerTest {

    @Test
    void acceptsMatchingQueryParamWhenBodyHasADifferentValue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("_csrf", new String[] {"stale-from-action", "cookie-token"});
        DefaultCsrfToken csrfToken = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "cookie-token");
        assertEquals("cookie-token",
                new CseCsrfTokenRequestHandler().resolveCsrfTokenValue(request, csrfToken));
    }

    @Test
    void prefersMatchingHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-XSRF-TOKEN", "cookie-token");
        request.setParameter("_csrf", "stale");
        DefaultCsrfToken csrfToken = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "cookie-token");
        assertEquals("cookie-token",
                new CseCsrfTokenRequestHandler().resolveCsrfTokenValue(request, csrfToken));
    }

    @Test
    void returnsSubmittedValueWhenNothingMatches() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("_csrf", "wrong");
        DefaultCsrfToken csrfToken = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "cookie-token");
        assertEquals("wrong",
                new CseCsrfTokenRequestHandler().resolveCsrfTokenValue(request, csrfToken));
    }

    @Test
    void missingTokenIsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        DefaultCsrfToken csrfToken = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "cookie-token");
        assertNull(new CseCsrfTokenRequestHandler().resolveCsrfTokenValue(request, csrfToken));
    }

    @Test
    void stillReadsCsrfBodyWhenTokenParameterNameDiffers() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("_csrf", "cookie-token");
        DefaultCsrfToken csrfToken = new DefaultCsrfToken("X-XSRF-TOKEN", "csrf", "cookie-token");
        assertEquals("cookie-token",
                new CseCsrfTokenRequestHandler().resolveCsrfTokenValue(request, csrfToken));
    }
}

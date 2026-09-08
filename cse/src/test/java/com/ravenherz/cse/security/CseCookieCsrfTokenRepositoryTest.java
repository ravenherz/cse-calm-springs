package com.ravenherz.cse.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CseCookieCsrfTokenRepositoryTest {

    @Test
    void saveTokenDoesNotExpireRootPathInTheSameResponse() {
        CseCookieCsrfTokenRepository repository = new CseCookieCsrfTokenRepository();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/rhz-we");
        request.addHeader("X-Forwarded-Proto", "https");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CsrfToken token = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "token-value");

        repository.saveToken(token, request, response);

        Cookie[] cookies = response.getCookies();
        assertEquals(1, cookies.length);
        assertEquals("XSRF-TOKEN", cookies[0].getName());
        assertEquals("token-value", cookies[0].getValue());
        assertEquals("/rhz-we", cookies[0].getPath());
        assertEquals(CseCookieCsrfTokenRepository.COOKIE_MAX_AGE_SECONDS, cookies[0].getMaxAge());
        assertTrue(cookies[0].getSecure());
    }

    @Test
    void clearExpiresContextPathAndRoot() {
        CseCookieCsrfTokenRepository repository = new CseCookieCsrfTokenRepository();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/rhz-we");
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveToken(null, request, response);

        Cookie[] cookies = response.getCookies();
        assertEquals(2, cookies.length);
        assertEquals("/rhz-we", cookies[0].getPath());
        assertEquals(0, cookies[0].getMaxAge());
        assertEquals("/", cookies[1].getPath());
        assertEquals(0, cookies[1].getMaxAge());
    }
}

package com.ravenherz.cse.security;

import com.ravenherz.cse.controller.AuthSupport;
import com.ravenherz.cse.dal.role.CapabilityIds;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SiteCapabilityFilterTest {

    @Test
    void guestDeniedSiteReadUsesErrorPageNotLogin() throws Exception {
        AuthSupport auth = mock(AuthSupport.class);
        CapabilityService capabilities = mock(CapabilityService.class);
        when(auth.getAccessor(any(), any())).thenReturn(null);
        when(capabilities.allows(any(), eq(CapabilityIds.SITE_READ))).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setContextPath("/rhz-we");
        request.setRequestURI("/rhz-we/");
        MockHttpServletResponse response = new MockHttpServletResponse();
        new SiteCapabilityFilter(auth, capabilities).doFilter(request, response, new MockFilterChain());
        assertEquals("/rhz-we/?error=403", response.getRedirectedUrl());
    }

    @Test
    void publicErrorPageSkipsSiteReadGate() throws Exception {
        AuthSupport auth = mock(AuthSupport.class);
        CapabilityService capabilities = mock(CapabilityService.class);
        when(auth.getAccessor(any(), any())).thenReturn(null);
        when(capabilities.allows(any(), eq(CapabilityIds.SITE_READ))).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setContextPath("/rhz-we");
        request.setRequestURI("/rhz-we/");
        request.setParameter("error", "403");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean continued = new AtomicBoolean();
        new SiteCapabilityFilter(auth, capabilities).doFilter(request, response,
                (req, res) -> continued.set(true));
        assertTrue(continued.get());
        assertTrue(response.getRedirectedUrl() == null || response.getRedirectedUrl().isEmpty());
    }

    @Test
    void guestDeniedProtectedFileUsesErrorPageNotLogin() throws Exception {
        AuthSupport auth = mock(AuthSupport.class);
        CapabilityService capabilities = mock(CapabilityService.class);
        when(auth.getAccessor(any(), any())).thenReturn(null);
        when(capabilities.allows(any(), eq(CapabilityIds.CONTENT_PROTECTED))).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET",
                "/content-protected/user/res/images/cover.jpg");
        request.setContextPath("");
        request.setRequestURI("/content-protected/user/res/images/cover.jpg");
        MockHttpServletResponse response = new MockHttpServletResponse();
        new SiteCapabilityFilter(auth, capabilities).doFilter(request, response, new MockFilterChain());
        assertEquals("/?error=403", response.getRedirectedUrl());
    }

    @Test
    void guestDeniedPagePdfUsesErrorPageNotLogin() throws Exception {
        AuthSupport auth = mock(AuthSupport.class);
        CapabilityService capabilities = mock(CapabilityService.class);
        when(auth.getAccessor(any(), any())).thenReturn(null);
        when(capabilities.allows(any(), eq(CapabilityIds.SITE_READ))).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/rest/pages/pdf");
        request.setContextPath("/rhz-we");
        request.setRequestURI("/rhz-we/rest/pages/pdf");
        request.setParameter("page", "cv");
        MockHttpServletResponse response = new MockHttpServletResponse();
        new SiteCapabilityFilter(auth, capabilities).doFilter(request, response, new MockFilterChain());
        assertEquals("/rhz-we/?error=403", response.getRedirectedUrl());
    }
}

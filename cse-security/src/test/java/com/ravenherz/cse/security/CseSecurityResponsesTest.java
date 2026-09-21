package com.ravenherz.cse.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CseSecurityResponsesTest {

    @Test
    void htmlUnauthorizedRedirectsLikeRequireEditor() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/rhz-we");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CseSecurityResponses.unauthorized(request, response);
        assertEquals("/rhz-we/?error=401", response.getRedirectedUrl());
    }

    @Test
    void jsonTailIsStatusNotRedirect() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/rhz-we/editor/logs/tail");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CseSecurityResponses.unauthorized(request, response);
        assertEquals(401, response.getStatus());
        assertTrue(response.getRedirectedUrl() == null || response.getRedirectedUrl().isEmpty());
        assertTrue(response.getContentAsString().contains("Sign in required"));
    }

    @Test
    void jsonInstanceSnapshotIsStatusNotRedirect() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/rhz-we/editor/instance/snapshot");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CseSecurityResponses.unauthorized(request, response);
        assertEquals(401, response.getStatus());
        assertTrue(response.getRedirectedUrl() == null || response.getRedirectedUrl().isEmpty());
        assertTrue(response.getContentAsString().contains("Sign in required"));
    }

    @Test
    void jsonTranscodeQueueIsStatusNotRedirect() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/rhz-we/editor/transcode/queue");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CseSecurityResponses.unauthorized(request, response);
        assertEquals(401, response.getStatus());
        assertTrue(response.getRedirectedUrl() == null || response.getRedirectedUrl().isEmpty());
        assertTrue(response.getContentAsString().contains("Sign in required"));
    }

    @Test
    void jsonForbiddenHasABody() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept", "application/json");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CseSecurityResponses.forbidden(request, response, "Security check failed");
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Security check failed"));
    }

    @Test
    void htmlForbiddenRedirectsLikeRequireAdmin() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/rhz-we");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CseSecurityResponses.forbidden(request, response);
        assertEquals("/rhz-we/?error=403", response.getRedirectedUrl());
    }
}

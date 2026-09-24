package com.ravenherz.cse.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RhzWeRedirectControllerTest {

    private final RhzWeRedirectController controller = new RhzWeRedirectController();

    @Test
    void prefixAloneRedirectsToContextRoot() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("");
        request.setRequestURI("/rhz-we");
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.redirect(request, response);

        assertEquals(301, response.getStatus());
        assertEquals("/", response.getHeader("Location"));
    }

    @Test
    void trailingSlashRedirectsToContextRoot() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("");
        request.setRequestURI("/rhz-we/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.redirect(request, response);

        assertEquals(301, response.getStatus());
        assertEquals("/", response.getHeader("Location"));
    }

    @Test
    void pathAndQueryArePreserved() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("");
        request.setRequestURI("/rhz-we/editor/resources");
        request.setQueryString("group=content");
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.redirect(request, response);

        assertEquals(301, response.getStatus());
        assertEquals("/editor/resources?group=content", response.getHeader("Location"));
    }

    @Test
    void contextPathIsKept() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/cse");
        request.setRequestURI("/cse/rhz-we/apps/login/");
        request.setQueryString("next=%2F");
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.redirect(request, response);

        assertEquals(301, response.getStatus());
        assertEquals("/cse/apps/login/?next=%2F", response.getHeader("Location"));
    }

    @Test
    void traversalIs404() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("");
        request.setRequestURI("/rhz-we/../editor");
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.redirect(request, response);

        assertEquals(404, response.getStatus());
        assertNull(response.getHeader("Location"));
    }

    @Test
    void protocolRelativeSuffixIs404() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("");
        request.setRequestURI("/rhz-we//evil.example");
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.redirect(request, response);

        assertEquals(404, response.getStatus());
        assertNull(response.getHeader("Location"));
    }
}

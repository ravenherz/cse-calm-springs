package com.ravenherz.cse.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StaticPagesRedirectControllerTest {

    private final StaticPagesRedirectController controller = new StaticPagesRedirectController();

    @Test
    void loginAppRedirectsWithContextAndQuery() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/rhz-we");
        request.setRequestURI("/rhz-we/static-pages/login/");
        request.setQueryString("next=%2Frhz-we%2F");
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.redirect(request, response);

        assertEquals(301, response.getStatus());
        assertEquals("/rhz-we/apps/login/?next=%2Frhz-we%2F", response.getHeader("Location"));
    }

    @Test
    void prefixAloneRedirectsToAppsRoot() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("");
        request.setRequestURI("/static-pages");
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.redirect(request, response);

        assertEquals(301, response.getStatus());
        assertEquals("/apps", response.getHeader("Location"));
    }

    @Test
    void traversalIs404() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/rhz-we");
        request.setRequestURI("/rhz-we/static-pages/../apps/login/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.redirect(request, response);

        assertEquals(404, response.getStatus());
        assertNull(response.getHeader("Location"));
    }
}

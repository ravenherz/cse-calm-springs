package com.ravenherz.cse.controller;

import com.ravenherz.cse.install.SiteReady;
import com.ravenherz.cse.engine.util.Settings;
import com.ravenherz.cse.util.staticapps.StaticAppDeployer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StaticPagesControllerInstallerTest {

    @Test
    void configuredInstallerPathIs404() throws IOException {
        Settings settings = mock(Settings.class);
        SiteReady siteReady = mock(SiteReady.class);
        when(siteReady.isConfigured()).thenReturn(true);
        StaticPagesController controller = new StaticPagesController();
        controller.setSettings(settings);
        controller.setSiteReady(siteReady);
        ReflectionTestUtils.setField(controller, "staticAppDeployer", mock(StaticAppDeployer.class));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/rhz-we");
        request.setRequestURI("/rhz-we/apps/setup/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertNull(controller.getResource(request, response));
        assertEquals(404, response.getStatus());
    }
}

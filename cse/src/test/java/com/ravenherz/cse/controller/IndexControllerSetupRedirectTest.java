package com.ravenherz.cse.controller;

import com.ravenherz.cse.controller.publicsite.PublicIndexModel;
import com.ravenherz.cse.install.SiteReady;
import com.ravenherz.cse.util.Settings;
import com.ravenherz.cse.util.themes.ThemeCatalog;
import com.ravenherz.cse.util.themes.ThemeInfo;
import com.ravenherz.cse.util.themes.ThemeSelection;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ExtendedModelMap;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IndexControllerSetupRedirectTest {

    @Test
    void unconfiguredRootRedirectsToInstaller() throws IOException {
        Settings settings = mock(Settings.class);
        SiteReady siteReady = mock(SiteReady.class);
        when(siteReady.isConfigured()).thenReturn(false);
        IndexController controller = new IndexController(mock(PublicIndexModel.class));
        controller.setSettings(settings);
        controller.setSiteReady(siteReady);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/rhz-we");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String view = controller.getPage(new ExtendedModelMap(), request, response,
                null, null, null, null, null);

        assertNull(view);
        assertEquals("/rhz-we/apps/setup/", response.getRedirectedUrl());
    }

    @Test
    void jsShellSkipsThymeleafPageFill() throws IOException {
        Settings settings = mock(Settings.class);
        SiteReady siteReady = mock(SiteReady.class);
        when(siteReady.isConfigured()).thenReturn(true);
        PublicIndexModel publicIndexModel = mock(PublicIndexModel.class);
        IndexController controller = new IndexController(publicIndexModel);
        controller.setSettings(settings);
        controller.setSiteReady(siteReady);
        controller.tags = Collections.emptyList();
        AuthSupport authSupport = mock(AuthSupport.class);
        when(authSupport.getAccessor(any(), any())).thenReturn(null);
        controller.setAuthSupport(authSupport);

        ThemeCatalog catalog = mock(ThemeCatalog.class);
        ThemeSelection selection = new ThemeSelection("client", "linen", "client");
        when(catalog.resolve(any())).thenReturn(selection);
        when(catalog.find("client")).thenReturn(new ThemeInfo("client", "JS", null, null, "js",
                "linen", List.of("linen", "ivory"), true));
        controller.setThemeCatalog(catalog);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/rhz-we");
        String view = controller.getPage(new ExtendedModelMap(), request, new MockHttpServletResponse(),
                "hello", null, null, null, null);

        assertEquals("/client/index", view);
        verify(publicIndexModel, never()).addPageContentToModel(any(), any(), any(), any(), any(),
                any(), any(), any(), any());
    }
}

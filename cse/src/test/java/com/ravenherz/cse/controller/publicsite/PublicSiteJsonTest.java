package com.ravenherz.cse.controller.publicsite;

import com.ravenherz.cse.install.SiteReady;
import com.ravenherz.cse.util.Settings;
import com.ravenherz.cse.util.helpers.HttpErrorHelper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicSiteJsonTest {

    @Test
    void unconfiguredSnapshotIsSetupView() {
        Settings settings = mock(Settings.class);
        SiteReady siteReady = mock(SiteReady.class);
        when(siteReady.isConfigured()).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/rhz-we");
        Map<String, Object> body = PublicSiteJson.from(new ExtendedModelMap(), request, settings,
                siteReady, null, null, null, new HttpErrorHelper(), 0);
        assertEquals(false, body.get("configured"));
        assertEquals("setup", body.get("view"));
        assertEquals("/rhz-we", body.get("contextPath"));
        assertNull(body.get("page"));
    }

    @Test
    void queryErrorUsesHelperCopy() {
        Settings settings = mock(Settings.class);
        SiteReady siteReady = mock(SiteReady.class);
        when(siteReady.isConfigured()).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/rhz-we");
        Map<String, Object> body = PublicSiteJson.from(new ExtendedModelMap(), request, settings,
                siteReady, null, null, "401", new HttpErrorHelper(), 0);
        assertEquals("error", body.get("view"));
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) body.get("error");
        assertEquals(401, error.get("code"));
    }

    @Test
    void footUsesRenderedContactTags() {
        Settings settings = mock(Settings.class);
        SiteReady siteReady = mock(SiteReady.class);
        when(siteReady.isConfigured()).thenReturn(true);
        ExtendedModelMap model = new ExtendedModelMap();
        model.addAttribute("TagCompanyPhone",
                "<div id=\"company-phone-container\" name=\"highlightable\">+1 555</div>");
        model.addAttribute("TagCompanySocialLinks",
                "<a class=\"company-social\" href=\"https://example.com\">ig</a>");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/rhz-we");
        Map<String, Object> body = PublicSiteJson.from(model, request, settings,
                siteReady, null, null, null, new HttpErrorHelper(), 0);
        @SuppressWarnings("unchecked")
        Map<String, String> foot = (Map<String, String>) body.get("foot");
        assertTrue(foot.get("org").contains("company-phone-container"));
        assertTrue(foot.get("org").contains("company-social"));
    }
}

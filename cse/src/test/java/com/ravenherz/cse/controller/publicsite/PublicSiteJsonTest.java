package com.ravenherz.cse.controller.publicsite;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.dto.basic.PageData;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import com.ravenherz.cse.dal.dto.events.PageEvent;
import com.ravenherz.cse.install.SiteReady;
import com.ravenherz.cse.engine.util.Settings;
import com.ravenherz.cse.util.helpers.HttpErrorHelper;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;
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

    @Test
    void pageJsonIncludesNoTopDisplayImage() {
        AccountEntity author = new AccountEntity(new AccountData("ada", "hash", "ada@example.com",
                SecurityLevel.ACTIVE_USER));
        PageData pageData = new PageData("H", "S", "Body", List.of());
        pageData.setNoTopDisplayImage(true);
        ItemEntity item = new ItemEntity("hello", pageData, author);
        item.setId(new ObjectId());
        Map<String, Object> map = PublicSiteJson.item(PageEvent.PageEventConverter.toEvent(item));
        assertEquals(true, map.get("noTopDisplayImage"));
        pageData.setNoTopDisplayImage(false);
        Map<String, Object> off = PublicSiteJson.item(PageEvent.PageEventConverter.toEvent(item));
        assertEquals(false, off.get("noTopDisplayImage"));
    }

    @Test
    void pageJsonIncludesExportPdf() {
        AccountEntity author = new AccountEntity(new AccountData("ada", "hash", "ada@example.com",
                SecurityLevel.ACTIVE_USER));
        PageData pageData = new PageData("H", "S", "Body", List.of());
        pageData.setExportPdf(true);
        ItemEntity item = new ItemEntity("hello", pageData, author);
        item.setId(new ObjectId());
        Map<String, Object> map = PublicSiteJson.item(PageEvent.PageEventConverter.toEvent(item));
        assertEquals(true, map.get("exportPdf"));
        pageData.setExportPdf(false);
        Map<String, Object> off = PublicSiteJson.item(PageEvent.PageEventConverter.toEvent(item));
        assertEquals(false, off.get("exportPdf"));
    }
}

package com.ravenherz.cse.engine.pdf;

import com.ravenherz.cse.pdf.PagePdfRenderer;

import com.ravenherz.cse.controller.AuthSupport;
import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.dao.ItemService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.basic.AccessRule;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.dto.basic.AlbumData;
import com.ravenherz.cse.dal.dto.basic.PageData;
import com.ravenherz.cse.dal.dto.basic.enums.AccessType;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import com.ravenherz.cse.dal.role.RoleSeeds;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PagePdfExportTest {

    @Test
    void missingPageIs404() throws Exception {
        PagePdfExport export = export(null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        export.write(new MockHttpServletRequest(), response, "missing");
        assertEquals(404, response.getStatus());
    }

    @Test
    void albumIs404() throws Exception {
        AccountEntity author = author();
        ItemEntity album = new ItemEntity("shots", new AlbumData(), author);
        album.setId(new ObjectId());
        PagePdfExport export = export(album);
        MockHttpServletResponse response = new MockHttpServletResponse();
        export.write(new MockHttpServletRequest(), response, "shots");
        assertEquals(404, response.getStatus());
    }

    @Test
    void exportOffIs404() throws Exception {
        ItemEntity item = page(false);
        PagePdfExport export = export(item);
        MockHttpServletResponse response = new MockHttpServletResponse();
        export.write(new MockHttpServletRequest(), response, "cv");
        assertEquals(404, response.getStatus());
    }

    @Test
    void unreadableIs403() throws Exception {
        ItemEntity item = page(true);
        item.getSecurityData().getAccessSettings()
                .put(AccessType.ACCESS_READ, AccessRule.ofRoles(List.of(RoleSeeds.MEMBER)));
        PagePdfExport export = export(item);
        MockHttpServletResponse response = new MockHttpServletResponse();
        export.write(new MockHttpServletRequest(), response, "cv");
        assertEquals(403, response.getStatus());
    }

    @Test
    void writesPdfAttachment() throws Exception {
        ItemEntity item = page(true);
        PagePdfExport export = export(item);
        MockHttpServletResponse response = new MockHttpServletResponse();
        export.write(new MockHttpServletRequest(), response, "cv");
        assertEquals(200, response.getStatus());
        assertEquals("application/pdf", response.getContentType());
        assertTrue(response.getHeader("Content-Disposition").contains("cv.pdf"),
                response.getHeader("Content-Disposition"));
        byte[] pdf = response.getContentAsByteArray();
        String head = new String(pdf, 0, Math.min(8, pdf.length), StandardCharsets.ISO_8859_1);
        assertTrue(head.startsWith("%PDF"), head);
    }

    @Test
    void publicBaseUrlUsesForwardedHostAndContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8081);
        request.setContextPath("/rhz-we");
        request.addHeader("X-Forwarded-Proto", "https");
        request.addHeader("X-Forwarded-Host", "ravenherz.com");
        assertEquals("https://ravenherz.com/rhz-we/", PagePdfExport.publicBaseUrl(request));
    }

    @Test
    void filenameSanitizesUri() {
        assertEquals("cv.pdf", PagePdfExport.filename("cv"));
        assertEquals("my_page.pdf", PagePdfExport.filename("my page"));
    }

    private static PagePdfExport export(ItemEntity item) {
        ItemService items = mock(ItemService.class);
        when(items.getByName(any())).thenReturn(item);
        ServiceProvider services = mock(ServiceProvider.class);
        when(services.getItemService()).thenReturn(items);
        AuthSupport auth = mock(AuthSupport.class);
        when(auth.getAccessor(any(), any())).thenReturn(null);
        return new PagePdfExport(services, auth, new PagePdfRenderer((src, accessor) -> null));
    }

    private static ItemEntity page(boolean exportPdf) {
        PageData pageData = new PageData("Ada", "Sub", "Body", List.of());
        pageData.setExportPdf(exportPdf);
        ItemEntity item = new ItemEntity("cv", pageData, author());
        item.setId(new ObjectId());
        return item;
    }

    private static AccountEntity author() {
        return new AccountEntity(new AccountData("ada", "hash", "ada@example.com",
                SecurityLevel.ACTIVE_USER));
    }
}

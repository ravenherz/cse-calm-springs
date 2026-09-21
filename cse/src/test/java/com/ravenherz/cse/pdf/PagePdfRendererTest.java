package com.ravenherz.cse.pdf;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.dto.basic.PageData;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import com.ravenherz.cse.dal.dto.events.PageEvent;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagePdfRendererTest {

    @Test
    void rendersPdfHeaderFromArticle() throws Exception {
        AccountEntity author = new AccountEntity(new AccountData("ada", "hash", "ada@example.com",
                SecurityLevel.ACTIVE_USER));
        PageData pageData = new PageData("Title", "Ada Lovelace", "Engineer",
                "<div class=\"cv-card\"><div class=\"cv-card-body\"><h3>Work</h3></div></div>",
                List.of());
        pageData.setExportPdf(true);
        ItemEntity item = new ItemEntity("cv", pageData, author);
        item.setId(new ObjectId());
        PageEvent event = PageEvent.PageEventConverter.toEvent(item);

        PagePdfRenderer renderer = new PagePdfRenderer((src, accessor) -> null);
        byte[] pdf = renderer.render(event, null);
        assertTrue(pdf.length > 100);
        String head = new String(pdf, 0, Math.min(8, pdf.length), StandardCharsets.ISO_8859_1);
        assertTrue(head.startsWith("%PDF"), head);
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDRectangle box = doc.getPage(0).getMediaBox();
            assertEquals(PDRectangle.A4.getWidth(), box.getWidth(), 1.0f);
            assertEquals(PDRectangle.A4.getHeight(), box.getHeight(), 1.0f);
        }
    }

    @Test
    void publicPathFromSrcStripsContentProtectedPrefix() {
        assertEquals("/u/res/image/cover.jpg",
                PagePdfRenderer.publicPathFromSrc("./content-protected/u/res/image/cover.jpg"));
        assertEquals("/u/res/image/cover.jpg",
                PagePdfRenderer.publicPathFromSrc("/rhz-we/content-protected/u/res/image/cover.jpg"));
        assertEquals("", PagePdfRenderer.publicPathFromSrc("https://example.com/x.jpg"));
    }

    @Test
    void dropsUnresolvableImages() {
        PagePdfRenderer renderer = new PagePdfRenderer((src, accessor) -> null);
        String html = renderer.inlineImages(
                "<article><img src=\"./content-protected/u/res/image/cover.jpg\" alt=\"\"/></article>",
                null);
        assertTrue(html.contains("<article"), html);
        assertTrue(!html.contains("img") && !html.contains("<img"), html);
    }

    @Test
    void layoutRewriteRunsDuringInline() {
        PagePdfRenderer renderer = new PagePdfRenderer((src, accessor) -> null);
        String html = renderer.inlineImages("""
                <article class="reading">
                  <div class="cv-img-card">
                    <div class="cv-img-card-media" style="width:256px;height:256px"></div>
                    <div class="cv-card-body"><h3>SGU</h3></div>
                  </div>
                </article>
                """, null);
        assertTrue(html.contains("<table"), html);
        assertTrue(html.contains("width:256px"), html);
    }

    @Test
    void bootstrapColumnsSurviveInline() {
        PagePdfRenderer renderer = new PagePdfRenderer((src, accessor) -> null);
        String html = renderer.inlineImages("""
                <article class="reading">
                  <div class="row">
                    <div class="col-sm-5"><p>Left</p></div>
                    <div class="col-sm-7"><p>Right</p></div>
                  </div>
                </article>
                """, null);
        assertTrue(html.contains("pdf-row"), html);
        assertTrue(html.contains("41.67%"), html);
        assertTrue(html.contains("58.33%"), html);
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
        assertEquals("https://ravenherz.com/rhz-we/", PagePdfRenderer.publicBaseUrl(request));
    }

    @Test
    void keepsClickableAbsoluteLinks() throws Exception {
        AccountEntity author = new AccountEntity(new AccountData("ada", "hash", "ada@example.com",
                SecurityLevel.ACTIVE_USER));
        PageData pageData = new PageData("Title", "Ada", "Sub",
                "<p><a href=\"./?page=cse\">Calm Springs</a></p>"
                        + "<a class=\"cse-embed cse-embed-m\" href=\"./content-protected/u/cert.pdf\">"
                        + "<span class=\"cse-embed-media\"></span>"
                        + "<span class=\"cse-embed-body\"><span class=\"cse-embed-title\">Cert</span></span></a>",
                List.of());
        pageData.setExportPdf(true);
        ItemEntity item = new ItemEntity("notes", pageData, author);
        item.setId(new ObjectId());
        PageEvent event = PageEvent.PageEventConverter.toEvent(item);
        PagePdfRenderer renderer = new PagePdfRenderer((src, accessor) -> null);
        byte[] pdf = renderer.render(event, null, "https://example.com/site/");
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            boolean self = false;
            boolean page = false;
            boolean cert = false;
            for (PDAnnotation annotation : doc.getPage(0).getAnnotations()) {
                if (annotation instanceof PDAnnotationLink link
                        && link.getAction() instanceof PDActionURI uri) {
                    if ("https://example.com/site/?page=notes".equals(uri.getURI())) {
                        self = true;
                    }
                    if ("https://example.com/site/?page=cse".equals(uri.getURI())) {
                        page = true;
                    }
                    if ("https://example.com/site/content-protected/u/cert.pdf".equals(uri.getURI())) {
                        cert = true;
                    }
                }
            }
            assertTrue(self, "expected the article header to link to the exported page");
            assertTrue(page, "expected a clickable link to https://example.com/site/?page=cse");
            assertTrue(cert, "expected a clickable embed link to the certificate");
        }
    }
}

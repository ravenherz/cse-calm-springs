package com.ravenherz.cse.pdf;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagePdfLayoutTest {

    @Test
    void cvImgCardKeepsLogoSizeAndStacksBesideBody() {
        Document doc = Jsoup.parseBodyFragment("""
                <div class="cv-img-card">
                  <div class="cv-img-card-media" style="width:256px;height:256px">
                    <img src="./content-protected/u/logo.png" alt="SGU"/>
                  </div>
                  <div class="cv-card-body"><h3>SGU</h3></div>
                </div>
                """);
        PagePdfLayout.prepare(doc.body());
        Element table = doc.selectFirst("table.cv-img-card");
        assertNotNull(table);
        Element media = table.selectFirst("td.cv-img-card-media");
        assertNotNull(media);
        assertTrue(media.attr("style").contains("width:256px"), media.attr("style"));
        assertTrue(media.attr("style").contains("height:256px"), media.attr("style"));
        assertEquals("SGU", table.selectFirst("td.cv-card-body h3").text());
        assertTrue(table.selectFirst("td.cv-card-body").attr("style").contains("padding-left:5mm"));
    }

    @Test
    void bootstrapRowBecomesFixedTable() {
        Document doc = Jsoup.parseBodyFragment("""
                <div class="row">
                  <div class="col-sm-5"><img src="./content-protected/u/photo.jpg" alt=""/></div>
                  <div class="col-sm-7"><p>Summary</p></div>
                </div>
                <div class="row">
                  <div class="col-sm-2"><figure class="cse-image"><img src="./logo.png" alt=""/></figure></div>
                  <div class="col-sm-10"><div class="cv-card"><h3>Kryptonite</h3></div></div>
                </div>
                """);
        PagePdfLayout.prepare(doc.body());
        Elements tables = doc.select("table.pdf-row");
        assertEquals(2, tables.size());
        Element header = tables.get(0);
        assertTrue(header.attr("style").contains("table-layout:fixed"), header.attr("style"));
        assertTrue(header.selectFirst("td.col-sm-5").attr("style").contains("41.67%"),
                header.selectFirst("td.col-sm-5").attr("style"));
        assertTrue(header.selectFirst("td.col-sm-7").attr("style").contains("58.33%"),
                header.selectFirst("td.col-sm-7").attr("style"));
        assertTrue(header.selectFirst("td.col-sm-7").attr("style").contains("padding-left:5mm"),
                header.selectFirst("td.col-sm-7").attr("style"));
        assertFalse(header.selectFirst("td.col-sm-5").attr("style").contains("padding-left"),
                header.selectFirst("td.col-sm-5").attr("style"));
        Element job = tables.get(1);
        assertFalse(job.hasClass("pdf-keep"), job.className());
        assertFalse(job.attr("style").contains("page-break-inside:avoid"), job.attr("style"));
        assertNull(job.closest(".pdf-unbreakable"));
        assertFalse(header.hasClass("pdf-unbreakable"), header.className());
        assertTrue(job.selectFirst("td.col-sm-2").attr("style").contains("16.67%"),
                job.selectFirst("td.col-sm-2").attr("style"));
        assertEquals("Kryptonite", job.selectFirst("td.col-sm-10 h3").text());
        String logo = job.selectFirst("td.pdf-col .cse-image img").attr("style");
        assertTrue(logo.contains("max-width:100%"), logo);
    }

    @Test
    void pdfUnbreakableIsTheOnlyKeepTogetherRule() {
        Document doc = Jsoup.parseBodyFragment("""
                <div class="pdf-unbreakable">
                  <div class="row">
                    <div class="col-sm-2"><figure class="cse-image"><img src="./logo.png" alt=""/></figure></div>
                    <div class="col-sm-10"><div class="cv-card"><h3>Kryptonite</h3></div></div>
                  </div>
                </div>
                <div class="row">
                  <div class="col-sm-2"><figure class="cse-image"><img src="./other.png" alt=""/></figure></div>
                  <div class="col-sm-10"><div class="cv-card"><h3>Other</h3></div></div>
                </div>
                """);
        PagePdfLayout.prepare(doc.body());
        Element marked = doc.selectFirst(".pdf-unbreakable");
        assertNotNull(marked);
        assertTrue(marked.attr("style").contains("page-break-inside:avoid"), marked.attr("style"));
        assertEquals("Kryptonite", marked.selectFirst("td.col-sm-10 h3").text());
        assertTrue(marked.selectFirst("table").attr("style").contains("page-break-inside:avoid"),
                marked.selectFirst("table").attr("style"));
        Element other = doc.select("table.pdf-row").stream()
                .filter(table -> table.text().contains("Other"))
                .findFirst()
                .orElseThrow();
        assertNull(other.closest(".pdf-unbreakable"));
        assertFalse(other.attr("style").contains("page-break-inside:avoid"), other.attr("style"));
    }

    @Test
    void wideColumnPhotoKeepsNaturalAspect() {
        Document doc = Jsoup.parseBodyFragment("""
                <div class="row">
                  <div class="col-sm-5"><figure class="cse-image"><img src="./photo.jpg" alt=""/></figure></div>
                  <div class="col-sm-7"><p>Summary</p></div>
                </div>
                """);
        PagePdfLayout.prepare(doc.body());
        assertTrue(doc.select("div.pdf-photo").isEmpty());
        String style = doc.selectFirst("td.col-sm-5 .cse-image img").attr("style");
        assertTrue(style.contains("height:auto"), style);
        assertTrue(style.contains("width:100%"), style);
        assertTrue(style.contains("margin-bottom:5mm"), style);
    }

    @Test
    void exportPdfControlIsDroppedFromArticle() {
        Document doc = Jsoup.parseBodyFragment("""
                <article class="reading">
                  <div class="reading-head">
                    <div class="reading-titles"><h1>Notes</h1></div>
                    <a class="export-pdf" href="./rest/pages/pdf?page=notes">
                      <img src="./content-public/cse-core/images/pdf.png" alt="PDF"/>
                    </a>
                  </div>
                </article>
                """);
        PagePdfLayout.prepare(doc.body(), "https://example.com/site/", "./?page=notes");
        assertTrue(doc.select(".export-pdf").isEmpty());
        assertTrue(doc.select("img[src*=pdf.png]").isEmpty());
        Element self = doc.selectFirst("h1 a.pdf-self");
        assertNotNull(self);
        assertTrue(self.text().contains("Notes"), self.text());
    }

    @Test
    void articleHeaderBecomesFirstLinkToTheExportedPage() {
        Document doc = Jsoup.parseBodyFragment("""
                <article class="reading">
                  <h1><span></span></h1>
                  <div class="piece-body item-description-content">
                    <h1>Notes</h1>
                    <a href="./?page=cse">Calm Springs</a>
                  </div>
                </article>
                """);
        PagePdfLayout.prepare(doc.body(), "https://example.com/site/", "./?page=notes");
        Element self = doc.selectFirst("h1 a.pdf-self");
        assertNotNull(self);
        assertEquals("https://example.com/site/?page=notes", self.attr("href"));
        assertTrue(self.text().contains("Notes"), self.text());
        assertEquals("[1]", self.selectFirst(".pdf-link-n").text());
        assertEquals("[1] https://example.com/site/?page=notes", doc.selectFirst(".pdf-links p").text());
        assertEquals("[2]", doc.selectFirst("a[href$=cse] .pdf-link-n").text());
    }

    @Test
    void compactEmbedBecomes32pxRow() {
        Document doc = Jsoup.parseBodyFragment("""
                <a class="cse-embed cse-embed-page cse-embed-m" href="./?page=cse">
                  <span class="cse-embed-media"><img src="./content-protected/u/cse.png" alt=""/></span>
                  <span class="cse-embed-body"><span class="cse-embed-title">Calm Springs</span></span>
                </a>
                """);
        PagePdfLayout.prepare(doc.body());
        Element link = doc.selectFirst("a.cse-embed-m");
        assertNotNull(link);
        assertEquals("./?page=cse", link.attr("href"));
        assertEquals("Calm Springs", link.selectFirst(".cse-embed-title").text());
        Element img = link.selectFirst("img");
        assertEquals("32", img.attr("width"));
        assertTrue(img.attr("style").contains("max-width:32px"), img.attr("style"));
        Element mark = link.selectFirst(".pdf-link-n");
        assertEquals("[1]", mark.text());
        assertTrue(mark.attr("style").contains("vertical-align:top"), mark.attr("style"));
        assertEquals("[1] ./?page=cse", doc.selectFirst(".pdf-links p").text());
    }

    @Test
    void relativeHrefsBecomeAbsolute() {
        Document doc = Jsoup.parseBodyFragment("""
                <a class="cse-embed cse-embed-m" href="./content-protected/u/cert.pdf">
                  <span class="cse-embed-media"><img src="./c.png" alt=""/></span>
                  <span class="cse-embed-body"><span class="cse-embed-title">Cert</span></span>
                </a>
                <p><a href="https://example.com/x">External</a></p>
                """);
        PagePdfLayout.prepare(doc.body(), "https://ravenherz.com/rhz-we/");
        assertEquals("https://ravenherz.com/rhz-we/content-protected/u/cert.pdf",
                doc.selectFirst("a.pdf-link").attr("href"));
        assertEquals("https://example.com/x", doc.selectFirst("p a").attr("href"));
        assertEquals("[1]", doc.selectFirst("a.pdf-link .pdf-link-n").text());
        assertEquals("[2]", doc.selectFirst("p a .pdf-link-n").text());
        Elements listed = doc.select(".pdf-links p");
        assertEquals(2, listed.size());
        assertTrue(listed.get(0).text().startsWith("[1] https://ravenherz.com/rhz-we/"));
        assertEquals("[2] https://example.com/x", listed.get(1).text());
    }

    @Test
    void resolveHrefLeavesAbsoluteAndSpecialSchemes() {
        assertEquals("https://t.me/ravenherz",
                PagePdfLayout.resolveHref("https://t.me/ravenherz", "https://site/"));
        assertEquals("https://site/?page=cse",
                PagePdfLayout.resolveHref("./?page=cse", "https://site/"));
        assertEquals("mailto:ada@example.com",
                PagePdfLayout.resolveHref("mailto:ada@example.com", "https://site/"));
    }

    @Test
    void prefersSmColumnOverXs() {
        assertEquals(5, PagePdfLayout.columnSpan("col-xs-12 col-sm-5"));
        assertEquals(2, PagePdfLayout.columnSpan("col-sm-2"));
        assertEquals(0, PagePdfLayout.columnSpan("row"));
    }
}

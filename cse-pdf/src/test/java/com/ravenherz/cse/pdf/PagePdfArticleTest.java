package com.ravenherz.cse.pdf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagePdfArticleTest {

    @Test
    void wrapsHeaderAndBodyInReadingArticle() {
        String html = PagePdfArticle.html(source("Hello", "Sub", "Body", null, false, false));
        assertTrue(html.contains("<article class=\"reading\">"), html);
        assertTrue(html.contains("<h1><span>Hello</span></h1>"), html);
        assertTrue(html.contains("class=\"piece-body item-description-content\""), html);
        assertTrue(html.contains("Body"), html);
        assertFalse(html.contains("class=\"kicker\""), html);
        assertFalse(html.contains("export-pdf"), html);
    }

    @Test
    void skipsThemeChromeWhenHeaderIsBlank() {
        String html = PagePdfArticle.html(source("", "Sub", "<h1>Aleksei Kolesnikov</h1>", null, false, false));
        assertTrue(html.contains("<h1>Aleksei Kolesnikov</h1>"), html);
        assertFalse(html.contains("<h1><span>"), html);
        assertFalse(html.contains("reading-sub"), html);
        assertFalse(html.contains(">Title<"), html);
        assertFalse(html.contains(">CV<") || html.contains(">cv<"), html);
    }

    @Test
    void skipsPlaceholderAndHiddenCover() {
        ArticleSource placeholder = source("Hello", "Sub", "Body",
                "./content-public/cse-core/images/no-image.jpg", false, false);
        assertFalse(PagePdfArticle.html(placeholder).contains("<figure"), PagePdfArticle.html(placeholder));

        ArticleSource cover = source("Hello", "Sub", "Body",
                "./content-protected/u/res/image/cover.jpg", false, false);
        assertTrue(PagePdfArticle.html(cover).contains("cover.jpg"), PagePdfArticle.html(cover));

        ArticleSource hidden = source("Hello", "Sub", "Body",
                "./content-protected/u/res/image/cover.jpg", false, true);
        assertFalse(PagePdfArticle.html(hidden).contains("<figure"), PagePdfArticle.html(hidden));
    }

    private static ArticleSource source(String header, String subHeader, String description,
            String image, boolean album, boolean noTopDisplayImage) {
        return new ArticleSource(header, subHeader, description, image, album, noTopDisplayImage, "./?page=hello");
    }
}

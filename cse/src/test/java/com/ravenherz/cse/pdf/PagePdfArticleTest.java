package com.ravenherz.cse.pdf;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.dto.basic.PageData;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import com.ravenherz.cse.dal.dto.events.PageEvent;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagePdfArticleTest {

    @Test
    void wrapsHeaderAndBodyInReadingArticle() {
        PageEvent event = event("Hello", "# Body");
        String html = PagePdfArticle.html(event);
        assertTrue(html.contains("<article class=\"reading\">"), html);
        assertTrue(html.contains("<h1><span>Hello</span></h1>"), html);
        assertTrue(html.contains("class=\"piece-body item-description-content\""), html);
        assertTrue(html.contains("<h1>Body</h1>") || html.contains("<p>Body</p>")
                || html.contains("Body"), html);
        assertFalse(html.contains("class=\"kicker\""), html);
        assertFalse(html.contains("export-pdf"), html);
    }

    @Test
    void skipsThemeChromeWhenHeaderIsBlank() {
        PageEvent event = event("", "<h1>Aleksei Kolesnikov</h1>");
        String html = PagePdfArticle.html(event);
        assertTrue(html.contains("<h1>Aleksei Kolesnikov</h1>"), html);
        assertFalse(html.contains("<h1><span>"), html);
        assertFalse(html.contains("reading-sub"), html);
        assertFalse(html.contains(">Title<"), html);
        assertFalse(html.contains(">CV<") || html.contains(">cv<"), html);
    }

    @Test
    void skipsPlaceholderAndHiddenCover() {
        PageEvent event = event("Hello", "Body");
        event.setImageLinkFull("./content-public/cse-core/images/no-image.jpg");
        assertFalse(PagePdfArticle.html(event).contains("<figure"), PagePdfArticle.html(event));

        event.setImageLinkFull("./content-protected/u/res/image/cover.jpg");
        assertTrue(PagePdfArticle.html(event).contains("cover.jpg"), PagePdfArticle.html(event));

        event.setNoTopDisplayImage(true);
        assertFalse(PagePdfArticle.html(event).contains("<figure"), PagePdfArticle.html(event));
    }

    private static PageEvent event(String header, String description) {
        AccountEntity author = new AccountEntity(new AccountData("ada", "hash", "ada@example.com",
                SecurityLevel.ACTIVE_USER));
        PageData pageData = new PageData("Title", header, "Sub", description, List.of());
        ItemEntity item = new ItemEntity("hello", pageData, author);
        item.setId(new ObjectId());
        return PageEvent.PageEventConverter.toEvent(item);
    }
}

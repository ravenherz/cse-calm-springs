package com.ravenherz.cse.dal.dto.events;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.dto.basic.PageData;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageEventTest {

    @Test
    void missingCommentAuthorDoesNotFailPageConversion() {
        AccountEntity author = new AccountEntity(new AccountData("ada", "hash", "ada@example.com",
                SecurityLevel.ACTIVE_USER));
        PageData pageData = new PageData("H", "S", "Body", List.of());
        List<PageData.Comment> comments = new ArrayList<>();
        comments.add(new PageData.Comment(author, LocalDateTime.of(2026, 9, 13, 15, 17), "hi"));
        comments.add(new PageData.Comment(null, LocalDateTime.of(2026, 8, 1, 9, 0), "orphan"));
        comments.add(new PageData.Comment(new AccountEntity(), LocalDateTime.of(2026, 7, 1, 9, 0),
                "empty account"));
        pageData.setComments(comments);

        ItemEntity item = new ItemEntity("hello", pageData, author);
        item.setId(new ObjectId());

        PageEvent event = PageEvent.PageEventConverter.toEvent(item);

        assertEquals(3, event.getPageComments().size());
        assertEquals("ada", event.getPageComments().get(0).getAuthor());
        assertEquals("Unknown", event.getPageComments().get(1).getAuthor());
        assertEquals("Unknown", event.getPageComments().get(2).getAuthor());
        assertEquals("orphan", event.getPageComments().get(1).getMessage());
        assertFalse(event.getPageComments().get(1).getDate().isBlank());
    }

    @Test
    void copiesNoTopDisplayImageFromPageData() {
        AccountEntity author = new AccountEntity(new AccountData("ada", "hash", "ada@example.com",
                SecurityLevel.ACTIVE_USER));
        PageData pageData = new PageData("H", "S", "Body", List.of());
        pageData.setNoTopDisplayImage(true);
        ItemEntity item = new ItemEntity("hello", pageData, author);
        item.setId(new ObjectId());

        PageEvent event = PageEvent.PageEventConverter.toEvent(item);

        assertTrue(event.isNoTopDisplayImage());
    }

    @Test
    void noTopDisplayImageDefaultsFalse() {
        AccountEntity author = new AccountEntity(new AccountData("ada", "hash", "ada@example.com",
                SecurityLevel.ACTIVE_USER));
        PageData pageData = new PageData("H", "S", "Body", List.of());
        ItemEntity item = new ItemEntity("hello", pageData, author);
        item.setId(new ObjectId());

        assertFalse(pageData.isNoTopDisplayImage());
        assertFalse(PageEvent.PageEventConverter.toEvent(item).isNoTopDisplayImage());
    }

    @Test
    void copiesExportPdfFromPageData() {
        AccountEntity author = new AccountEntity(new AccountData("ada", "hash", "ada@example.com",
                SecurityLevel.ACTIVE_USER));
        PageData pageData = new PageData("H", "S", "Body", List.of());
        pageData.setExportPdf(true);
        ItemEntity item = new ItemEntity("hello", pageData, author);
        item.setId(new ObjectId());

        PageEvent event = PageEvent.PageEventConverter.toEvent(item);

        assertTrue(event.isExportPdf());
    }

    @Test
    void exportPdfDefaultsFalse() {
        AccountEntity author = new AccountEntity(new AccountData("ada", "hash", "ada@example.com",
                SecurityLevel.ACTIVE_USER));
        PageData pageData = new PageData("H", "S", "Body", List.of());
        ItemEntity item = new ItemEntity("hello", pageData, author);
        item.setId(new ObjectId());

        assertFalse(pageData.isExportPdf());
        assertFalse(PageEvent.PageEventConverter.toEvent(item).isExportPdf());
    }
}

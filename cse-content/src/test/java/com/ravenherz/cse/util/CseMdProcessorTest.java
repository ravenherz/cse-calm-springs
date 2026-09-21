package com.ravenherz.cse.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CseMdProcessorTest {

    @Test
    void rendersInnerMarkdownAndDefaultsPadding() {
        String html = MarkdownRenderer.render("<cse-md>**Hi**</cse-md>");
        assertTrue(html.contains("class=\"cse-md\""));
        assertTrue(html.contains("padding-left:0px"));
        assertTrue(html.contains("<strong>Hi</strong>"));
        assertFalse(html.contains("<cse-md"));
    }

    @Test
    void appliesPaddingLeft() {
        String html = MarkdownRenderer.render(
                "<cse-md paddingLeft=\"250px\">### Title</cse-md>");
        assertTrue(html.contains("padding-left:250px"));
        assertTrue(html.contains("<h3>Title</h3>"));
    }

    @Test
    void keepsBlankLinesAsParagraphs() {
        String html = MarkdownRenderer.render("<cse-md paddingLeft=\"250\">\n\nhello\n\nworld\n</cse-md>");
        assertTrue(html.contains("<p>hello</p>"));
        assertTrue(html.contains("<p>world</p>"));
    }

    @Test
    void dedentsIndentedBody() {
        String html = MarkdownRenderer.render("<cse-md>\n    ### Title\n    next\n</cse-md>");
        assertTrue(html.contains("<h3>Title</h3>"));
        assertFalse(html.contains("<pre>"));
    }

    @Test
    void leavesSurroundingMarkdown() {
        String html = MarkdownRenderer.render("## Page\n\n<cse-md>**in**</cse-md>");
        assertTrue(html.contains("<h2>Page</h2>"));
        assertTrue(html.contains("<strong>in</strong>"));
    }

    @Test
    void paddingLeftRejectsJunk() {
        assertEquals(0, CseMdProcessor.paddingLeftPx(null));
        assertEquals(0, CseMdProcessor.paddingLeftPx("wide"));
        assertEquals(250, CseMdProcessor.paddingLeftPx("250"));
        assertEquals(4096, CseMdProcessor.paddingLeftPx("9999px"));
    }
}

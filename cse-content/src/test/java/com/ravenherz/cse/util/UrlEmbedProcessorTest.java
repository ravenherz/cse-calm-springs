package com.ravenherz.cse.util;

import com.ravenherz.cse.dal.dto.UrlTemplateEntity;
import com.ravenherz.cse.dal.dto.basic.UrlTemplateData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrlEmbedProcessorTest {

    @Test
    void xsRendersAsPlainLinkWithSubstitutedText() {
        UrlEmbedProcessor processor = UrlEmbedProcessor.of(id -> youtube());
        String html = processor.expandHtml(
                "<cse-url templateId=\"youtube\" id=\"ravenherz\" size=\"xs\"></cse-url>");
        assertTrue(html.contains("class=\"cse-url cse-url-xs\""));
        assertTrue(html.contains("href=\"https://youtube.com/ravenherz\""));
        assertTrue(html.contains("Find more videos on my YouTube channel: ravenherz"));
        assertFalse(html.contains("<img"));
        assertFalse(html.contains("<cse-url"));
    }

    @Test
    void textOverrideWinsAndSecondLineShowsOnMedium() {
        UrlEmbedProcessor processor = UrlEmbedProcessor.of(id -> youtube());
        String html = processor.expandHtml(
                "<cse-url templateId=\"youtube\" id=\"ravenherz\" size=\"m\""
                        + " textOverride=\"My override text\" secondLine=\"Displayed second line\"></cse-url>");
        assertTrue(html.contains("class=\"cse-url cse-url-m\""));
        assertTrue(html.contains("cse-url-title"));
        assertTrue(html.contains("My override text"));
        assertFalse(html.contains("Find more videos"));
        assertTrue(html.contains("cse-url-second"));
        assertTrue(html.contains("Displayed second line"));
        assertTrue(html.contains("cse-url-media"));
    }

    @Test
    void largeIsFullWidthBlock() {
        UrlEmbedProcessor processor = UrlEmbedProcessor.of(id -> youtube());
        String html = processor.expandHtml(
                "<cse-url templateId=\"youtube\" id=\"ravenherz\" size=\"l\"></cse-url>");
        assertTrue(html.contains("class=\"cse-url cse-url-l\""));
        assertTrue(html.contains("Find more videos on my YouTube channel: ravenherz"));
    }

    @Test
    void smallRendersIconLink() {
        UrlEmbedProcessor processor = UrlEmbedProcessor.of(id -> youtube());
        String html = processor.expandHtml(
                "<cse-url templateId=\"youtube\" id=\"ravenherz\" size=\"s\"></cse-url>");
        assertTrue(html.contains("class=\"cse-url cse-url-s\""));
        assertTrue(html.contains("<img"));
        assertTrue(html.contains("title=\"Find more videos on my YouTube channel: ravenherz\""));
        assertFalse(html.contains("cse-url-title"));
    }

    @Test
    void containerOverrideAppliesToChildren() {
        UrlEmbedProcessor processor = UrlEmbedProcessor.of(id -> youtube());
        String html = processor.expandHtml(
                "<cse-urls sizeOverride=\"s\">"
                        + "<cse-url templateId=\"youtube\" id=\"a\" size=\"m\"></cse-url>"
                        + "<cse-url templateId=\"youtube\" id=\"b\"></cse-url>"
                        + "</cse-urls>");
        assertTrue(html.contains("class=\"cse-urls cse-urls-s\""));
        assertEquals(2, count(html, "cse-url-s"));
        assertFalse(html.contains("cse-url-m"));
        assertFalse(html.contains("<cse-url"));
        assertFalse(html.contains("<cse-urls"));
    }

    @Test
    void defaultSizeIsMediumCard() {
        UrlEmbedProcessor processor = UrlEmbedProcessor.of(id -> youtube());
        String html = processor.expandHtml(
                "<cse-url templateId=\"youtube\" id=\"ravenherz\"></cse-url>");
        assertTrue(html.contains("class=\"cse-url cse-url-m\""));
        assertTrue(html.contains("Find more videos on my YouTube channel: ravenherz"));
    }

    @Test
    void overrideUrlReplacesPatternHref() {
        UrlEmbedProcessor processor = UrlEmbedProcessor.of(id -> youtube());
        String html = processor.expandHtml(
                "<cse-url templateId=\"youtube\" id=\"ravenherz\" size=\"xs\""
                        + " overrideUrl=\"https://bandcamp.com/album/tracks\"></cse-url>");
        assertTrue(html.contains("href=\"https://bandcamp.com/album/tracks\""));
        assertFalse(html.contains("https://youtube.com/ravenherz"));
        assertTrue(html.contains("Find more videos on my YouTube channel: ravenherz"));
    }

    @Test
    void overrideUrlJavascriptIsRejected() {
        UrlEmbedProcessor processor = UrlEmbedProcessor.of(id -> youtube());
        String html = processor.expandHtml(
                "<cse-url templateId=\"youtube\" id=\"ravenherz\" size=\"xs\""
                        + " overrideUrl=\"javascript:alert(1)\"></cse-url>");
        assertTrue(html.contains("href=\"#\""));
        assertFalse(html.toLowerCase().contains("javascript:"));
    }

    @Test
    void javascriptPatternIsRejected() {
        UrlEmbedProcessor processor = UrlEmbedProcessor.of(id -> {
            UrlTemplateData data = new UrlTemplateData();
            data.setUrlPattern("javascript:alert(%s)");
            data.setUrlDefaultText("bad");
            return new UrlTemplateEntity("evil", data, null);
        });
        String html = processor.expandHtml(
                "<cse-url templateId=\"evil\" id=\"1\" size=\"xs\"></cse-url>");
        assertTrue(html.contains("href=\"#\""));
        assertFalse(html.toLowerCase().contains("javascript:"));
    }

    @Test
    void missingTemplateShowsFallback() {
        UrlEmbedProcessor processor = UrlEmbedProcessor.of(id -> null);
        String html = processor.expandHtml("<cse-url templateId=\"missing\" id=\"x\"></cse-url>");
        assertTrue(html.contains("URL template not found"));
        assertTrue(html.contains("cse-url-missing"));
    }

    private static int count(String html, String needle) {
        int n = 0;
        int from = 0;
        while (true) {
            int at = html.indexOf(needle, from);
            if (at < 0) {
                return n;
            }
            n++;
            from = at + needle.length();
        }
    }

    private static UrlTemplateEntity youtube() {
        UrlTemplateData data = new UrlTemplateData();
        data.setUrlPattern("https://youtube.com/%s");
        data.setUrlDefaultText("Find more videos on my YouTube channel: %s");
        data.setUrlImage("data:image/jpeg;base64,aaaa");
        UrlTemplateEntity entity = new UrlTemplateEntity("youtube", data, null);
        return entity;
    }
}

package com.ravenherz.cse.util;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanySocialMarkupTest {

    @Test
    void parsesLegacyPairsIntoSmallUrlContainer() {
        String html = CompanySocialMarkup.toCseUrls("vk:ravenherz youtube:ravenherz");
        assertEquals("<cse-urls sizeOverride=\"s\">"
                + "<cse-url templateId=\"vk\" id=\"ravenherz\"></cse-url>"
                + "<cse-url templateId=\"youtube\" id=\"ravenherz\"></cse-url>"
                + "</cse-urls>", html);
    }

    @Test
    void mapsTwitterToXAndSkipsJunk() {
        String html = CompanySocialMarkup.toCseUrls(
                "twitter:ravenherz  ::  bad  vk:  VK:other youtube:ravenherz twitter:ravenherz");
        assertTrue(html.contains("templateId=\"x\""));
        assertTrue(html.contains("id=\"ravenherz\""));
        assertTrue(html.contains("templateId=\"vk\""));
        assertTrue(html.contains("id=\"other\""));
        assertTrue(html.contains("templateId=\"youtube\""));
        assertFalse(html.contains("twitter"));
        assertEquals(1, html.split("templateId=\"x\"", -1).length - 1);
    }

    @Test
    void includePredicateDropsUnknownTemplates() {
        String html = CompanySocialMarkup.toCseUrls("vk:ravenherz soundcloud:ravenherz",
                Set.of("vk")::contains);
        assertTrue(html.contains("templateId=\"vk\""));
        assertFalse(html.contains("soundcloud"));
    }

    @Test
    void blankIsEmpty() {
        assertEquals("", CompanySocialMarkup.toCseUrls(null));
        assertEquals("", CompanySocialMarkup.toCseUrls("   "));
        assertEquals("", CompanySocialMarkup.toCseUrls("not-a-pair"));
    }
}

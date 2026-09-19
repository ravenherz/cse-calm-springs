package com.ravenherz.cse.util.html.impl;

import com.ravenherz.cse.dal.dao.UrlTemplateService;
import com.ravenherz.cse.dal.dto.UrlTemplateEntity;
import com.ravenherz.cse.dal.dto.basic.UrlTemplateData;
import com.ravenherz.cse.util.UrlEmbedProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TagCompanySocialLinksTest {

    @Test
    void rendersLegacyStringWithUrlTags() {
        UrlTemplateService templates = mock(UrlTemplateService.class);
        when(templates.getByUrlTemplateId("vk")).thenReturn(vk());
        when(templates.getByUrlTemplateId("x")).thenReturn(x());
        @SuppressWarnings("unchecked")
        ObjectProvider<UrlTemplateService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(templates);

        TagCompanySocialLinks tag = new TagCompanySocialLinks(
                UrlEmbedProcessor.of(templates::getByUrlTemplateId), provider);
        String html = tag.getContents("vk:ravenherz twitter:ravenherz soundcloud:ravenherz");

        assertTrue(html.contains("class=\"cse-urls cse-urls-s\""));
        assertTrue(html.contains("cse-url-s"));
        assertTrue(html.contains("https://vk.com/ravenherz"));
        assertTrue(html.contains("https://x.com/ravenherz"));
        assertFalse(html.contains("soundcloud"));
        assertFalse(html.contains("<cse-url"));
        assertFalse(html.contains("company-social-vk"));
    }

    @Test
    void wrapperKeepsCompanySocialClass() {
        @SuppressWarnings("unchecked")
        ObjectProvider<UrlTemplateService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        TagCompanySocialLinks tag = new TagCompanySocialLinks(UrlEmbedProcessor.of(id -> null), provider);
        String html = tag.getContainer("vk:ravenherz").getCode();
        assertTrue(html.contains("id=\"company-social-container\""));
        assertTrue(html.contains("company-social"));
        assertTrue(html.contains("name=\"highlightable\""));
    }

    private static UrlTemplateEntity vk() {
        UrlTemplateData data = new UrlTemplateData();
        data.setUrlPattern("https://vk.com/%s");
        data.setUrlDefaultText("VK %s");
        data.setUrlImage("data:image/png;base64,aa");
        return new UrlTemplateEntity("vk", data, null);
    }

    private static UrlTemplateEntity x() {
        UrlTemplateData data = new UrlTemplateData();
        data.setUrlPattern("https://x.com/%s");
        data.setUrlDefaultText("X %s");
        data.setUrlImage("data:image/png;base64,aa");
        return new UrlTemplateEntity("x", data, null);
    }
}

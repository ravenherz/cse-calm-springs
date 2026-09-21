package com.ravenherz.cse.install;

import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.dao.AccountService;
import com.ravenherz.cse.dal.dao.UrlTemplateService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.UrlTemplateEntity;
import com.ravenherz.cse.dal.dto.basic.SecurityData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlTemplateSeedsTest {

    @Mock
    private ServiceProvider serviceProvider;
    @Mock
    private AccountService accountService;
    @Mock
    private UrlTemplateService urlTemplateService;

    @Test
    void loadInitUsesInheritAccessAndShippedIds() throws Exception {
        List<UrlTemplateEntity> templates = UrlTemplateSeeds.loadInit(new AccountEntity());
        assertEquals(List.of("youtube", "linkedin", "instagram", "facebook", "vk", "telegram",
                "x", "github", "bandcamp", "spotify", "itunes"),
                templates.stream().map(UrlTemplateEntity::getUrlTemplateId).toList());
        for (UrlTemplateEntity template : templates) {
            assertInherit(template);
            assertTrue(template.getUrlTemplateData().getUrlPattern().contains("%s"));
            assertTrue(template.getUrlTemplateData().getUrlImage().startsWith("data:image/"));
        }
    }

    @Test
    void parseSkipsInvalidDuplicateAndIgnoresJsonSecurity() throws Exception {
        String json = """
                [
                  {"urlTemplateId": "youtube", "urlTemplateData": {"urlPattern": "https://youtube.com/%s"}},
                  {"urlTemplateId": "YOUTUBE", "urlTemplateData": {"urlPattern": "https://youtu.be/%s"}},
                  {"urlTemplateId": "Bad Id", "urlTemplateData": {"urlPattern": "https://x.com/%s"}},
                  {"urlTemplateId": "x", "urlTemplateData": {"urlPattern": ""}},
                  {
                    "urlTemplateId": "telegram",
                    "securityData": {"accessSettings": {"ACCESS_READ": {"inherit": false}}},
                    "urlTemplateData": {"urlPattern": "https://t.me/%s", "urlDefaultText": "Hi"}
                  }
                ]
                """;
        List<UrlTemplateEntity> templates = UrlTemplateSeeds.parse(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), new AccountEntity());
        assertEquals(List.of("youtube", "telegram"),
                templates.stream().map(UrlTemplateEntity::getUrlTemplateId).toList());
        assertEquals("Hi", templates.get(1).getUrlTemplateData().getUrlDefaultText());
        assertInherit(templates.get(1));
    }

    @Test
    void ensureSeededInsertsWhenCollectionIsEmpty() {
        AccountEntity creator = new AccountEntity();
        when(serviceProvider.getUrlTemplateService()).thenReturn(urlTemplateService);
        when(urlTemplateService.getAllUrlTemplates()).thenReturn(List.of());
        when(serviceProvider.getAccountService()).thenReturn(accountService);
        when(accountService.getAllAccounts()).thenReturn(List.of(creator));

        int inserted = new UrlTemplateSeeds(serviceProvider).ensureSeeded();

        assertEquals(11, inserted);
        ArgumentCaptor<UrlTemplateEntity> captor = ArgumentCaptor.forClass(UrlTemplateEntity.class);
        verify(urlTemplateService, times(11)).insert(captor.capture());
        assertInherit(captor.getAllValues().get(0));
        assertEquals("youtube", captor.getAllValues().get(0).getUrlTemplateId());
    }

    @Test
    void ensureSeededSkipsWhenCollectionHasRows() {
        when(serviceProvider.getUrlTemplateService()).thenReturn(urlTemplateService);
        when(urlTemplateService.getAllUrlTemplates()).thenReturn(List.of(new UrlTemplateEntity()));

        assertEquals(0, new UrlTemplateSeeds(serviceProvider).ensureSeeded());
        verify(urlTemplateService, never()).insert(any());
    }

    @Test
    void ensureSeededSkipsWhenNoAccounts() {
        when(serviceProvider.getUrlTemplateService()).thenReturn(urlTemplateService);
        when(urlTemplateService.getAllUrlTemplates()).thenReturn(List.of());
        when(serviceProvider.getAccountService()).thenReturn(accountService);
        when(accountService.getAllAccounts()).thenReturn(List.of());

        assertEquals(0, new UrlTemplateSeeds(serviceProvider).ensureSeeded());
        verify(urlTemplateService, never()).insert(any());
    }

    private static void assertInherit(UrlTemplateEntity template) {
        SecurityData security = template.getSecurityData();
        assertTrue(security.getRead().isInherit());
        assertTrue(security.getEdit().isInherit());
        assertTrue(security.getDelete().isInherit());
    }
}

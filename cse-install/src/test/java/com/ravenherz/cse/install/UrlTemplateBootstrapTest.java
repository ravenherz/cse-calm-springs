package com.ravenherz.cse.install;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UrlTemplateBootstrapTest {

    @Test
    void configuredBootSeedsEmptyCollection() {
        SiteReady siteReady = mock(SiteReady.class);
        UrlTemplateSeeds seeds = mock(UrlTemplateSeeds.class);
        when(siteReady.isConfigured()).thenReturn(true);

        new UrlTemplateBootstrap(siteReady, seeds).run(null);

        verify(seeds).ensureSeeded();
    }

    @Test
    void unconfiguredBootDoesNotSeed() {
        SiteReady siteReady = mock(SiteReady.class);
        UrlTemplateSeeds seeds = mock(UrlTemplateSeeds.class);
        when(siteReady.isConfigured()).thenReturn(false);

        new UrlTemplateBootstrap(siteReady, seeds).run(null);

        verify(seeds, never()).ensureSeeded();
    }
}

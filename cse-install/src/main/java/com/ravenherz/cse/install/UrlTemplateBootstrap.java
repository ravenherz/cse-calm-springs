package com.ravenherz.cse.install;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
class UrlTemplateBootstrap implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(UrlTemplateBootstrap.class);

    private final SiteReady siteReady;
    private final UrlTemplateSeeds seeds;

    public UrlTemplateBootstrap(SiteReady siteReady, UrlTemplateSeeds seeds) {
        this.siteReady = siteReady;
        this.seeds = seeds;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!siteReady.isConfigured()) {
            LOGGER.info("Skip URL template seed: engine is not configured");
            return;
        }
        seeds.ensureSeeded();
    }
}

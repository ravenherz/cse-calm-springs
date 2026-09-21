package com.ravenherz.cse.util.staticapps;

import com.ravenherz.cse.core.SiteConfigured;
import com.ravenherz.cse.dal.dao.AppService;
import com.ravenherz.cse.dal.dto.AppEntity;
import com.ravenherz.cse.util.frontend.ShippedPackCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component
@DependsOn("appRootsBinder")
class StaticAppBootstrap implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(StaticAppBootstrap.class);

    private final SiteConfigured siteReady;
    private final ObjectProvider<AppService> appService;
    private final StaticAppDeployer deployer;

    public StaticAppBootstrap(SiteConfigured siteReady,
            ObjectProvider<AppService> appService,
            StaticAppDeployer deployer) {
        this.siteReady = siteReady;
        this.appService = appService;
        this.deployer = deployer;
    }

    @Override
    public void run(ApplicationArguments args) {
        deployShipped();
        if (!siteReady.isConfigured()) {
            LOGGER.info("Skip Mongo static app deploy: engine is not configured");
            return;
        }
        try {
            AppService service = appService.getObject();
            for (AppEntity app : service.getAllApps()) {
                if (app.getAppData() == null || app.getAppData().getSlug() == null) {
                    continue;
                }
                String slug = app.getAppData().getSlug();
                try {
                    byte[] zip = service.loadZipBytes(app);
                    if (zip == null || zip.length == 0) {
                        LOGGER.warn("Static app '{}' has no zip payload", slug);
                        continue;
                    }
                    deployer.deploy(slug, zip);
                } catch (Exception ex) {
                    LOGGER.error("Failed to deploy static app '{}'", slug, ex);
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Skip Mongo static app deploy: {}", ex.getMessage());
        }
    }

    private void deployShipped() {
        for (ShippedPackCatalog.Pack pack : ShippedPackCatalog.apps()) {
            AppManifest manifest = deployer.readManifest(pack.bytes());
            String slug = manifest != null && manifest.getSlug() != null ? manifest.getSlug() : pack.stem();
            if (StaticAppDeployer.INSTALLER_SLUG.equals(slug)) {
                continue;
            }
            try {
                deployer.deployEngineApp(slug, pack.bytes());
            } catch (Exception ex) {
                LOGGER.error("Failed to deploy shipped app '{}'", slug, ex);
            }
        }
    }
}

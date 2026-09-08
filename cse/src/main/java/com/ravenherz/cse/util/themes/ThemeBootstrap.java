package com.ravenherz.cse.util.themes;

import com.ravenherz.cse.dal.dao.ThemeService;
import com.ravenherz.cse.dal.dto.ThemeEntity;
import com.ravenherz.cse.install.SiteReady;
import com.ravenherz.cse.util.frontend.ShippedPackCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class ThemeBootstrap implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThemeBootstrap.class);

    private final SiteReady siteReady;
    private final ObjectProvider<ThemeService> themeService;
    private final ThemePackDeployer deployer;

    public ThemeBootstrap(SiteReady siteReady,
            ObjectProvider<ThemeService> themeService,
            ThemePackDeployer deployer) {
        this.siteReady = siteReady;
        this.themeService = themeService;
        this.deployer = deployer;
    }

    @Override
    public void run(ApplicationArguments args) {
        pruneOrphans();
        deployShipped();
        if (!siteReady.isConfigured()) {
            LOGGER.info("Skip Mongo theme pack deploy: engine is not configured");
            return;
        }
        try {
            ThemeService service = themeService.getObject();
            for (ThemeEntity theme : service.getAllThemes()) {
                if (theme.getThemeData() == null || theme.getThemeData().getThemeId() == null) {
                    continue;
                }
                String id = theme.getThemeData().getThemeId();
                try {
                    byte[] zip = service.loadZipBytes(theme);
                    if (zip == null || zip.length == 0) {
                        LOGGER.warn("Theme pack '{}' has no zip payload", id);
                        continue;
                    }
                    deployer.deploy(id, zip);
                } catch (Exception ex) {
                    LOGGER.error("Failed to deploy theme pack '{}'", id, ex);
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Skip Mongo theme pack deploy: {}", ex.getMessage());
        }
    }

    private void pruneOrphans() {
        Set<String> keep = new HashSet<>();
        for (ShippedPackCatalog.Pack pack : ShippedPackCatalog.themes()) {
            ThemeManifest manifest = deployer.readManifest(pack.bytes());
            String id = manifest != null && manifest.getId() != null ? manifest.getId() : pack.stem();
            if (id != null && !id.isBlank()) {
                keep.add(id.toLowerCase(Locale.ROOT));
            }
        }
        if (siteReady.isConfigured()) {
            try {
                ThemeService service = themeService.getObject();
                for (ThemeEntity theme : service.getAllThemes()) {
                    if (theme.getThemeData() == null || theme.getThemeData().getThemeId() == null) {
                        continue;
                    }
                    keep.add(theme.getThemeData().getThemeId().toLowerCase(Locale.ROOT));
                }
            } catch (Exception ex) {
                LOGGER.warn("Skip theme prune against Mongo: {}", ex.getMessage());
            }
        }
        try {
            deployer.retainOnly(keep);
        } catch (Exception ex) {
            LOGGER.warn("Could not prune leftover theme trees: {}", ex.getMessage());
        }
    }

    private void deployShipped() {
        for (ShippedPackCatalog.Pack pack : ShippedPackCatalog.themes()) {
            ThemeManifest manifest = deployer.readManifest(pack.bytes());
            String id = manifest != null && manifest.getId() != null ? manifest.getId() : pack.stem();
            try {
                deployer.deployEngineTheme(id, pack.bytes());
            } catch (Exception ex) {
                LOGGER.error("Failed to deploy shipped theme '{}'", id, ex);
            }
        }
    }
}

package com.ravenherz.cse.util.themes;

import com.ravenherz.cse.core.SiteConfigured;
import com.ravenherz.cse.dal.dao.ThemeService;
import com.ravenherz.cse.dal.dto.ThemeEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@DependsOn("themeRootsBinder")
class ThemeBootstrap implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThemeBootstrap.class);

    private final SiteConfigured siteReady;
    private final ObjectProvider<ThemeService> themeService;
    private final ThemePackDeployer deployer;
    private final ShippedThemes shipped;

    public ThemeBootstrap(SiteConfigured siteReady,
            ObjectProvider<ThemeService> themeService,
            ThemePackDeployer deployer,
            ObjectProvider<ShippedThemes> shippedThemes) {
        this.siteReady = siteReady;
        this.themeService = themeService;
        this.deployer = deployer;
        ShippedThemes resolved = shippedThemes == null ? null : shippedThemes.getIfAvailable();
        this.shipped = resolved == null ? List::of : resolved;
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
        for (ShippedThemes.Pack pack : shipped.packs()) {
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
        for (ShippedThemes.Pack pack : shipped.packs()) {
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

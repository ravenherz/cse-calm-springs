package com.ravenherz.cse.present;

import com.ravenherz.cse.dal.dao.AppService;
import com.ravenherz.cse.util.CseEmbedProcessor;
import com.ravenherz.cse.util.staticapps.StaticAppDeployer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class CatalogAppLookup implements CseEmbedProcessor.AppLookup {

    private final AppService apps;
    private final StaticAppDeployer deployer;

    public CatalogAppLookup(ObjectProvider<AppService> apps, ObjectProvider<StaticAppDeployer> deployer) {
        this.apps = apps == null ? null : apps.getIfAvailable();
        this.deployer = deployer == null ? null : deployer.getIfAvailable();
    }

    @Override
    public CseEmbedProcessor.AppCard findBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return null;
        }
        String needle = slug.trim().toLowerCase(Locale.ROOT);
        List<AppDisplayDTO> all = EditorContentCatalog.apps(apps, deployer);
        if (all == null) {
            return null;
        }
        for (AppDisplayDTO app : all) {
            if (app == null || app.getSlug() == null || !needle.equals(app.getSlug().toLowerCase(Locale.ROOT))) {
                continue;
            }
            return new CseEmbedProcessor.AppCard(app.getSlug(), app.getName(), app.getDescription(),
                    app.getVersion(), app.getAuthor(), app.isProductLogo());
        }
        return null;
    }
}

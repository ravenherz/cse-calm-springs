package com.ravenherz.cse.present;

import com.ravenherz.cse.dal.dao.AppService;
import com.ravenherz.cse.dal.dao.ThemeService;
import com.ravenherz.cse.dal.dto.AppEntity;
import com.ravenherz.cse.dal.dto.ThemeEntity;
import com.ravenherz.cse.dal.dto.basic.ThemeData;
import com.ravenherz.cse.util.frontend.ShippedPackCatalog;
import com.ravenherz.cse.util.staticapps.AppManifest;
import com.ravenherz.cse.util.staticapps.StaticAppDeployer;
import com.ravenherz.cse.util.themes.ThemeCatalog;
import com.ravenherz.cse.util.themes.ThemeInfo;
import com.ravenherz.cse.util.themes.ThemeSelection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Display lists for Catalog Apps and Themes panes.
 */
public final class EditorContentCatalog {

    private EditorContentCatalog() {
    }

    public static List<AppDisplayDTO> apps(AppService apps, StaticAppDeployer deployer) {
        List<AppDisplayDTO> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (ShippedPackCatalog.Pack pack : ShippedPackCatalog.apps()) {
            AppDisplayDTO dto = fromShipped(pack, deployer);
            if (dto.getSlug() == null || !seen.add(dto.getSlug())) {
                continue;
            }
            out.add(dto);
        }
        if (apps == null) {
            return out;
        }
        List<AppEntity> stored = apps.getAllApps();
        if (stored == null) {
            return out;
        }
        for (AppEntity app : stored) {
            if (app == null || app.getAppData() == null || app.getAppData().getSlug() == null) {
                continue;
            }
            String slug = app.getAppData().getSlug().toLowerCase(Locale.ROOT);
            if (!seen.add(slug)) {
                continue;
            }
            out.add(fromMongo(app, deployer));
        }
        return out;
    }

    public static List<ThemeDisplayDTO> themes(ThemeCatalog catalog, ThemeService themes) {
        if (catalog == null) {
            return List.of();
        }
        List<ThemeDisplayDTO> out = new ArrayList<>();
        ThemeSelection active = catalog.resolve();
        Map<String, ThemeEntity> installed = new HashMap<>();
        if (themes != null) {
            List<ThemeEntity> stored = themes.getAllThemes();
            if (stored != null) {
                for (ThemeEntity theme : stored) {
                    if (theme != null && theme.getThemeData() != null
                            && theme.getThemeData().getThemeId() != null) {
                        installed.put(theme.getThemeData().getThemeId().toLowerCase(Locale.ROOT), theme);
                    }
                }
            }
        }
        for (ThemeInfo info : catalog.list()) {
            if (info == null || info.getId() == null || info.getId().isBlank()) {
                continue;
            }
            ThemeDisplayDTO dto = new ThemeDisplayDTO();
            dto.setThemeId(info.getId());
            dto.setTitle(info.getTitle());
            dto.setAuthor(info.getAuthor());
            dto.setDescription(info.getDescription());
            dto.setShell(info.getShell());
            dto.setDefaultSchema(info.getDefaultSchema());
            dto.setSchemas(info.getSchemas());
            dto.setBuiltin(info.isBuiltin());
            dto.setHasPreview(info.hasPreview());
            dto.setSortOrder(info.getSortOrder());
            dto.setActive(info.getId().equalsIgnoreCase(active.getCssId()));
            ThemeEntity entity = installed.get(info.getId().toLowerCase(Locale.ROOT));
            if (entity != null && entity.getThemeData() != null) {
                ThemeData data = entity.getThemeData();
                dto.setId(entity.getId() == null ? null : entity.getId().toString());
                dto.setOriginalFilename(data.getOriginalFilename());
                dto.setSizeInBytes(data.getSizeInBytes());
            }
            out.add(dto);
        }
        return out;
    }

    public static String publicUrlFor(String slug) {
        if (StaticAppDeployer.ADMIN_SLUG.equals(slug)) {
            return "/editor/resources";
        }
        return "/apps/" + slug + "/";
    }

    private static AppDisplayDTO fromShipped(ShippedPackCatalog.Pack pack, StaticAppDeployer deployer) {
        AppManifest manifest = deployer == null ? null : deployer.readManifest(pack.bytes());
        String slug = manifest != null && manifest.getSlug() != null
                ? manifest.getSlug().toLowerCase(Locale.ROOT)
                : pack.stem();
        AppDisplayDTO dto = new AppDisplayDTO();
        dto.setBundled(true);
        dto.setSlug(slug);
        dto.setOriginalFilename(pack.filename());
        dto.setSizeInBytes(pack.size());
        if (manifest != null) {
            dto.setName(manifest.getName());
            dto.setVersion(manifest.getVersion());
            dto.setAuthor(manifest.getAuthor());
            dto.setCompany(manifest.getCompany());
            dto.setDescription(manifest.getDescription());
        }
        dto.setPublicUrl(publicUrlFor(slug));
        dto.setProductLogo(deployer != null && deployer.hasValidProductLogo(slug));
        return dto;
    }

    private static AppDisplayDTO fromMongo(AppEntity app, StaticAppDeployer deployer) {
        AppDisplayDTO dto = new AppDisplayDTO();
        dto.setId(app.getId() == null ? null : app.getId().toString());
        dto.setSlug(app.getAppData().getSlug());
        dto.setOriginalFilename(app.getAppData().getOriginalFilename());
        dto.setSizeInBytes(app.getAppData().getSizeInBytes());
        dto.setLargeFile(app.getAppData().isLargeFile());
        List<String> chunkIds = new ArrayList<>();
        if (app.getAppData().getDataChunkIds() != null) {
            for (var chunkId : app.getAppData().getDataChunkIds()) {
                if (chunkId != null) {
                    chunkIds.add(chunkId.toString());
                }
            }
        }
        dto.setChunkIds(chunkIds);
        dto.setPublicUrl(publicUrlFor(app.getAppData().getSlug()));
        dto.setName(app.getAppData().getAppName());
        dto.setVersion(app.getAppData().getAppVersion());
        dto.setAuthor(app.getAppData().getAuthor());
        dto.setCompany(app.getAppData().getCompany());
        dto.setDescription(app.getAppData().getDescription());
        dto.setProductLogo(deployer != null && deployer.hasValidProductLogo(app.getAppData().getSlug()));
        dto.setStoreEnabled(app.getAppData().isStoreEnabled());
        dto.setStoreOpen(app.getAppData().isStoreOpen());
        dto.setStoreRequested(app.getAppData().requestsStore());
        var store = app.getAppData().storeSettings();
        dto.setStoreMaxDataKb(store.maxDataKb());
        dto.setStoreMaxDocs(store.resolvedMaxDocs());
        dto.setStoreMaxBytesMb(store.maxBytesMb());
        return dto;
    }
}

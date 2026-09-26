package com.ravenherz.cse.controller;

import com.ravenherz.cse.dal.EntityAccess;
import com.ravenherz.cse.dal.StoredIds;
import com.ravenherz.cse.dal.ResourceGroupTree;
import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.AppEntity;
import com.ravenherz.cse.dal.dto.CategoryEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.PlaylistEntity;
import com.ravenherz.cse.dal.dto.UrlTemplateEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.ResourceGroupEntity;
import com.ravenherz.cse.dal.dto.ThemeEntity;
import com.ravenherz.cse.dal.dto.basic.enums.AccessType;
import com.ravenherz.cse.dal.role.CapabilityIds;
import com.ravenherz.cse.present.ResourceGroupIndex;
import com.ravenherz.cse.security.CapabilityService;
import com.ravenherz.cse.util.frontend.ShippedPackCatalog;
import com.ravenherz.cse.util.staticapps.StaticAppDeployer;
import com.ravenherz.cse.util.themes.ThemeCatalog;
import com.ravenherz.cse.util.themes.ThemePackDeployer;
import com.ravenherz.cse.util.themes.ThemeSelection;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

@Component
public class CatalogBatchDelete {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogBatchDelete.class);

    @Autowired
    private ServiceProvider serviceProvider;

    @Autowired
    private ResourceGroupIndex resourceGroupIndex;

    @Autowired
    @Lazy
    private ContentProtectedAndCacheController contentCacheController;

    @Autowired
    private StaticAppDeployer staticAppDeployer;

    @Autowired
    private ThemePackDeployer themePackDeployer;

    @Autowired
    private ThemeCatalog themeCatalog;

    @Autowired(required = false)
    private CapabilityService capabilityService;

    public String deleteAll(List<String> kinds, List<String> ids, AccountEntity accessor) {
        if (kinds == null || ids == null || kinds.size() != ids.size()) {
            return "invalid_item";
        }
        List<Target> rest = new ArrayList<>();
        List<Target> groups = new ArrayList<>();
        for (int i = 0; i < kinds.size(); i++) {
            String kind = kinds.get(i) == null ? "" : kinds.get(i).trim().toLowerCase(Locale.ROOT);
            String id = ids.get(i) == null ? "" : ids.get(i).trim();
            if (kind.isEmpty() || id.isEmpty() || "folder".equals(kind)) {
                continue;
            }
            Target target = new Target(kind, id);
            if ("group".equals(kind)) {
                groups.add(target);
            } else {
                rest.add(target);
            }
        }
        rest.sort((a, b) -> Integer.compare(rank(a.kind), rank(b.kind)));
        String firstError = null;
        int missed = 0;
        for (Target target : rest) {
            String error = deleteOne(target.kind, target.id, accessor);
            if ("not_found".equals(error)) {
                missed++;
            } else {
                firstError = keepFirst(firstError, error);
            }
        }
        String groupsError = deleteGroups(groups, accessor, firstError);
        if (groupsError == null && missed > 0 && missed == rest.size() && groups.isEmpty()) {
            return "not_found";
        }
        return groupsError;
    }

    private String deleteGroups(List<Target> groups, AccountEntity accessor, String firstError) {
        List<Target> remaining = new ArrayList<>(groups);
        boolean progress = true;
        while (!remaining.isEmpty() && progress) {
            progress = false;
            Iterator<Target> it = remaining.iterator();
            while (it.hasNext()) {
                Target target = it.next();
                String error = deleteOne("group", target.id, accessor);
                if (error == null || "not_found".equals(error)) {
                    it.remove();
                    progress = true;
                } else if (!"has_children".equals(error)) {
                    firstError = keepFirst(firstError, error);
                }
            }
        }
        if (!remaining.isEmpty()) {
            return keepFirst(firstError, "has_children");
        }
        return firstError;
    }

    private String deleteOne(String kind, String id, AccountEntity accessor) {
        try {
            return switch (kind) {
                case "resource" -> deleteResource(id, accessor);
                case "page", "album" -> deletePage(id, accessor);
                case "playlist" -> deletePlaylist(id, accessor);
                case "url-template" -> deleteUrlTemplate(id, accessor);
                case "category" -> deleteCategory(id, accessor);
                case "app" -> deleteApp(id, accessor);
                case "theme" -> deleteTheme(id, accessor);
                case "group" -> deleteGroup(id, accessor);
                default -> "invalid_item";
            };
        } catch (Exception e) {
            LOGGER.error("Batch delete failed for {} {}: {}", kind, id, e.getMessage(), e);
            return "invalid_item";
        }
    }

    private String deleteResource(String id, AccountEntity accessor) {
        if (!allows(accessor, CapabilityIds.EDITOR_FILES)) {
            return "forbidden";
        }
        ResourceEntity existing = findResource(id);
        if (existing == null) {
            return "not_found";
        }
        String pathPublic = existing.getResourceData() == null ? null : existing.getResourceData().getPathPublic();
        LOGGER.info("Deleting resource: {} with id: {}", pathPublic, existing.getId());
        List<ItemEntity> itemsWithRefImage = serviceProvider.getItemService().getAllByRefImage(existing.getId());
        if (itemsWithRefImage != null) {
            for (ItemEntity item : itemsWithRefImage) {
                if (item.getPageData() != null) {
                    item.getPageData().setRefImageId(null);
                    serviceProvider.getItemService().replace(item);
                }
            }
        }
        List<PlaylistEntity> playlistsWithCover = serviceProvider.getPlaylistService().getAllByRefImage(existing.getId());
        if (playlistsWithCover != null) {
            for (PlaylistEntity playlist : playlistsWithCover) {
                if (playlist.getPlaylistData() != null) {
                    playlist.getPlaylistData().setRefImageId(null);
                    serviceProvider.getPlaylistService().replace(playlist);
                }
            }
            if (!playlistsWithCover.isEmpty()) {
                resourceGroupIndex.contentChanged();
            }
        }
        if (pathPublic != null && !pathPublic.isBlank()) {
            serviceProvider.getResourceService().deleteByPublicPath(pathPublic);
            contentCacheController.invalidateCacheForResource(pathPublic);
        } else {
            serviceProvider.getResourceService().delete(existing);
        }
        if (existing.getPreviewData() != null && existing.getPreviewData().getPathPublic() != null) {
            contentCacheController.invalidateCacheForResource(existing.getPreviewData().getPathPublic());
        }
        resourceGroupIndex.fileRemoved(existing);
        return null;
    }

    private ResourceEntity findResource(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        String value = id.trim();
        ResourceEntity byPath = serviceProvider.getResourceService().getByPublicPath(value);
        if (byPath != null) {
            return byPath;
        }
        ObjectId objectId = parseObjectId(value);
        if (objectId == null) {
            return null;
        }
        Object row = serviceProvider.getResourceService().getById(ResourceEntity.class,
                StoredIds.entityId(objectId));
        return row instanceof ResourceEntity resource ? resource : null;
    }

    private String deletePage(String name, AccountEntity accessor) {
        if (!allows(accessor, CapabilityIds.EDITOR_PAGES)) {
            return "forbidden";
        }
        ItemEntity item = serviceProvider.getItemService().getByName(name);
        if (item == null) {
            return "not_found";
        }
        if (!EntityAccess.isAccessible(item, AccessType.ACCESS_DELETE, accessor)) {
            return "forbidden";
        }
        serviceProvider.getItemService().delete(item);
        resourceGroupIndex.contentChanged();
        return null;
    }

    private String deletePlaylist(String id, AccountEntity accessor) {
        if (!allows(accessor, CapabilityIds.EDITOR_PLAYLISTS)) {
            return "forbidden";
        }
        ObjectId objectId = parseObjectId(id);
        if (objectId == null) {
            return "invalid_item";
        }
        PlaylistEntity playlist = (PlaylistEntity) serviceProvider.getPlaylistService()
                .getById(PlaylistEntity.class, StoredIds.entityId(objectId));
        if (playlist == null) {
            return "not_found";
        }
        serviceProvider.getPlaylistService().delete(playlist);
        resourceGroupIndex.contentChanged();
        return null;
    }

    private String deleteUrlTemplate(String id, AccountEntity accessor) {
        if (!allows(accessor, CapabilityIds.EDITOR_URL_TEMPLATES)) {
            return "forbidden";
        }
        ObjectId objectId = parseObjectId(id);
        if (objectId == null) {
            return "invalid_item";
        }
        UrlTemplateEntity template = (UrlTemplateEntity) serviceProvider.getUrlTemplateService()
                .getById(UrlTemplateEntity.class, StoredIds.entityId(objectId));
        if (template == null) {
            return "not_found";
        }
        serviceProvider.getUrlTemplateService().delete(template);
        resourceGroupIndex.contentChanged();
        return null;
    }

    private String deleteCategory(String id, AccountEntity accessor) {
        if (!allows(accessor, CapabilityIds.EDITOR_CATEGORIES)) {
            return "forbidden";
        }
        ObjectId objectId = parseObjectId(id);
        if (objectId == null) {
            return "invalid_item";
        }
        CategoryEntity category = (CategoryEntity) serviceProvider.getCategoryService()
                .getById(CategoryEntity.class, StoredIds.entityId(objectId));
        if (category == null) {
            return "not_found";
        }
        List<ItemEntity> pages = serviceProvider.getItemService().getAllByCategory(category);
        if (pages != null) {
            for (ItemEntity item : pages) {
                if (item != null) {
                    serviceProvider.getItemService().delete(item);
                }
            }
        }
        serviceProvider.getCategoryService().delete(category);
        resourceGroupIndex.contentChanged();
        return null;
    }

    private String deleteApp(String slug, AccountEntity accessor) {
        if (!allows(accessor, CapabilityIds.EDITOR_APPS)) {
            return "forbidden";
        }
        String normalized = slug.trim().toLowerCase(Locale.ROOT);
        if (ShippedPackCatalog.isShippedApp(normalized) || StaticAppDeployer.isReservedSlug(normalized)) {
            return "bundled";
        }
        try {
            staticAppDeployer.validateSlug(normalized);
        } catch (IllegalArgumentException ex) {
            return "invalid_item";
        }
        AppEntity existing = serviceProvider.getAppService().getBySlug(normalized);
        if (existing != null) {
            serviceProvider.getAppService().delete(existing);
            resourceGroupIndex.contentChanged();
        }
        try {
            staticAppDeployer.undeploy(normalized);
        } catch (Exception ex) {
            LOGGER.warn("Failed to remove disk tree for '{}'", normalized, ex);
        }
        return existing == null ? "not_found" : null;
    }

    private String deleteTheme(String themeId, AccountEntity accessor) {
        if (!allows(accessor, CapabilityIds.EDITOR_THEMES)) {
            return "forbidden";
        }
        String normalized = themeId.trim().toLowerCase(Locale.ROOT);
        if (ThemePackDeployer.isReservedId(normalized) || ShippedPackCatalog.isShippedTheme(normalized)) {
            return "bundled";
        }
        try {
            themePackDeployer.validateId(normalized);
        } catch (IllegalArgumentException ex) {
            return "invalid_item";
        }
        ThemeSelection active = themeCatalog.resolve();
        if (normalized.equalsIgnoreCase(active.getCssId())) {
            return "theme_active";
        }
        ThemeEntity existing = serviceProvider.getThemeService().getByThemeId(normalized);
        if (existing != null) {
            serviceProvider.getThemeService().delete(existing);
            resourceGroupIndex.contentChanged();
        }
        try {
            themePackDeployer.undeploy(normalized);
        } catch (Exception ex) {
            LOGGER.warn("Failed to remove disk tree for theme '{}'", normalized, ex);
        }
        return existing == null ? "not_found" : null;
    }

    private String deleteGroup(String groupId, AccountEntity accessor) {
        if (!allows(accessor, CapabilityIds.EDITOR_FILES)) {
            return "forbidden";
        }
        ObjectId groupObjId = parseObjectId(groupId);
        if (groupObjId == null) {
            return "invalid_group";
        }
        List<ResourceGroupEntity> all = serviceProvider.getResourceGroupService().getAllGroups();
        if (all == null) {
            all = List.of();
        }
        ResourceGroupEntity group = ResourceGroupTree.find(all, groupObjId);
        String refused = ResourceGroupTree.refuseDelete(all, group);
        if (refused != null) {
            return refused;
        }
        for (ResourceEntity resource : serviceProvider.getResourceService().listForEditor(groupObjId)) {
            if (resource.getResourceData() == null || resource.getResourceData().getPathPublic() == null) {
                continue;
            }
            String pathPublic = resource.getResourceData().getPathPublic();
            String previewPath = resource.getPreviewData() == null
                    ? null : resource.getPreviewData().getPathPublic();
            serviceProvider.getResourceService().deleteByPublicPath(pathPublic);
            contentCacheController.invalidateCacheForResource(pathPublic);
            if (previewPath != null) {
                contentCacheController.invalidateCacheForResource(previewPath);
            }
        }
        serviceProvider.getResourceGroupService().delete(group);
        resourceGroupIndex.structureChanged();
        return null;
    }

    private boolean allows(AccountEntity accessor, String capabilityId) {
        return capabilityService == null || capabilityService.allows(accessor, capabilityId);
    }

    private static String keepFirst(String current, String next) {
        if (next == null || "not_found".equals(next)) {
            return current;
        }
        return current == null ? next : current;
    }

    private static int rank(String kind) {
        return switch (kind) {
            case "resource" -> 0;
            case "page", "album" -> 1;
            case "playlist" -> 2;
            case "url-template" -> 2;
            case "app" -> 3;
            case "theme" -> 4;
            case "group" -> 5;
            case "category" -> 6;
            default -> 9;
        };
    }

    private static ObjectId parseObjectId(String raw) {
        try {
            return new ObjectId(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private record Target(String kind, String id) {
    }
}

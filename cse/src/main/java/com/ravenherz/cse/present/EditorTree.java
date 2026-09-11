package com.ravenherz.cse.present;

import com.ravenherz.cse.dal.dto.CategoryEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.PlaylistEntity;
import com.ravenherz.cse.dal.dto.basic.CategoryData;
import com.ravenherz.cse.dal.dto.basic.ItemData;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Content branch for the Resources tree. Cached on {@link ResourceGroupIndex}.
 */
public final class EditorTree {

    public static final String CONTENT_ID = "content";
    public static final String APPS_ID = "content-apps";
    public static final String CATEGORIES_ID = "content-categories";
    public static final String PLAYLISTS_ID = "content-playlists";
    public static final String THEMES_ID = "content-themes";
    public static final String CATEGORY_PREFIX = "category-";
    public static final String PAGE_PREFIX = "page-";
    public static final String PLAYLIST_PREFIX = "playlist-";

    public static final String CONTENT_HREF = "/editor/resources?group=content";
    public static final String APPS_HREF = "/editor/resources?group=" + APPS_ID;
    public static final String CATEGORIES_HREF = "/editor/resources?group=" + CATEGORIES_ID;
    public static final String PLAYLISTS_HREF = "/editor/resources?group=" + PLAYLISTS_ID;
    public static final String THEMES_HREF = "/editor/resources?group=" + THEMES_ID;

    private EditorTree() {
    }

    public static boolean isChromeId(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        String trimmed = id.trim();
        return CONTENT_ID.equals(trimmed)
                || APPS_ID.equals(trimmed)
                || CATEGORIES_ID.equals(trimmed)
                || PLAYLISTS_ID.equals(trimmed)
                || THEMES_ID.equals(trimmed)
                || trimmed.startsWith(CATEGORY_PREFIX)
                || trimmed.startsWith(PAGE_PREFIX);
    }

    public static boolean browseInResources(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        String trimmed = id.trim();
        return CONTENT_ID.equals(trimmed)
                || APPS_ID.equals(trimmed)
                || CATEGORIES_ID.equals(trimmed)
                || PLAYLISTS_ID.equals(trimmed)
                || THEMES_ID.equals(trimmed)
                || trimmed.startsWith(CATEGORY_PREFIX);
    }

    public static String packUploadReturnHref(String returnGroup) {
        if (APPS_ID.equals(returnGroup) || THEMES_ID.equals(returnGroup)) {
            return hrefOf(returnGroup);
        }
        return null;
    }

    public static String catalogReturnHref(String returnGroup, String fallbackId) {
        if (browseInResources(returnGroup)) {
            return hrefOf(returnGroup.trim());
        }
        return hrefOf(fallbackId);
    }

    public static String categoryDeleteReturnHref(String returnGroup) {
        if (returnGroup != null && returnGroup.trim().startsWith(CATEGORY_PREFIX)) {
            return CATEGORIES_HREF;
        }
        return catalogReturnHref(returnGroup, CATEGORIES_ID);
    }

    public static String pageDeleteReturnHref(String returnGroup, String categoryId) {
        if (browseInResources(returnGroup)) {
            return hrefOf(returnGroup.trim());
        }
        String node = categoryNodeId(categoryId);
        return node != null ? hrefOf(node) : CATEGORIES_HREF;
    }

    public static String withNotice(String href, String notice) {
        String dest = href == null || href.isBlank() ? "/editor/resources" : href;
        if (notice == null || notice.isBlank()) {
            return dest;
        }
        String sep = dest.contains("?") ? "&" : "?";
        return dest + sep + "notice=" + URLEncoder.encode(notice, StandardCharsets.UTF_8);
    }

    public static String pageCreatePath(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return "/editor/create";
        }
        return "/editor/create?categoryId=" + categoryId.trim();
    }

    public static String albumCreatePath(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return "/editor/album/create";
        }
        return "/editor/album/create?categoryId=" + categoryId.trim();
    }

    public static String hrefOf(String id) {
        if (id == null || id.isBlank()) {
            return "/editor/resources";
        }
        String trimmed = id.trim();
        return switch (trimmed) {
            case CONTENT_ID -> CONTENT_HREF;
            case APPS_ID -> APPS_HREF;
            case CATEGORIES_ID -> CATEGORIES_HREF;
            case PLAYLISTS_ID -> PLAYLISTS_HREF;
            case THEMES_ID -> THEMES_HREF;
            default -> {
                if (trimmed.startsWith(CATEGORY_PREFIX)) {
                    yield "/editor/resources?group=" + trimmed;
                }
                yield "/editor/resources";
            }
        };
    }

    public static String categoryNodeId(String categoryId) {
        return categoryId == null || categoryId.isBlank() ? null : CATEGORY_PREFIX + categoryId.trim();
    }

    public static List<ResourceGroupDisplayDTO> attach(ResourceGroupTreeView.Assembled resources,
            List<CategoryEntity> categories, List<ItemEntity> items) {
        return attach(resources, contentBranch(categories, items));
    }

    public static List<ResourceGroupDisplayDTO> attach(ResourceGroupTreeView.Assembled resources,
            ResourceGroupDisplayDTO content) {
        ResourceGroupTreeView.Assembled copy = ResourceGroupTreeView.copy(resources);
        applyResourceHrefs(copy.roots());
        List<ResourceGroupDisplayDTO> roots = new ArrayList<>();
        roots.add(content != null ? content : contentBranch(List.of(), List.of()));
        if (copy.roots() != null) {
            roots.addAll(copy.roots());
        }
        return roots;
    }

    public static ResourceGroupDisplayDTO contentBranch(List<CategoryEntity> categories,
            List<ItemEntity> items) {
        return contentBranch(categories, items, List.of(), List.of(), List.of());
    }

    public static ResourceGroupDisplayDTO contentBranch(List<CategoryEntity> categories,
            List<ItemEntity> items, List<ResourceTreeFile> apps, List<ResourceTreeFile> playlists,
            List<ResourceTreeFile> themes) {
        return buildContent(categories, items, apps, playlists, themes);
    }

    public static ResourceGroupDisplayDTO find(List<ResourceGroupDisplayDTO> roots, String id) {
        if (roots == null || id == null || id.isBlank()) {
            return null;
        }
        String needle = id.trim();
        for (ResourceGroupDisplayDTO root : roots) {
            ResourceGroupDisplayDTO found = findIn(root, needle);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    public static String selectionId(String uri, String group, String category, String editName,
            String categoryEditId, String itemCategoryId) {
        if (uri == null) {
            return null;
        }
        if (uri.contains("/editor/content")) {
            return CONTENT_ID;
        }
        if (uri.contains("/editor/apps")) {
            return APPS_ID;
        }
        if (uri.contains("/editor/themes")) {
            return THEMES_ID;
        }
        if (uri.contains("/editor/playlist")) {
            return PLAYLISTS_ID;
        }
        if (uri.contains("/editor/category/edit")) {
            return categoryNodeId(categoryEditId) != null
                    ? categoryNodeId(categoryEditId) : CATEGORIES_ID;
        }
        if (uri.contains("/editor/categor")) {
            return CATEGORIES_ID;
        }
        if (uri.contains("/editor/pages")) {
            return categoryNodeId(category) != null ? categoryNodeId(category) : CATEGORIES_ID;
        }
        if (uri.contains("/editor/edit")) {
            return categoryNodeId(itemCategoryId) != null
                    ? categoryNodeId(itemCategoryId) : CATEGORIES_ID;
        }
        if (uri.contains("/editor/create")) {
            return categoryNodeId(category) != null ? categoryNodeId(category) : CATEGORIES_ID;
        }
        if (uri.contains("/editor/album")) {
            return categoryNodeId(category) != null ? categoryNodeId(category) : CATEGORIES_ID;
        }
        if (uri.contains("/editor/resources")) {
            if (isChromeId(group)) {
                return group.trim();
            }
            if (group != null && !group.isBlank() && !ResourceGroupTreeView.isHomeAlias(group)) {
                return group.trim();
            }
            return null;
        }
        return null;
    }

    private static ResourceGroupDisplayDTO buildContent(List<CategoryEntity> categories,
            List<ItemEntity> items, List<ResourceTreeFile> appFiles,
            List<ResourceTreeFile> playlistFiles, List<ResourceTreeFile> themeFiles) {
        ResourceGroupDisplayDTO content = virtual(CONTENT_ID, "Content", CONTENT_HREF, null, 1);
        ResourceGroupDisplayDTO apps = virtual(APPS_ID, "Apps", APPS_HREF, CONTENT_ID, 2);
        ResourceGroupDisplayDTO cats = virtual(CATEGORIES_ID, "Categories", CATEGORIES_HREF, CONTENT_ID, 2);
        ResourceGroupDisplayDTO playlists = virtual(PLAYLISTS_ID, "Playlists", PLAYLISTS_HREF, CONTENT_ID, 2);
        ResourceGroupDisplayDTO themes = virtual(THEMES_ID, "Themes", THEMES_HREF, CONTENT_ID, 2);
        apps.setTreeFiles(sortedLeaves(appFiles));
        playlists.setTreeFiles(sortedLeaves(playlistFiles));
        themes.setTreeFiles(sortedLeaves(themeFiles));
        attachCategories(cats, categories, items);
        content.getChildren().add(apps);
        content.getChildren().add(cats);
        content.getChildren().add(playlists);
        content.getChildren().add(themes);
        content.setSubtreeHeight(content.getChildren().stream().anyMatch(ResourceGroupDisplayDTO::hasExpandableChildren)
                ? 2 : 1);
        content.setPathLabel("Content");
        return content;
    }

    private static void attachCategories(ResourceGroupDisplayDTO cats,
            List<CategoryEntity> categories, List<ItemEntity> items) {
        Map<String, List<ItemEntity>> byCategory = new HashMap<>();
        List<ItemEntity> loose = new ArrayList<>();
        if (items != null) {
            for (ItemEntity item : items) {
                if (item == null || item.getUniqueUriName() == null || item.getUniqueUriName().isBlank()) {
                    continue;
                }
                if (item.getRefCategoryId() == null) {
                    loose.add(item);
                    continue;
                }
                byCategory.computeIfAbsent(item.getRefCategoryId().toString(), id -> new ArrayList<>())
                        .add(item);
            }
        }
        List<CategoryEntity> ordered = new ArrayList<>();
        if (categories != null) {
            for (CategoryEntity category : categories) {
                if (category != null && category.getId() != null) {
                    ordered.add(category);
                }
            }
        }
        ordered.sort(Comparator
                .comparingInt(EditorTree::categoryPriority)
                .thenComparing(EditorTree::categoryName, String.CASE_INSENSITIVE_ORDER));
        for (CategoryEntity category : ordered) {
            String id = category.getId().toString();
            String name = categoryName(category);
            ResourceGroupDisplayDTO node = new ResourceGroupDisplayDTO();
            node.setId(CATEGORY_PREFIX + id);
            node.setParentId(CATEGORIES_ID);
            node.setHumanReadableId(name);
            CategoryData data = category.getCategoryData();
            if (data != null && data.getItemName() != null && !data.getItemName().isBlank()) {
                node.setCategoryItemName(data.getItemName());
            }
            node.setPathLabel("Content / Categories / " + name);
            node.setDepth(3);
            node.setHref("/editor/resources?group=" + CATEGORY_PREFIX + id);
            node.setTreeFiles(pageLeaves(byCategory.get(id)));
            node.setSubtreeHeight(0);
            cats.getChildren().add(node);
        }
        cats.setTreeFiles(pageLeaves(loose));
        cats.setSubtreeHeight(cats.getChildren().isEmpty() ? 0 : 1);
        cats.setPathLabel("Content / Categories");
    }

    private static List<ResourceTreeFile> sortedLeaves(List<ResourceTreeFile> files) {
        List<ResourceTreeFile> out = new ArrayList<>();
        if (files == null) {
            return out;
        }
        for (ResourceTreeFile file : files) {
            if (file != null && file.name() != null && !file.name().isBlank()) {
                out.add(file);
            }
        }
        out.sort(Comparator.comparing(ResourceTreeFile::name, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    private static List<ResourceTreeFile> pageLeaves(List<ItemEntity> items) {
        List<ResourceTreeFile> files = new ArrayList<>();
        if (items == null) {
            return files;
        }
        List<ItemEntity> ordered = new ArrayList<>(items);
        ordered.sort(Comparator.comparing(EditorTree::itemTitle, String.CASE_INSENSITIVE_ORDER));
        for (ItemEntity item : ordered) {
            String name = item.getUniqueUriName().trim();
            String href = "/editor/edit?name=" + URLEncoder.encode(name, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            files.add(new ResourceTreeFile(pageLeafId(item), itemTitle(item), href,
                    item.isAlbum() ? ResourceTreeFile.Mark.ALBUM : ResourceTreeFile.Mark.PAGE)
                    .withKey(name));
        }
        return files;
    }

    public static String pageLeafId(ItemEntity item) {
        if (item == null) {
            return null;
        }
        if (item.getId() != null) {
            return PAGE_PREFIX + item.getId();
        }
        String name = item.getUniqueUriName();
        return name == null || name.isBlank() ? null : PAGE_PREFIX + name.trim();
    }

    public static String playlistLeafId(PlaylistEntity playlist) {
        if (playlist == null || playlist.getId() == null) {
            return null;
        }
        return PLAYLIST_PREFIX + playlist.getId();
    }

    private static ResourceGroupDisplayDTO virtual(String id, String name, String href,
            String parentId, int depth) {
        ResourceGroupDisplayDTO dto = new ResourceGroupDisplayDTO();
        dto.setId(id);
        dto.setParentId(parentId);
        dto.setHumanReadableId(name);
        dto.setPathLabel(parentId == null ? name : "Content / " + name);
        dto.setDepth(depth);
        dto.setVirtual(true);
        dto.setHref(href);
        dto.setSubtreeHeight(0);
        return dto;
    }

    private static void applyResourceHrefs(List<ResourceGroupDisplayDTO> nodes) {
        if (nodes == null) {
            return;
        }
        for (ResourceGroupDisplayDTO node : nodes) {
            if (node.getHref() == null && node.getId() != null && !node.isUngrouped()) {
                node.setHref("/editor/resources?group=" + node.getId());
            }
            applyResourceHrefs(node.getChildren());
        }
    }

    private static ResourceGroupDisplayDTO findIn(ResourceGroupDisplayDTO node, String id) {
        if (node == null) {
            return null;
        }
        if (id.equals(node.getId())) {
            return node;
        }
        if (node.getChildren() == null) {
            return null;
        }
        for (ResourceGroupDisplayDTO child : node.getChildren()) {
            ResourceGroupDisplayDTO found = findIn(child, id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static int categoryPriority(CategoryEntity category) {
        CategoryData data = category.getCategoryData();
        return data == null ? 0 : data.getDisplayPriority();
    }

    private static String categoryName(CategoryEntity category) {
        CategoryData data = category == null ? null : category.getCategoryData();
        if (data != null) {
            if (data.getNavigationTitle() != null && !data.getNavigationTitle().isBlank()) {
                return data.getNavigationTitle().trim();
            }
            if (data.getItemName() != null && !data.getItemName().isBlank()) {
                return data.getItemName().trim();
            }
        }
        return category == null || category.getId() == null ? "Category" : category.getId().toString();
    }

    static String itemTitle(ItemEntity item) {
        ItemData data = item.isAlbum() ? item.getAlbumData() : item.getPageData();
        if (data != null && data.getTitle() != null && !data.getTitle().isBlank()) {
            return data.getTitle().trim();
        }
        return item.getUniqueUriName() == null ? "" : item.getUniqueUriName().trim();
    }
}

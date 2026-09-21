package com.ravenherz.cse.present;

import com.ravenherz.cse.dal.dao.AppService;
import com.ravenherz.cse.transfer.ResourceGroupRebuild;
import com.ravenherz.cse.dal.dao.CategoryService;
import com.ravenherz.cse.dal.dao.ItemService;
import com.ravenherz.cse.dal.dao.PlaylistService;
import com.ravenherz.cse.dal.dao.ResourceGroupService;
import com.ravenherz.cse.dal.dao.ResourceService;
import com.ravenherz.cse.dal.dao.ThemeService;
import com.ravenherz.cse.dal.dao.UrlTemplateService;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.CategoryEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.PlaylistEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.ResourceGroupEntity;
import com.ravenherz.cse.dal.dto.UrlTemplateEntity;
import com.ravenherz.cse.dal.dto.basic.AlbumData;
import com.ravenherz.cse.dal.dto.basic.PageData;
import com.ravenherz.cse.dal.dto.basic.PlaylistData;
import com.ravenherz.cse.dal.dto.basic.PlaylistTrack;
import com.ravenherz.cse.dal.dto.basic.ResourceSizeHint;
import com.ravenherz.cse.dal.dto.basic.UrlTemplateData;
import com.ravenherz.cse.util.imaging.UrlTemplateImages;
import com.ravenherz.cse.util.io.CseDisk;
import com.ravenherz.cse.util.staticapps.StaticAppDeployer;
import com.ravenherz.cse.util.themes.ThemeCatalog;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * In-memory group tree + own/total file stats. Not persisted. One JVM.
 */
@Component
public class ResourceGroupIndex implements ResourceGroupRebuild {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceGroupIndex.class);
    private static final String THEME_CLASSPATH_PREVIEW = "/static/content-public/themes/%s/preview.jpg";

    private final ResourceGroupService groupService;
    private final ResourceService resourceService;
    private final CategoryService categoryService;
    private final ItemService itemService;
    private final AppService appService;
    private final PlaylistService playlistService;
    private final UrlTemplateService urlTemplateService;
    private final ThemeCatalog themeCatalog;
    private final StaticAppDeployer staticAppDeployer;
    private final ThemeService themeService;
    private final Object lock = new Object();
    private final AtomicReference<ResourceGroupTreeView.Assembled> snapshot = new AtomicReference<>();
    private final AtomicReference<ContentCache> content = new AtomicReference<>();
    private final AtomicReference<Map<String, byte[]>> previews = new AtomicReference<>(Map.of());

    public ResourceGroupIndex(ResourceGroupService groupService, ResourceService resourceService) {
        this(groupService, resourceService, null, null, null, null, null, null, null, null);
    }

    public ResourceGroupIndex(ResourceGroupService groupService, ResourceService resourceService,
            CategoryService categoryService, ItemService itemService) {
        this(groupService, resourceService, categoryService, itemService, null, null, null, null, null, null);
    }

    public ResourceGroupIndex(ResourceGroupService groupService, ResourceService resourceService,
            CategoryService categoryService, ItemService itemService, AppService appService,
            PlaylistService playlistService, ThemeCatalog themeCatalog) {
        this(groupService, resourceService, categoryService, itemService, appService, playlistService,
                themeCatalog, null, null, null);
    }

    public ResourceGroupIndex(ResourceGroupService groupService, ResourceService resourceService,
            CategoryService categoryService, ItemService itemService, AppService appService,
            PlaylistService playlistService, ThemeCatalog themeCatalog,
            StaticAppDeployer staticAppDeployer, ThemeService themeService) {
        this(groupService, resourceService, categoryService, itemService, appService, playlistService,
                themeCatalog, staticAppDeployer, themeService, null);
    }

    @Autowired
    public ResourceGroupIndex(ResourceGroupService groupService, ResourceService resourceService,
            CategoryService categoryService, ItemService itemService, AppService appService,
            PlaylistService playlistService, ThemeCatalog themeCatalog,
            StaticAppDeployer staticAppDeployer, ThemeService themeService,
            UrlTemplateService urlTemplateService) {
        this.groupService = groupService;
        this.resourceService = resourceService;
        this.categoryService = categoryService;
        this.itemService = itemService;
        this.appService = appService;
        this.playlistService = playlistService;
        this.themeCatalog = themeCatalog;
        this.staticAppDeployer = staticAppDeployer;
        this.themeService = themeService;
        this.urlTemplateService = urlTemplateService;
    }

    public ResourceGroupTreeView.Assembled view() {
        ResourceGroupTreeView.Assembled local = snapshot.get();
        if (local != null) {
            return local;
        }
        synchronized (lock) {
            local = snapshot.get();
            if (local == null) {
                rebuildLocked();
                local = snapshot.get();
            }
            return local;
        }
    }

    public byte[] treePreview(String id) {
        view();
        if (id == null || id.isBlank()) {
            return null;
        }
        Map<String, byte[]> map = previews.get();
        return map == null ? null : map.get(id.trim());
    }

    public void rebuild() {
        synchronized (lock) {
            rebuildLocked();
        }
    }

    public void fileAdded(String groupId, long bytes) {
        deltaIfWarm(groupId, 1, bytes, null, null);
    }

    public void fileAdded(ResourceEntity resource) {
        if (resource == null) {
            return;
        }
        byte[] thumb = TreePreviews.jpeg(resource);
        ResourceTreeFile file = ResourceTreeFile.from(resource);
        if (file != null && thumb != null) {
            file = file.withPreview(true);
        }
        synchronized (lock) {
            deltaIfWarmUnlocked(ResourceGroupTreeView.statsKey(resource),
                    1, ResourceGroupTreeView.byteSize(resource), file, null);
            if (file != null && thumb != null) {
                putPreviewUnlocked(file.id(), thumb);
                ResourceGroupTreeView.applyPreviews(snapshot.get(), orEmpty(previews.get()).keySet());
            }
            refreshItemPreviewsUnlocked();
        }
    }

    public void fileRemoved(String groupId, long bytes) {
        deltaIfWarm(groupId, -1, -bytes, null, null);
    }

    public void fileRemoved(ResourceEntity resource) {
        if (resource == null) {
            return;
        }
        ResourceTreeFile file = ResourceTreeFile.from(resource);
        synchronized (lock) {
            if (!deltaIfWarmUnlocked(ResourceGroupTreeView.statsKey(resource),
                    -1, -ResourceGroupTreeView.byteSize(resource),
                    null, file == null ? null : file.id())) {
                return;
            }
            if (file != null) {
                putPreviewUnlocked(file.id(), null);
            }
            refreshItemPreviewsUnlocked();
        }
    }

    public void fileMoved(String fromGroupId, String toGroupId, long bytes) {
        fileMoved(fromGroupId, toGroupId, bytes, null);
    }

    public void fileRenamed(ResourceEntity resource) {
        if (resource == null) {
            return;
        }
        byte[] thumb = treePreview(resource.getId() == null ? null : resource.getId().toString());
        if (thumb == null) {
            thumb = TreePreviews.jpeg(resource);
        }
        ResourceTreeFile file = ResourceTreeFile.from(resource);
        if (file != null && thumb != null) {
            file = file.withPreview(true);
        }
        synchronized (lock) {
            if (snapshot.get() == null
                    || !ResourceGroupTreeView.containsGroup(snapshot.get(),
                            ResourceGroupTreeView.statsKey(resource))) {
                rebuildLocked();
                return;
            }
            ResourceGroupTreeView.Assembled next = ResourceGroupTreeView.copy(snapshot.get());
            if (file != null) {
                ResourceGroupTreeView.putOwnFile(next, ResourceGroupTreeView.statsKey(resource), file);
            }
            snapshot.set(next);
            if (file != null && thumb != null) {
                putPreviewUnlocked(file.id(), thumb);
            }
            refreshItemPreviewsUnlocked();
        }
    }

    public void fileMoved(String fromGroupId, String toGroupId, long bytes, ResourceEntity resource) {
        String from = normalize(fromGroupId);
        String to = normalize(toGroupId);
        if (from.equals(to)) {
            return;
        }
        synchronized (lock) {
            if (snapshot.get() == null
                    || !ResourceGroupTreeView.containsGroup(snapshot.get(), from)
                    || !ResourceGroupTreeView.containsGroup(snapshot.get(), to)) {
                rebuildLocked();
                return;
            }
            ResourceGroupTreeView.Assembled next = ResourceGroupTreeView.copy(snapshot.get());
            ResourceGroupTreeView.applyFileDelta(next, from, -1, -bytes);
            ResourceGroupTreeView.applyFileDelta(next, to, 1, bytes);
            ResourceTreeFile file = ResourceTreeFile.from(resource);
            if (file != null) {
                byte[] thumb = previewOf(file.id());
                if (thumb == null) {
                    thumb = TreePreviews.jpeg(resource);
                    putPreviewUnlocked(file.id(), thumb);
                }
                if (thumb != null) {
                    file = file.withPreview(true);
                }
                ResourceGroupTreeView.dropOwnFile(next, from, file.id());
                ResourceGroupTreeView.putOwnFile(next, to, file);
            }
            snapshot.set(next);
            refreshItemPreviewsUnlocked();
        }
    }

    public void structureChanged() {
        rebuild();
    }

    public void contentChanged() {
        synchronized (lock) {
            rebuildContentLocked();
        }
    }

    public List<ResourceGroupDisplayDTO> editorRoots() {
        return EditorTree.attach(view(), contentView());
    }

    public ResourceGroupDisplayDTO contentView() {
        return contentCache().tree();
    }

    public List<AppDisplayDTO> apps() {
        return contentCache().apps();
    }

    public List<ThemeDisplayDTO> themes() {
        return contentCache().themes();
    }

    public List<PlaylistEntity> playlists() {
        return contentCache().playlists();
    }

    public List<UrlTemplateEntity> urlTemplates() {
        return contentCache().urlTemplates();
    }

    public List<CategoryEntity> categories() {
        return contentCache().categories();
    }

    public List<ItemEntity> itemsInCategory(String categoryId) {
        String needle = categoryId == null ? "" : categoryId.trim();
        List<ItemEntity> out = new ArrayList<>();
        for (ItemEntity item : contentCache().items()) {
            if (item == null || item.getUniqueUriName() == null || item.getUniqueUriName().isBlank()) {
                continue;
            }
            String ref = item.getRefCategoryId() == null ? "" : item.getRefCategoryId().toString();
            if (needle.equals(ref)) {
                out.add(item);
            }
        }
        out.sort(Comparator.comparing(EditorTree::itemTitle, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    public List<ResourceEntity> filesFor(ResourceGroupDisplayDTO group) {
        if (group == null || group.isUngrouped()) {
            return emptyOr(resourceService.listForEditor(null));
        }
        if (group.getId() == null || group.getId().isBlank()) {
            return group.isDefaultGroup() ? emptyOr(resourceService.listForEditor(null)) : List.of();
        }
        ObjectId id;
        try {
            id = new ObjectId(group.getId());
        } catch (IllegalArgumentException e) {
            return List.of();
        }
        List<ResourceEntity> files = emptyOr(resourceService.listForEditor(id));
        if (group.isDefaultGroup()) {
            List<ResourceEntity> orphans = emptyOr(resourceService.listForEditor(null));
            if (!orphans.isEmpty()) {
                List<ResourceEntity> merged = new ArrayList<>(files);
                merged.addAll(orphans);
                return merged;
            }
        }
        return files;
    }

    private static List<ResourceEntity> emptyOr(List<ResourceEntity> files) {
        return files == null ? List.of() : files;
    }

    private void deltaIfWarm(String groupId, int dCount, long dBytes, ResourceTreeFile addFile,
            String removeId) {
        synchronized (lock) {
            deltaIfWarmUnlocked(groupId, dCount, dBytes, addFile, removeId);
        }
    }

    private boolean deltaIfWarmUnlocked(String groupId, int dCount, long dBytes, ResourceTreeFile addFile,
            String removeId) {
        if (snapshot.get() == null) {
            rebuildLocked();
            return false;
        }
        String key = normalize(groupId);
        if (!ResourceGroupTreeView.containsGroup(snapshot.get(), key)) {
            rebuildLocked();
            return false;
        }
        ResourceGroupTreeView.Assembled next = ResourceGroupTreeView.copy(snapshot.get());
        ResourceGroupTreeView.applyFileDelta(next, key, dCount, dBytes);
        if (removeId != null) {
            ResourceGroupTreeView.dropOwnFile(next, key, removeId);
        }
        if (addFile != null) {
            ResourceGroupTreeView.putOwnFile(next, key, addFile);
        }
        snapshot.set(next);
        return true;
    }

    private void rebuildLocked() {
        List<ResourceGroupEntity> groups = groupService.getAllGroups();
        List<ResourceSizeHint> hints = resourceService.listSizeHints();
        Map<String, byte[]> thumbs = resourceThumbs();
        ResourceGroupTreeView.Assembled tree = ResourceGroupTreeView.assembleStats(
                groups == null ? List.of() : groups, hints);
        ResourceGroupTreeView.applyPreviews(tree, thumbs.keySet());
        snapshot.set(tree);
        previews.set(thumbs);
        rebuildContentLocked();
        LOGGER.debug("Rebuilt resource group index ({} groups, {} previews)",
                tree.assignableGroups().size(), previews.get().size());
    }

    private ContentCache contentCache() {
        ContentCache local = content.get();
        if (local != null) {
            return local;
        }
        synchronized (lock) {
            local = content.get();
            if (local == null) {
                rebuildContentLocked();
                local = content.get();
            }
            return local;
        }
    }

    private void rebuildContentLocked() {
        if (categoryService == null || itemService == null) {
            content.set(ContentCache.empty());
            return;
        }
        List<AppDisplayDTO> apps = EditorContentCatalog.apps(appService, staticAppDeployer);
        List<ThemeDisplayDTO> themes = EditorContentCatalog.themes(themeCatalog, themeService);
        List<PlaylistEntity> playlists = loadPlaylists();
        List<UrlTemplateEntity> urlTemplates = loadUrlTemplates();
        List<CategoryEntity> categories = loadCategories();
        List<ItemEntity> items = loadItems();
        content.set(new ContentCache(
                EditorTree.contentBranch(categories, items, appLeaves(apps), playlistLeaves(playlists),
                        themeLeaves(themes), urlTemplateLeaves(urlTemplates)),
                apps, themes, playlists, urlTemplates, categories, items));
        Map<String, byte[]> next = new HashMap<>(orEmpty(previews.get()));
        next.keySet().removeIf(id -> id == null || !ObjectId.isValid(id));
        putItemDerived(next);
        putPackDerived(next);
        previews.set(Map.copyOf(next));
        ResourceGroupTreeView.applyPreviews(content.get().tree(), next.keySet());
        LOGGER.debug("Rebuilt content branch");
    }

    private List<CategoryEntity> loadCategories() {
        List<CategoryEntity> all = categoryService.getAllCategories();
        return all == null ? List.of() : all;
    }

    private List<PlaylistEntity> loadPlaylists() {
        if (playlistService == null) {
            return List.of();
        }
        List<PlaylistEntity> all = playlistService.getAllPlaylists();
        return all == null ? List.of() : all;
    }

    private List<UrlTemplateEntity> loadUrlTemplates() {
        if (urlTemplateService == null) {
            return List.of();
        }
        List<UrlTemplateEntity> all = urlTemplateService.getAllUrlTemplates();
        return all == null ? List.of() : all;
    }

    private static List<ResourceTreeFile> appLeaves(List<AppDisplayDTO> apps) {
        List<ResourceTreeFile> out = new ArrayList<>();
        if (apps == null) {
            return out;
        }
        for (AppDisplayDTO app : apps) {
            if (app == null || app.getSlug() == null || app.getSlug().isBlank()) {
                continue;
            }
            String slug = app.getSlug().trim();
            String name = app.getName() == null || app.getName().isBlank() ? slug : app.getName().trim();
            String href = app.getPublicUrl() == null || app.getPublicUrl().isBlank()
                    ? EditorContentCatalog.publicUrlFor(slug) : app.getPublicUrl();
            out.add(new ResourceTreeFile("app-" + slug.toLowerCase(Locale.ROOT), name, href,
                    ResourceTreeFile.Mark.APP)
                    .withKey(slug.toLowerCase(Locale.ROOT))
                    .withDelete(!app.isBundled()));
        }
        return out;
    }

    private static List<ResourceTreeFile> playlistLeaves(List<PlaylistEntity> playlists) {
        List<ResourceTreeFile> out = new ArrayList<>();
        if (playlists == null) {
            return out;
        }
        for (PlaylistEntity playlist : playlists) {
            if (playlist == null || playlist.getId() == null) {
                continue;
            }
            PlaylistData data = playlist.getPlaylistData();
            String name = data != null && data.getTitle() != null && !data.getTitle().isBlank()
                    ? data.getTitle().trim()
                    : (playlist.getPlaylistId() == null ? playlist.getId().toString()
                            : playlist.getPlaylistId());
            out.add(new ResourceTreeFile(EditorTree.playlistLeafId(playlist), name,
                    "/editor/playlist/edit?id=" + playlist.getId(), ResourceTreeFile.Mark.PLAYLIST)
                    .withKey(playlist.getId().toString())
                    .withEmbedId(playlist.getPlaylistId()));
        }
        return out;
    }

    private static List<ResourceTreeFile> urlTemplateLeaves(List<UrlTemplateEntity> templates) {
        List<ResourceTreeFile> out = new ArrayList<>();
        if (templates == null) {
            return out;
        }
        for (UrlTemplateEntity template : templates) {
            if (template == null || template.getId() == null) {
                continue;
            }
            String name = template.getUrlTemplateId() == null || template.getUrlTemplateId().isBlank()
                    ? template.getId().toString() : template.getUrlTemplateId();
            out.add(new ResourceTreeFile(EditorTree.urlTemplateLeafId(template), name,
                    "/editor/url-template/edit?id=" + template.getId(), ResourceTreeFile.Mark.URL_TEMPLATE)
                    .withKey(template.getId().toString())
                    .withEmbedId(template.getUrlTemplateId()));
        }
        return out;
    }

    private static List<ResourceTreeFile> themeLeaves(List<ThemeDisplayDTO> themes) {
        List<ResourceTreeFile> out = new ArrayList<>();
        if (themes == null) {
            return out;
        }
        for (ThemeDisplayDTO theme : themes) {
            if (theme == null || theme.getThemeId() == null || theme.getThemeId().isBlank()) {
                continue;
            }
            String name = theme.getTitle() == null || theme.getTitle().isBlank()
                    ? theme.getThemeId() : theme.getTitle().trim();
            out.add(new ResourceTreeFile("theme-" + theme.getThemeId(), name, EditorTree.THEMES_HREF,
                    ResourceTreeFile.Mark.THEME)
                    .withKey(theme.getThemeId())
                    .withDelete(!theme.isBuiltin() && !theme.isActive())
                    .withActivate(!theme.isActive()));
        }
        return out;
    }

    private List<ItemEntity> loadItems() {
        List<ItemEntity> out = new ArrayList<>();
        List<BasicEntity> all = itemService.getAll();
        if (all == null) {
            return out;
        }
        for (BasicEntity entity : all) {
            if (entity instanceof ItemEntity item) {
                out.add(item);
            }
        }
        return out;
    }

    private Map<String, byte[]> resourceThumbs() {
        Map<String, byte[]> out = new HashMap<>();
        resourceService.forEachPreviewSource(source -> {
            if (source == null || source.id() == null) {
                return;
            }
            byte[] jpeg = TreePreviews.jpeg(source.bytes());
            if (jpeg != null) {
                out.put(source.id().toString(), jpeg);
            }
        });
        return out;
    }

    private void putPreviewUnlocked(String id, byte[] jpeg) {
        if (id == null || id.isBlank()) {
            return;
        }
        Map<String, byte[]> next = new HashMap<>(orEmpty(previews.get()));
        if (jpeg == null || jpeg.length == 0) {
            next.remove(id);
        } else {
            next.put(id, jpeg);
        }
        previews.set(Map.copyOf(next));
    }

    private void refreshItemPreviewsUnlocked() {
        ContentCache local = content.get();
        if (local == null || local.tree() == null) {
            return;
        }
        Map<String, byte[]> next = new HashMap<>(orEmpty(previews.get()));
        next.keySet().removeIf(id -> id != null && (id.startsWith("page-") || id.startsWith("playlist-")
                || id.startsWith("url-template-")));
        putItemDerived(next);
        previews.set(Map.copyOf(next));
        ResourceGroupTreeView.applyPreviews(local.tree(), next.keySet());
    }

    private void putItemDerived(Map<String, byte[]> thumbs) {
        ContentCache local = content.get();
        if (local == null) {
            return;
        }
        for (ItemEntity item : local.items()) {
            if (item == null) {
                continue;
            }
            String leafId = EditorTree.pageLeafId(item);
            if (leafId == null) {
                continue;
            }
            byte[] jpeg = item.isAlbum() ? albumThumb(item, thumbs) : pageThumb(item, thumbs);
            if (jpeg != null) {
                thumbs.put(leafId, jpeg);
            }
        }
        for (PlaylistEntity playlist : local.playlists()) {
            if (playlist == null || playlist.getId() == null) {
                continue;
            }
            byte[] jpeg = playlistThumb(playlist, thumbs);
            if (jpeg != null) {
                thumbs.put("playlist-" + playlist.getId(), jpeg);
            }
        }
        for (UrlTemplateEntity template : local.urlTemplates()) {
            if (template == null || template.getId() == null) {
                continue;
            }
            byte[] jpeg = urlTemplateThumb(template);
            if (jpeg != null) {
                thumbs.put("url-template-" + template.getId(), jpeg);
            }
        }
    }

    private void putPackDerived(Map<String, byte[]> thumbs) {
        ContentCache local = content.get();
        if (local == null) {
            return;
        }
        for (AppDisplayDTO app : local.apps()) {
            if (app == null || app.getSlug() == null || app.getSlug().isBlank()) {
                continue;
            }
            byte[] jpeg = appThumb(app);
            if (jpeg != null) {
                thumbs.put("app-" + app.getSlug().trim().toLowerCase(Locale.ROOT), jpeg);
            }
        }
        for (ThemeDisplayDTO theme : local.themes()) {
            if (theme == null || theme.getThemeId() == null || theme.getThemeId().isBlank()) {
                continue;
            }
            byte[] jpeg = themeThumb(theme);
            if (jpeg != null) {
                thumbs.put("theme-" + theme.getThemeId(), jpeg);
            }
        }
    }

    private static byte[] pageThumb(ItemEntity item, Map<String, byte[]> thumbs) {
        PageData page = item.getPageData();
        if (page == null || page.getRefImageId() == null) {
            return null;
        }
        return thumbs.get(page.getRefImageId().toString());
    }

    private byte[] albumThumb(ItemEntity item, Map<String, byte[]> thumbs) {
        AlbumData album = item.getAlbumData();
        if (album == null || album.getRefResourceGroupId() == null) {
            return null;
        }
        ResourceGroupDisplayDTO group = ResourceGroupTreeView.find(snapshot.get(),
                album.getRefResourceGroupId().toString());
        if (group == null || group.getTreeFiles() == null) {
            return null;
        }
        for (ResourceTreeFile file : group.getTreeFiles()) {
            if (file == null || file.id() == null) {
                continue;
            }
            byte[] jpeg = thumbs.get(file.id());
            if (jpeg != null) {
                return jpeg;
            }
        }
        return null;
    }

    private static byte[] playlistThumb(PlaylistEntity playlist, Map<String, byte[]> thumbs) {
        PlaylistData data = playlist.getPlaylistData();
        if (data == null) {
            return null;
        }
        if (data.getRefImageId() != null) {
            byte[] cover = thumbs.get(data.getRefImageId().toString());
            if (cover != null) {
                return cover;
            }
        }
        if (data.getTracks() == null) {
            return null;
        }
        for (PlaylistTrack track : data.getTracks()) {
            if (track == null || track.getRefResourceId() == null) {
                continue;
            }
            byte[] jpeg = thumbs.get(track.getRefResourceId().toString());
            if (jpeg != null) {
                return jpeg;
            }
        }
        return null;
    }

    private static byte[] urlTemplateThumb(UrlTemplateEntity template) {
        UrlTemplateData data = template.getUrlTemplateData();
        if (data == null) {
            return null;
        }
        return TreePreviews.jpeg(UrlTemplateImages.rawBytes(data.getUrlImage()));
    }

    private byte[] appThumb(AppDisplayDTO app) {
        if (app == null || !app.isProductLogo() || staticAppDeployer == null || app.getSlug() == null) {
            return null;
        }
        try {
            Path root = staticAppDeployer.pagesRoot();
            if (root == null) {
                return null;
            }
            Path logo = root.resolve(app.getSlug().trim()).resolve(StaticAppDeployer.PRODUCT_LOGO);
            return TreePreviews.jpegFile(logo);
        } catch (IOException e) {
            return null;
        }
    }

    private byte[] themeThumb(ThemeDisplayDTO theme) {
        if (theme == null || !theme.isHasPreview() || theme.getThemeId() == null) {
            return null;
        }
        String id = theme.getThemeId();
        try {
            Path dir = CseDisk.themesDir().toPath().resolve(id);
            byte[] jpeg = TreePreviews.jpegFile(dir.resolve("preview.jpg"));
            if (jpeg == null) {
                jpeg = TreePreviews.jpegFile(dir.resolve("preview.png"));
            }
            if (jpeg != null) {
                return jpeg;
            }
        } catch (IOException e) {
            LOGGER.debug("No disk preview for theme {}", id);
        }
        String classpath = String.format(THEME_CLASSPATH_PREVIEW, id);
        try (InputStream in = ThemeCatalog.class.getResourceAsStream(classpath)) {
            if (in == null) {
                return null;
            }
            return TreePreviews.jpeg(in.readAllBytes());
        } catch (IOException e) {
            return null;
        }
    }

    private byte[] previewOf(String id) {
        return orEmpty(previews.get()).get(id);
    }

    private static Map<String, byte[]> orEmpty(Map<String, byte[]> map) {
        return map == null ? Map.of() : map;
    }

    private record ContentCache(ResourceGroupDisplayDTO tree, List<AppDisplayDTO> apps,
            List<ThemeDisplayDTO> themes, List<PlaylistEntity> playlists,
            List<UrlTemplateEntity> urlTemplates, List<CategoryEntity> categories, List<ItemEntity> items) {
        static ContentCache empty() {
            return new ContentCache(EditorTree.contentBranch(List.of(), List.of()),
                    List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        }
    }

    private String normalize(String groupId) {
        return ResourceGroupTreeView.homeKey(snapshot.get(), groupId);
    }
}

package com.ravenherz.cse.util;

import com.ravenherz.cse.dal.EntityAccess;
import com.ravenherz.cse.dal.dao.AppService;
import com.ravenherz.cse.dal.dao.CategoryService;
import com.ravenherz.cse.dal.dao.ItemService;
import com.ravenherz.cse.dal.dao.ResourceService;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.CategoryEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.ResourceGroupEntity;
import com.ravenherz.cse.dal.dto.basic.AlbumData;
import com.ravenherz.cse.dal.dto.basic.CategoryData;
import com.ravenherz.cse.dal.dto.basic.PageData;
import com.ravenherz.cse.dal.dto.basic.ResourceData;
import com.ravenherz.cse.dal.dto.basic.enums.AccessType;
import com.ravenherz.cse.dal.dto.basic.enums.ResourceType;
import com.ravenherz.cse.present.AppDisplayDTO;
import com.ravenherz.cse.present.CategorySectionDTO;
import com.ravenherz.cse.present.EditorContentCatalog;
import com.ravenherz.cse.util.staticapps.StaticAppDeployer;
import com.ravenherz.cse.util.video.VideoStatus;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expands {@code <cse-*>} tags left in page Markdown/HTML after CommonMark.
 * Playlist markup stays in {@link PlaylistEmbedProcessor}.
 */
@Component
public class CseEmbedProcessor {

    public interface ImageLookup {
        ResourceEntity find(String id);
    }

    public interface CategoryLookup {
        CategoryEntity findByItemName(String itemName);

        List<ItemEntity> itemsIn(CategoryEntity category);
    }

    public interface PageLookup {
        ItemEntity findByName(String uniqueUriName);

        ResourceEntity featuredImage(ItemEntity item);
    }

    public interface AppLookup {
        AppDisplayDTO findBySlug(String slug);
    }

    private static final Pattern ID_ATTR = Pattern.compile(
            "(?i)\\bid\\s*=\\s*[\"']([^\"']+)[\"']");
    private static final Pattern IMAGE_TAG = tagPattern("cse-image");
    private static final Pattern VIDEO_TAG = tagPattern("cse-video");
    private static final Pattern CATEGORY_TAG = tagPattern("cse-category");
    private static final Pattern PAGE_TAG = tagPattern("cse-page");
    private static final Pattern APP_TAG = tagPattern("cse-app");
    private static final int EXCERPT_LEN = 160;

    private static volatile CseEmbedProcessor instance;

    private final ImageLookup images;
    private final CategoryLookup categories;
    private final PageLookup pages;
    private final AppLookup apps;

    @Autowired
    public CseEmbedProcessor(ObjectProvider<ResourceService> resourceService,
            ObjectProvider<CategoryService> categoryService,
            ObjectProvider<ItemService> itemService,
            ObjectProvider<AppService> appService,
            ObjectProvider<StaticAppDeployer> deployer) {
        ResourceService resources = resourceService == null ? null : resourceService.getIfAvailable();
        CategoryService categoryDao = categoryService == null ? null : categoryService.getIfAvailable();
        ItemService itemDao = itemService == null ? null : itemService.getIfAvailable();
        AppService appDao = appService == null ? null : appService.getIfAvailable();
        StaticAppDeployer packDeployer = deployer == null ? null : deployer.getIfAvailable();
        this.images = id -> findImage(resources, id);
        this.categories = new CategoryLookup() {
            @Override
            public CategoryEntity findByItemName(String itemName) {
                return findCategory(categoryDao, itemName);
            }

            @Override
            public List<ItemEntity> itemsIn(CategoryEntity category) {
                if (itemDao == null || category == null) {
                    return List.of();
                }
                List<ItemEntity> found = itemDao.getAllByCategory(category);
                return found == null ? List.of() : found;
            }
        };
        this.pages = new PageLookup() {
            @Override
            public ItemEntity findByName(String uniqueUriName) {
                return itemDao == null ? null : itemDao.getByName(uniqueUriName);
            }

            @Override
            public ResourceEntity featuredImage(ItemEntity item) {
                return featuredImageOf(resources, item);
            }
        };
        this.apps = slug -> findApp(appDao, packDeployer, slug);
    }

    static CseEmbedProcessor of(ImageLookup images, CategoryLookup categories,
            PageLookup pages, AppLookup apps) {
        return new CseEmbedProcessor(images, categories, pages, apps);
    }

    private CseEmbedProcessor(ImageLookup images, CategoryLookup categories,
            PageLookup pages, AppLookup apps) {
        this.images = images == null ? id -> null : images;
        this.categories = categories == null ? emptyCategories() : categories;
        this.pages = pages == null ? emptyPages() : pages;
        this.apps = apps == null ? slug -> null : apps;
    }

    @PostConstruct
    void register() {
        instance = this;
    }

    @PreDestroy
    void unregister() {
        if (instance == this) {
            instance = null;
        }
    }

    public static String expand(String html) {
        String withPlaylists = PlaylistEmbedProcessor.expand(html);
        String withUrls = UrlEmbedProcessor.expand(withPlaylists);
        CseEmbedProcessor processor = instance;
        if (processor == null) {
            return withUrls;
        }
        return processor.expandHtml(withUrls);
    }

    public String expandHtml(String html) {
        if (html == null || html.isEmpty()) {
            return html == null ? "" : html;
        }
        String out = expandTag(html, IMAGE_TAG, this::renderImage);
        out = expandTag(out, VIDEO_TAG, this::renderVideo);
        out = expandTag(out, CATEGORY_TAG, this::renderCategory);
        out = expandTag(out, PAGE_TAG, this::renderPage);
        return expandTag(out, APP_TAG, this::renderApp);
    }

    private static String expandTag(String html, Pattern tag, Function<String, String> renderer) {
        Matcher matcher = tag.matcher(html);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String attrs = matcher.group(1) == null ? "" : matcher.group(1);
            Matcher idMatcher = ID_ATTR.matcher(attrs);
            String id = idMatcher.find() ? idMatcher.group(1).trim() : "";
            matcher.appendReplacement(out, Matcher.quoteReplacement(renderer.apply(id)));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private String renderImage(String id) {
        ResourceEntity resource = images.find(id);
        if (!isPublicImage(resource)) {
            return missing("image", "Image not found");
        }
        ResourceData data = resource.getResourceData();
        String src = publicSrc(data.getPathPublic());
        if (src.isBlank()) {
            return missing("image", "Image not found");
        }
        String alt = data.getImageDescription();
        if (alt == null || alt.isBlank()) {
            alt = data.getFileName();
        }
        if (alt == null) {
            alt = "";
        }
        return "<figure class=\"cse-image\"><img src=\"" + escape(src)
                + "\" alt=\"" + escape(alt) + "\"/></figure>";
    }

    private String renderVideo(String id) {
        ResourceEntity resource = images.find(id);
        if (!isPublicVideo(resource)) {
            return missing("video", "Video not found");
        }
        ResourceData data = resource.getResourceData();
        if (VideoStatus.processing(data)) {
            return "<figure class=\"cse-video cse-video-processing\">Video is processing</figure>";
        }
        if (VideoStatus.failed(data) || !VideoStatus.ready(data)) {
            return missing("video", "Video not found");
        }
        String src = publicSrc(data.getPathPublic());
        if (src.isBlank()) {
            return missing("video", "Video not found");
        }
        String poster = "";
        if (resource.getPreviewData() != null) {
            poster = publicSrc(resource.getPreviewData().getPathPublic());
        }
        StringBuilder html = new StringBuilder();
        html.append("<figure class=\"cse-video\"><div class=\"cse-video-frame\">");
        html.append("<video src=\"").append(escape(src)).append("\"");
        if (!poster.isBlank()) {
            html.append(" poster=\"").append(escape(poster)).append("\"");
        }
        html.append(" playsinline preload=\"metadata\"></video></div></figure>");
        return html.toString();
    }

    private String renderCategory(String id) {
        CategoryEntity category = categories.findByItemName(id);
        CategoryData data = category == null ? null : category.getCategoryData();
        if (data == null || !data.isActive() || !publiclyReadable(category)) {
            return missing("category", "Category not found");
        }
        String itemName = data.getItemName() == null ? "" : data.getItemName().trim();
        if (itemName.isBlank()) {
            return missing("category", "Category not found");
        }
        String title = firstNonBlank(data.getNavigationTitle(), itemName);
        String desc = excerpt(data.getNavigationDescription());
        List<ItemEntity> items = categories.itemsIn(category);
        int count = items == null ? 0 : items.size();
        String meta = count == 1 ? "1 page" : count + " pages";
        String href = "./?category=" + escapeUrl(itemName);
        ResourceEntity cover = firstCover(items);
        return card("category", href, publicSrc(pathOf(cover)), title, desc, meta);
    }

    private String renderPage(String id) {
        ItemEntity item = pages.findByName(id);
        if (item == null || item.getUniqueUriName() == null || item.getUniqueUriName().isBlank()
                || !publiclyReadable(item)) {
            return missing("page", "Page not found");
        }
        boolean album = item.isAlbum();
        String uri = item.getUniqueUriName().trim();
        String title;
        String desc;
        if (album) {
            AlbumData data = item.getAlbumData() == null ? new AlbumData() : item.getAlbumData();
            title = firstNonBlank(data.getTitle(), uri);
            desc = excerpt(firstNonBlank(
                    blankOrSame(data.getHeader(), title) ? null : data.getHeader(),
                    data.getSubHeader()));
        } else {
            PageData data = item.getPageData() == null ? new PageData() : item.getPageData();
            title = firstNonBlank(data.getTitle(), uri);
            desc = excerpt(firstNonBlank(
                    blankOrSame(data.getHeader(), title) ? null : data.getHeader(),
                    data.getSubHeader()));
        }
        String href = album ? "./?album=" + escapeUrl(uri) : "./?page=" + escapeUrl(uri);
        return card("page", href, publicSrc(pathOf(pages.featuredImage(item))),
                title, desc, album ? "Album" : "Page");
    }

    private String renderApp(String id) {
        AppDisplayDTO app = apps.findBySlug(id);
        if (app == null || app.getSlug() == null || app.getSlug().isBlank()) {
            return missing("app", "App not found");
        }
        String slug = app.getSlug().trim();
        String title = firstNonBlank(app.getName(), slug);
        String desc = excerpt(app.getDescription());
        String meta = joinMeta(app.getVersion(), app.getAuthor());
        String href = "./apps/" + escapeUrl(slug) + "/";
        String src = app.isProductLogo() ? "./apps/" + escapeUrl(slug) + "/product-logo.jpg" : "";
        return card("app", href, src, title, desc, meta);
    }

    private ResourceEntity firstCover(List<ItemEntity> items) {
        if (items == null) {
            return null;
        }
        for (ItemEntity item : items) {
            ResourceEntity cover = pages.featuredImage(item);
            if (pathOf(cover) != null) {
                return cover;
            }
        }
        return null;
    }

    private static String card(String kind, String href, String src, String title,
            String desc, String meta) {
        StringBuilder html = new StringBuilder();
        html.append("<a class=\"cse-embed cse-embed-").append(escape(kind))
                .append("\" href=\"").append(escape(href)).append("\">");
        html.append("<span class=\"cse-embed-media\">");
        if (src != null && !src.isBlank()) {
            html.append("<img src=\"").append(escape(src)).append("\" alt=\"\"/>");
        } else {
            html.append("<span class=\"cse-embed-placeholder\" aria-hidden=\"true\"></span>");
        }
        html.append("</span><span class=\"cse-embed-body\">");
        html.append("<span class=\"cse-embed-title\">").append(escape(title)).append("</span>");
        if (desc != null && !desc.isBlank()) {
            html.append("<span class=\"cse-embed-desc\">").append(escape(desc)).append("</span>");
        }
        if (meta != null && !meta.isBlank()) {
            html.append("<span class=\"cse-embed-meta\">").append(escape(meta)).append("</span>");
        }
        html.append("</span></a>");
        return html.toString();
    }

    private static String missing(String kind, String label) {
        return "<div class=\"cse-embed cse-embed-missing\" data-embed=\""
                + escape(kind) + "\">" + escape(label) + "</div>";
    }

    private static boolean isPublicImage(ResourceEntity resource) {
        if (resource == null || resource.getResourceData() == null || !publiclyReadable(resource)) {
            return false;
        }
        ResourceData data = resource.getResourceData();
        if (data.getType() == ResourceType.IMAGE) {
            return true;
        }
        String path = data.getPathPublic();
        return path != null && ResourceType.getByFileName(path) == ResourceType.IMAGE;
    }

    private static boolean isPublicVideo(ResourceEntity resource) {
        if (resource == null || resource.getResourceData() == null || !publiclyReadable(resource)) {
            return false;
        }
        ResourceData data = resource.getResourceData();
        if (data.getType() == ResourceType.VIDEO) {
            return true;
        }
        String path = data.getPathPublic();
        return path != null && ResourceType.getByFileName(path) == ResourceType.VIDEO;
    }

    private static boolean publiclyReadable(BasicEntity entity) {
        if (entity == null) {
            return false;
        }
        if (entity.getHistoryData() == null || entity.getHistoryData().getEvents() == null) {
            return true;
        }
        return EntityAccess.isAccessible(entity, AccessType.ACCESS_READ, null);
    }

    private static ResourceEntity findImage(ResourceService resources, String id) {
        if (resources == null || id == null || id.isBlank()) {
            return null;
        }
        String raw = id.trim();
        if (ObjectId.isValid(raw)) {
            BasicEntity found = resources.getById(ResourceEntity.class, new ObjectId(raw));
            if (found instanceof ResourceEntity resource) {
                return resource;
            }
        }
        return resources.getByPublicPath(normalizePublicPath(raw));
    }

    private static CategoryEntity findCategory(CategoryService categoryDao, String itemName) {
        if (categoryDao == null || itemName == null || itemName.isBlank()) {
            return null;
        }
        String requested = itemName.trim();
        String slug = CategorySectionDTO.slugify(requested);
        List<CategoryEntity> all = categoryDao.getAllCategories();
        if (all == null) {
            return null;
        }
        for (CategoryEntity candidate : all) {
            if (candidate == null || candidate.getCategoryData() == null) {
                continue;
            }
            String name = candidate.getCategoryData().getItemName();
            if (requested.equals(name) || slug.equals(CategorySectionDTO.slugify(name))) {
                return candidate;
            }
        }
        return null;
    }

    private static ResourceEntity featuredImageOf(ResourceService resources, ItemEntity item) {
        if (item == null) {
            return null;
        }
        if (item.isAlbum()) {
            AlbumData album = item.getAlbumData();
            if (album == null) {
                return null;
            }
            ResourceGroupEntity group = album.getRefResourceGroup();
            if (group == null && album.getRefResourceGroupId() != null) {
                group = new ResourceGroupEntity();
                group.setId(album.getRefResourceGroupId());
            }
            if (resources == null || group == null) {
                return null;
            }
            List<ResourceEntity> images = resources.getImagesByGroup(group);
            return images == null || images.isEmpty() ? null : images.get(0);
        }
        PageData page = item.getPageData();
        if (page == null) {
            return null;
        }
        if (page.getRefImage() != null) {
            return page.getRefImage();
        }
        if (resources == null || page.getRefImageId() == null) {
            return null;
        }
        BasicEntity found = resources.getById(ResourceEntity.class, page.getRefImageId());
        return found instanceof ResourceEntity resource ? resource : null;
    }

    private static AppDisplayDTO findApp(AppService appDao, StaticAppDeployer deployer, String slug) {
        if (slug == null || slug.isBlank()) {
            return null;
        }
        String needle = slug.trim().toLowerCase(Locale.ROOT);
        List<AppDisplayDTO> all = EditorContentCatalog.apps(appDao, deployer);
        if (all == null) {
            return null;
        }
        for (AppDisplayDTO app : all) {
            if (app != null && app.getSlug() != null && needle.equals(app.getSlug().toLowerCase(Locale.ROOT))) {
                return app;
            }
        }
        return null;
    }

    static String normalizePublicPath(String raw) {
        if (raw == null) {
            return "";
        }
        String path = raw.trim();
        if (path.startsWith("./content-protected")) {
            path = path.substring("./content-protected".length());
        } else if (path.startsWith("/content-protected")) {
            path = path.substring("/content-protected".length());
        }
        if (path.isEmpty()) {
            return "";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    static String publicSrc(String pathPublic) {
        if (pathPublic == null || pathPublic.isBlank()) {
            return "";
        }
        String path = normalizePublicPath(pathPublic);
        return path.isBlank() ? "" : "./content-protected" + path;
    }

    private static String pathOf(ResourceEntity resource) {
        if (resource == null || resource.getResourceData() == null) {
            return null;
        }
        String path = resource.getResourceData().getPathPublic();
        return path == null || path.isBlank() ? null : path;
    }

    private static String excerpt(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim().replaceAll("\\s+", " ");
        if (trimmed.length() <= EXCERPT_LEN) {
            return trimmed;
        }
        int cut = trimmed.lastIndexOf(' ', EXCERPT_LEN);
        if (cut < EXCERPT_LEN / 2) {
            cut = EXCERPT_LEN;
        }
        return trimmed.substring(0, cut).trim() + "…";
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static boolean blankOrSame(String value, String other) {
        return value == null || value.isBlank() || value.trim().equals(other);
    }

    private static String joinMeta(String left, String right) {
        boolean hasLeft = left != null && !left.isBlank();
        boolean hasRight = right != null && !right.isBlank();
        if (hasLeft && hasRight) {
            return left.trim() + " · " + right.trim();
        }
        if (hasLeft) {
            return left.trim();
        }
        return hasRight ? right.trim() : "";
    }

    private static String escapeUrl(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == ' ') {
                out.append("%20");
            } else if (ch == '"' || ch == '<' || ch == '>' || ch == '&' || ch == '#') {
                out.append('%');
                out.append(String.format("%02X", (int) ch));
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }

    private static String escape(String value) {
        return PlaylistEmbedProcessor.escape(value);
    }

    private static Pattern tagPattern(String tag) {
        return Pattern.compile("(?is)<" + tag + "\\b([^>]*)(?:\\s*/>|>\\s*</" + tag + ">)");
    }

    private static CategoryLookup emptyCategories() {
        return new CategoryLookup() {
            @Override
            public CategoryEntity findByItemName(String itemName) {
                return null;
            }

            @Override
            public List<ItemEntity> itemsIn(CategoryEntity category) {
                return List.of();
            }
        };
    }

    private static PageLookup emptyPages() {
        return new PageLookup() {
            @Override
            public ItemEntity findByName(String uniqueUriName) {
                return null;
            }

            @Override
            public ResourceEntity featuredImage(ItemEntity item) {
                return null;
            }
        };
    }
}

package com.ravenherz.cse.util;

import com.ravenherz.cse.dal.EntityAccess;
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

    public record AppCard(String slug, String name, String description, String version, String author,
            boolean productLogo) {}

    public interface AppLookup {
        AppCard findBySlug(String slug);
    }

    private static final Pattern ID_ATTR = Pattern.compile(
            "(?i)\\bid\\s*=\\s*[\"']([^\"']+)[\"']");
    private static final Pattern ATTR = Pattern.compile(
            "(?i)\\b([a-zA-Z][\\w-]*)\\s*=\\s*[\"']([^\"']*)[\"']");
    private static final Pattern IMAGE_TAG = tagPattern("cse-image");
    private static final Pattern VIDEO_TAG = tagPattern("cse-video");
    private static final Pattern BINARY_TAG = tagPattern("cse-binary");
    private static final Pattern CATEGORY_TAG = tagPattern("cse-category");
    private static final Pattern PAGE_TAG = tagPattern("cse-page");
    private static final Pattern APP_TAG = tagPattern("cse-app");
    private static final Pattern INTERVAL_TAG = tagPattern("cse-interval");
    private static final Pattern CV_IMG_CARD_TAG = tagPattern("cv-img-card");
    private static final Pattern CV_CARD_TAG = tagPattern("cv-card");
    private static final int EXCERPT_LEN = 160;
    private static final int DEFAULT_CV_IMAGE_PX = 64;
    private static final int MIN_BINARY_PX = 16;
    private static final int MAX_BINARY_PX = 4096;

    private static volatile CseEmbedProcessor instance;

    private final ImageLookup images;
    private final CategoryLookup categories;
    private final PageLookup pages;
    private final AppLookup apps;

    @Autowired
    public CseEmbedProcessor(ObjectProvider<ResourceService> resourceService,
            ObjectProvider<CategoryService> categoryService,
            ObjectProvider<ItemService> itemService,
            ObjectProvider<AppLookup> appLookup) {
        ResourceService resources = resourceService == null ? null : resourceService.getIfAvailable();
        CategoryService categoryDao = categoryService == null ? null : categoryService.getIfAvailable();
        ItemService itemDao = itemService == null ? null : itemService.getIfAvailable();
        AppLookup resolvedApps = appLookup == null ? null : appLookup.getIfAvailable();
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
        this.apps = resolvedApps == null ? slug -> null : resolvedApps;
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
        String out = CseMdProcessor.expand(html);
        out = expandTag(out, IMAGE_TAG, this::renderImage);
        out = expandTag(out, VIDEO_TAG, this::renderVideo);
        out = expandAttrs(out, BINARY_TAG, this::renderBinary);
        out = expandTag(out, CATEGORY_TAG, this::renderCategory);
        out = expandAttrs(out, PAGE_TAG, this::renderPage);
        out = expandTag(out, APP_TAG, this::renderApp);
        out = expandAttrs(out, INTERVAL_TAG, this::renderInterval);
        out = expandAttrs(out, CV_IMG_CARD_TAG, this::renderCvImgCard);
        return expandAttrs(out, CV_CARD_TAG, this::renderCvCard);
    }

    private String renderInterval(String attrs) {
        String formatted = CvEmploymentInterval.format(attr(attrs, "interval"));
        if (formatted.isBlank()) {
            return "";
        }
        return "<span class=\"cse-interval\">" + escape(formatted) + "</span>";
    }

    private String renderCvImgCard(String attrs) {
        String company = attr(attrs, "company").trim();
        String role = attr(attrs, "role").trim();
        String interval = CvEmploymentInterval.format(attr(attrs, "interval"));
        String location = attr(attrs, "location").trim();
        int size = cvImageSizePx(attr(attrs, "imageRectangle"));
        String imageId = attr(attrs, "imageId").trim();
        String src = "";
        String alt = company;
        if (!imageId.isEmpty()) {
            ResourceEntity resource = images.find(imageId);
            if (isPublicImage(resource)) {
                ResourceData data = resource.getResourceData();
                src = publicSrc(data.getPathPublic());
                if (alt.isBlank()) {
                    alt = firstNonBlank(data.getImageDescription(), data.getFileName());
                }
            }
        }
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"cv-img-card\">");
        html.append("<div class=\"cv-img-card-media\" style=\"width:").append(size)
                .append("px;height:").append(size).append("px\">");
        if (!src.isBlank()) {
            html.append("<img src=\"").append(escape(src)).append("\" alt=\"")
                    .append(escape(alt)).append("\"/>");
        } else {
            html.append("<span class=\"cv-img-card-placeholder\" aria-hidden=\"true\"></span>");
        }
        html.append("</div>");
        appendCvCardBody(html, company, role, interval, location);
        html.append("</div>");
        return html.toString();
    }

    private String renderCvCard(String attrs) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"cv-card\">");
        appendCvCardBody(html,
                attr(attrs, "company").trim(),
                attr(attrs, "role").trim(),
                CvEmploymentInterval.format(attr(attrs, "interval")),
                attr(attrs, "location").trim());
        html.append("</div>");
        return html.toString();
    }

    private static void appendCvCardBody(StringBuilder html, String company, String role,
            String interval, String location) {
        html.append("<div class=\"cv-card-body\">");
        if (!company.isBlank()) {
            html.append("<h3>").append(escape(company)).append("</h3>");
        }
        if (!role.isBlank()) {
            html.append("<strong class=\"cv-card-role\">").append(escape(role)).append("</strong>");
        }
        if (!interval.isBlank()) {
            html.append("<span class=\"cv-card-interval\">").append(escape(interval)).append("</span>");
        }
        if (!location.isBlank()) {
            html.append("<span class=\"cv-card-location\">").append(escape(location)).append("</span>");
        }
        html.append("</div>");
    }

    static int cvImageSizePx(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_CV_IMAGE_PX;
        }
        Matcher matcher = Pattern.compile("^\\s*(\\d{1,4})\\s*(px)?\\s*$", Pattern.CASE_INSENSITIVE)
                .matcher(raw);
        if (!matcher.matches()) {
            return DEFAULT_CV_IMAGE_PX;
        }
        int size = Integer.parseInt(matcher.group(1));
        if (size < 16) {
            return 16;
        }
        return Math.min(size, 1024);
    }

    static Integer optionalMaxPx(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Matcher matcher = Pattern.compile("^\\s*(\\d{1,4})\\s*(px)?\\s*$", Pattern.CASE_INSENSITIVE)
                .matcher(raw);
        if (!matcher.matches()) {
            return null;
        }
        int size = Integer.parseInt(matcher.group(1));
        if (size < MIN_BINARY_PX) {
            return MIN_BINARY_PX;
        }
        return Math.min(size, MAX_BINARY_PX);
    }

    private static String binaryMediaStyle(Integer width, Integer height) {
        if (width == null && height == null) {
            return "";
        }
        StringBuilder style = new StringBuilder("width:auto;height:auto;flex:0 0 auto;min-height:0;");
        if (width != null) {
            style.append("max-width:").append(width).append("px;");
        }
        if (height != null) {
            style.append("max-height:").append(height).append("px;");
        }
        return style.toString();
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

    private static String expandAttrs(String html, Pattern tag, Function<String, String> renderer) {
        Matcher matcher = tag.matcher(html);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String attrs = matcher.group(1) == null ? "" : matcher.group(1);
            matcher.appendReplacement(out, Matcher.quoteReplacement(renderer.apply(attrs)));
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
        if (videoProcessing(data)) {
            return "<figure class=\"cse-video cse-video-processing\">Video is processing</figure>";
        }
        if (videoFailed(data) || !videoReady(data)) {
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

    private String renderBinary(String attrs) {
        String id = attr(attrs, "id").trim();
        ResourceEntity resource = images.find(id);
        if (!isPublicBinary(resource)) {
            return missing("binary", "File not found");
        }
        ResourceData data = resource.getResourceData();
        String href = publicSrc(data.getPathPublic());
        if (href.isBlank()) {
            return missing("binary", "File not found");
        }
        boolean compact = isMediumEmbed(attr(attrs, "size"));
        String title = firstNonBlank(attr(attrs, "textOverride"),
                compact ? firstNonBlank(data.getImageDescription(), data.getFileName()) : "");
        String preview = "";
        if (resource.getPreviewData() != null) {
            preview = publicSrc(resource.getPreviewData().getPathPublic());
        }
        return card("binary", href, preview, title, "", "", compact, true,
                compact ? "" : binaryMediaStyle(optionalMaxPx(attr(attrs, "width")),
                        optionalMaxPx(attr(attrs, "height"))));
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

    private String renderPage(String attrs) {
        String id = attr(attrs, "id").trim();
        boolean compact = isMediumEmbed(attr(attrs, "size"));
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
            title = firstNonBlank(data.getHeader(), uri);
            desc = excerpt(firstNonBlank(
                    blankOrSame(data.getHeader(), title) ? null : data.getHeader(),
                    data.getSubHeader()));
        } else {
            PageData data = item.getPageData() == null ? new PageData() : item.getPageData();
            title = firstNonBlank(data.getHeader(), uri);
            desc = excerpt(firstNonBlank(
                    blankOrSame(data.getHeader(), title) ? null : data.getHeader(),
                    data.getSubHeader()));
        }
        String href = album ? "./?album=" + escapeUrl(uri) : "./?page=" + escapeUrl(uri);
        return card("page", href, publicSrc(pathOf(pages.featuredImage(item))),
                title, compact ? "" : desc, compact ? "" : (album ? "Album" : "Page"), compact);
    }

    private String renderApp(String id) {
        AppCard app = apps.findBySlug(id);
        if (app == null || app.slug() == null || app.slug().isBlank()) {
            return missing("app", "App not found");
        }
        String slug = app.slug().trim();
        String title = firstNonBlank(app.name(), slug);
        String desc = excerpt(app.description());
        String meta = joinMeta(app.version(), app.author());
        String href = "./apps/" + escapeUrl(slug) + "/";
        String src = app.productLogo() ? "./apps/" + escapeUrl(slug) + "/product-logo.jpg" : "";
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
        return card(kind, href, src, title, desc, meta, false, false, "");
    }

    private static String card(String kind, String href, String src, String title,
            String desc, String meta, boolean compact) {
        return card(kind, href, src, title, desc, meta, compact, false, "");
    }

    private static String card(String kind, String href, String src, String title,
            String desc, String meta, boolean compact, boolean newTab, String mediaStyle) {
        StringBuilder html = new StringBuilder();
        html.append("<a class=\"cse-embed cse-embed-").append(escape(kind));
        if (compact) {
            html.append(" cse-embed-m");
        }
        html.append("\" href=\"").append(escape(href)).append("\"");
        if (newTab) {
            html.append(" target=\"_blank\" rel=\"noopener\"");
        }
        html.append(">");
        html.append("<span class=\"cse-embed-media\"");
        if (mediaStyle != null && !mediaStyle.isBlank()) {
            html.append(" style=\"").append(mediaStyle).append("\"");
        }
        html.append(">");
        if (src != null && !src.isBlank()) {
            html.append("<img src=\"").append(escape(src)).append("\" alt=\"\"/>");
        } else {
            html.append("<span class=\"cse-embed-placeholder\" aria-hidden=\"true\"></span>");
        }
        boolean hasBody = (title != null && !title.isBlank())
                || (!compact && desc != null && !desc.isBlank())
                || (!compact && meta != null && !meta.isBlank());
        if (hasBody) {
            html.append("</span><span class=\"cse-embed-body\">");
            if (title != null && !title.isBlank()) {
                html.append("<span class=\"cse-embed-title\">").append(escape(title)).append("</span>");
            }
            if (!compact && desc != null && !desc.isBlank()) {
                html.append("<span class=\"cse-embed-desc\">").append(escape(desc)).append("</span>");
            }
            if (!compact && meta != null && !meta.isBlank()) {
                html.append("<span class=\"cse-embed-meta\">").append(escape(meta)).append("</span>");
            }
            html.append("</span></a>");
        } else {
            html.append("</span></a>");
        }
        return html.toString();
    }

    private static boolean isMediumEmbed(String size) {
        return size != null && "m".equalsIgnoreCase(size.trim());
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

    private static boolean isPublicBinary(ResourceEntity resource) {
        if (resource == null || resource.getResourceData() == null || !publiclyReadable(resource)) {
            return false;
        }
        ResourceData data = resource.getResourceData();
        if (data.getType() == ResourceType.BINARY) {
            return true;
        }
        String path = data.getPathPublic();
        return path != null && ResourceType.getByFileName(path) == ResourceType.BINARY;
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
        String slug = slugify(requested);
        List<CategoryEntity> all = categoryDao.getAllCategories();
        if (all == null) {
            return null;
        }
        for (CategoryEntity candidate : all) {
            if (candidate == null || candidate.getCategoryData() == null) {
                continue;
            }
            String name = candidate.getCategoryData().getItemName();
            if (requested.equals(name) || slug.equals(slugify(name))) {
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

    private static boolean videoProcessing(ResourceData data) {
        return "processing".equalsIgnoreCase(videoTranscode(data));
    }

    private static boolean videoFailed(ResourceData data) {
        return "failed".equalsIgnoreCase(videoTranscode(data));
    }

    private static boolean videoReady(ResourceData data) {
        if (data == null || data.getType() != ResourceType.VIDEO) {
            return false;
        }
        String status = videoTranscode(data);
        return status.isEmpty() || "ready".equalsIgnoreCase(status);
    }

    private static String videoTranscode(ResourceData data) {
        if (data == null || data.getMetadata() == null) {
            return "";
        }
        String status = data.getMetadata().get("transcode");
        return status == null ? "" : status.trim();
    }

    private static String slugify(String raw) {
        if (raw == null || raw.isBlank()) {
            return "section";
        }
        String slug = raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        slug = slug.replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "section" : slug;
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

    private static String attr(String attrs, String name) {
        if (attrs == null || attrs.isBlank() || name == null) {
            return "";
        }
        Matcher matcher = ATTR.matcher(attrs);
        while (matcher.find()) {
            if (name.equalsIgnoreCase(matcher.group(1))) {
                return matcher.group(2);
            }
        }
        return "";
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

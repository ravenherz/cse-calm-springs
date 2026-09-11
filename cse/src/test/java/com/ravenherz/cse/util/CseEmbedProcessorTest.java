package com.ravenherz.cse.util;

import com.ravenherz.cse.dal.dto.CategoryEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.basic.AlbumData;
import com.ravenherz.cse.dal.dto.basic.CategoryData;
import com.ravenherz.cse.dal.dto.basic.PageData;
import com.ravenherz.cse.dal.dto.basic.ResourceData;
import com.ravenherz.cse.dal.dto.basic.enums.ResourceType;
import com.ravenherz.cse.present.AppDisplayDTO;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CseEmbedProcessorTest {

    @Test
    void imageExpandsFromObjectIdAndPublicPath() {
        ObjectId id = new ObjectId();
        ResourceEntity image = image(id, "/user/res/images/cover.jpg", ResourceType.IMAGE);
        CseEmbedProcessor processor = processor(image);
        String byId = processor.expandHtml("<cse-image id=\"" + id + "\"></cse-image>");
        assertTrue(byId.contains("class=\"cse-image\""));
        assertTrue(byId.contains("src=\"./content-protected/user/res/images/cover.jpg\""));
        assertTrue(byId.contains("alt=\"cover.jpg\""));
        assertFalse(byId.contains("<cse-image"));

        String byPath = processor.expandHtml("<cse-image id=\"/user/res/images/cover.jpg\" />");
        assertTrue(byPath.contains("src=\"./content-protected/user/res/images/cover.jpg\""));

        String prefixed = processor.expandHtml(
                "<cse-image id=\"./content-protected/user/res/images/cover.jpg\"></cse-image>");
        assertTrue(prefixed.contains("src=\"./content-protected/user/res/images/cover.jpg\""));
    }

    @Test
    void audioResourceDoesNotExpandAsImage() {
        ObjectId id = new ObjectId();
        ResourceEntity audio = image(id, "/user/res/audio/tide.mp3", ResourceType.AUDIO);
        CseEmbedProcessor processor = processor(audio);
        String html = processor.expandHtml("<cse-image id=\"" + id + "\"></cse-image>");
        assertTrue(html.contains("cse-embed-missing"));
        assertTrue(html.contains("Image not found"));
        assertFalse(html.contains("<img"));
    }

    @Test
    void unknownImageKeepsSurroundingHtml() {
        CseEmbedProcessor processor = processor(null);
        String html = processor.expandHtml("before <cse-image id=\"missing\"></cse-image> after");
        assertTrue(html.startsWith("before "));
        assertTrue(html.endsWith(" after"));
        assertTrue(html.contains("Image not found"));
    }

    @Test
    void categoryCardUsesLiveCountAndTitle() {
        CategoryEntity music = category("music", "Music", "Releases and notes", true);
        ItemEntity page = page("host", "Host", false);
        CseEmbedProcessor processor = CseEmbedProcessor.of(
                id -> null,
                new CseEmbedProcessor.CategoryLookup() {
                    @Override
                    public CategoryEntity findByItemName(String itemName) {
                        return "music".equals(itemName) ? music : null;
                    }

                    @Override
                    public List<ItemEntity> itemsIn(CategoryEntity category) {
                        return List.of(page, page("second", "Second", false));
                    }
                },
                emptyPages(),
                slug -> null);
        String html = processor.expandHtml("<p>Go</p><cse-category id=\"music\"></cse-category>");
        assertTrue(html.contains("<p>Go</p>"));
        assertTrue(html.contains("class=\"cse-embed cse-embed-category\""));
        assertTrue(html.contains("href=\"./?category=music\""));
        assertTrue(html.contains("cse-embed-title"));
        assertTrue(html.contains("Music"));
        assertTrue(html.contains("Releases and notes"));
        assertTrue(html.contains("2 pages"));
        assertFalse(html.contains("<cse-category"));
    }

    @Test
    void inactiveCategoryDoesNotLeakTitle() {
        CategoryEntity hidden = category("vault", "Secret vault", "Do not show", false);
        CseEmbedProcessor processor = CseEmbedProcessor.of(
                id -> null,
                new CseEmbedProcessor.CategoryLookup() {
                    @Override
                    public CategoryEntity findByItemName(String itemName) {
                        return hidden;
                    }

                    @Override
                    public List<ItemEntity> itemsIn(CategoryEntity category) {
                        return List.of();
                    }
                },
                emptyPages(),
                slug -> null);
        String html = processor.expandHtml("<cse-category id=\"vault\"></cse-category>");
        assertTrue(html.contains("Category not found"));
        assertFalse(html.contains("Secret vault"));
        assertFalse(html.contains("Do not show"));
    }

    @Test
    void pageAndAlbumUseDifferentHrefs() {
        ItemEntity story = page("hello", "Hello", false);
        story.getPageData().setHeader("A greeting");
        ItemEntity gallery = page("shots", "Shots", true);
        gallery.getAlbumData().setSubHeader("Twelve frames");
        CseEmbedProcessor processor = CseEmbedProcessor.of(
                id -> null,
                emptyCategories(),
                new CseEmbedProcessor.PageLookup() {
                    @Override
                    public ItemEntity findByName(String uniqueUriName) {
                        if ("hello".equals(uniqueUriName)) {
                            return story;
                        }
                        if ("shots".equals(uniqueUriName)) {
                            return gallery;
                        }
                        return null;
                    }

                    @Override
                    public ResourceEntity featuredImage(ItemEntity item) {
                        return null;
                    }
                },
                slug -> null);
        String pageHtml = processor.expandHtml("<cse-page id=\"hello\"></cse-page>");
        assertTrue(pageHtml.contains("href=\"./?page=hello\""));
        assertTrue(pageHtml.contains("Hello"));
        assertTrue(pageHtml.contains("A greeting"));
        assertTrue(pageHtml.contains(">Page<"));

        String albumHtml = processor.expandHtml("<cse-page id=\"shots\"></cse-page>");
        assertTrue(albumHtml.contains("href=\"./?album=shots\""));
        assertTrue(albumHtml.contains("Shots"));
        assertTrue(albumHtml.contains("Twelve frames"));
        assertTrue(albumHtml.contains(">Album<"));
    }

    @Test
    void pageBodyIsNotUsedAsCardCopy() {
        ItemEntity story = page("hello", "Hello", false);
        story.getPageData().setDescription("# Secret draft\n\nDo not publish this.");
        CseEmbedProcessor processor = CseEmbedProcessor.of(
                id -> null,
                emptyCategories(),
                namedPage(story),
                slug -> null);
        String html = processor.expandHtml("<cse-page id=\"hello\"></cse-page>");
        assertFalse(html.contains("Secret draft"));
        assertFalse(html.contains("Do not publish"));
    }

    @Test
    void missingPageShowsFallback() {
        CseEmbedProcessor processor = CseEmbedProcessor.of(id -> null, emptyCategories(), emptyPages(), slug -> null);
        String html = processor.expandHtml("<cse-page id=\"ghost\"></cse-page>");
        assertTrue(html.contains("Page not found"));
        assertTrue(html.contains("data-embed=\"page\""));
    }

    @Test
    void appCardUsesSlugAndLogo() {
        AppDisplayDTO app = new AppDisplayDTO();
        app.setSlug("fretlab");
        app.setName("Fretboard Lab");
        app.setDescription("Practice the neck.");
        app.setVersion("1.2.0");
        app.setAuthor("Ravenherz");
        app.setProductLogo(true);
        CseEmbedProcessor processor = CseEmbedProcessor.of(
                id -> null, emptyCategories(), emptyPages(), slug -> "fretlab".equals(slug) ? app : null);
        String html = processor.expandHtml("<cse-app id=\"fretlab\"></cse-app>");
        assertTrue(html.contains("class=\"cse-embed cse-embed-app\""));
        assertTrue(html.contains("href=\"./apps/fretlab/\""));
        assertTrue(html.contains("src=\"./apps/fretlab/product-logo.jpg\""));
        assertTrue(html.contains("Fretboard Lab"));
        assertTrue(html.contains("Practice the neck."));
        assertTrue(html.contains("1.2.0 · Ravenherz"));
        assertFalse(html.contains("<cse-app"));
    }

    @Test
    void unknownAppShowsFallback() {
        CseEmbedProcessor processor = CseEmbedProcessor.of(id -> null, emptyCategories(), emptyPages(), slug -> null);
        String html = processor.expandHtml("<cse-app id=\"missing\"></cse-app>");
        assertTrue(html.contains("App not found"));
        assertFalse(html.contains("cse-embed-app\""));
    }

    @Test
    void unknownTagsStay() {
        CseEmbedProcessor processor = processor(null);
        String html = processor.expandHtml("<cse-other id=\"x\"></cse-other>");
        assertTrue(html.contains("<cse-other id=\"x\"></cse-other>"));
    }

    private static CseEmbedProcessor processor(ResourceEntity image) {
        return CseEmbedProcessor.of(
                id -> matchesImage(image, id) ? image : null,
                emptyCategories(),
                emptyPages(),
                slug -> null);
    }

    private static boolean matchesImage(ResourceEntity image, String id) {
        if (image == null || id == null) {
            return false;
        }
        String raw = id.trim();
        if (image.getId() != null && raw.equals(image.getId().toString())) {
            return true;
        }
        String path = image.getResourceData().getPathPublic();
        String normalized = CseEmbedProcessor.normalizePublicPath(raw);
        return path.equals(raw) || path.equals(normalized);
    }

    private static ResourceEntity image(ObjectId id, String path, ResourceType type) {
        ResourceData data = new ResourceData();
        data.setPathPublic(path);
        data.setType(type);
        ResourceEntity resource = new ResourceEntity();
        resource.setId(id);
        resource.setResourceData(data);
        return resource;
    }

    private static CategoryEntity category(String itemName, String title, String description, boolean active) {
        CategoryData data = new CategoryData(itemName, title, description, true, active);
        return new CategoryEntity(data, null);
    }

    private static ItemEntity page(String uri, String title, boolean album) {
        if (album) {
            AlbumData data = new AlbumData();
            data.setTitle(title);
            return new ItemEntity(uri, data, null);
        }
        PageData data = new PageData();
        data.setTitle(title);
        return new ItemEntity(uri, data, null);
    }

    private static CseEmbedProcessor.CategoryLookup emptyCategories() {
        return new CseEmbedProcessor.CategoryLookup() {
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

    private static CseEmbedProcessor.PageLookup emptyPages() {
        return new CseEmbedProcessor.PageLookup() {
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

    private static CseEmbedProcessor.PageLookup namedPage(ItemEntity item) {
        return new CseEmbedProcessor.PageLookup() {
            @Override
            public ItemEntity findByName(String uniqueUriName) {
                return item.getUniqueUriName().equals(uniqueUriName) ? item : null;
            }

            @Override
            public ResourceEntity featuredImage(ItemEntity itemEntity) {
                return null;
            }
        };
    }
}

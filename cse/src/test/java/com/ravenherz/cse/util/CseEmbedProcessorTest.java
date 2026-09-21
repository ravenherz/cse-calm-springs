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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void videoExpandsWhenReadyAndShowsProcessingPlaceholder() {
        ObjectId id = new ObjectId();
        ResourceEntity video = image(id, "/user/res/video/clip.mp4", ResourceType.VIDEO);
        video.getResourceData().addMetadata("transcode", "ready");
        CseEmbedProcessor processor = processor(video);
        String byId = processor.expandHtml("<cse-video id=\"" + id + "\"></cse-video>");
        assertTrue(byId.contains("class=\"cse-video\""));
        assertTrue(byId.contains("src=\"./content-protected/user/res/video/clip.mp4\""));
        assertTrue(byId.contains("<video"));
        assertFalse(byId.contains("<cse-video"));
        String byPath = processor.expandHtml("<cse-video id=\"/user/res/video/clip.mp4\" />");
        assertTrue(byPath.contains("src=\"./content-protected/user/res/video/clip.mp4\""));

        video.getResourceData().addMetadata("transcode", "processing");
        String processing = processor.expandHtml("<cse-video id=\"" + id + "\"></cse-video>");
        assertTrue(processing.contains("cse-video-processing"));
        assertTrue(processing.contains("Video is processing"));
        assertFalse(processing.contains("<video"));

        video.getResourceData().addMetadata("transcode", "failed");
        String failed = processor.expandHtml("<cse-video id=\"" + id + "\"></cse-video>");
        assertTrue(failed.contains("Video not found"));
        assertFalse(failed.contains("<video"));
    }

    @Test
    void imageResourceDoesNotExpandAsVideo() {
        ObjectId id = new ObjectId();
        ResourceEntity image = image(id, "/user/res/images/cover.jpg", ResourceType.IMAGE);
        CseEmbedProcessor processor = processor(image);
        String html = processor.expandHtml("<cse-video id=\"" + id + "\"></cse-video>");
        assertTrue(html.contains("cse-embed-missing"));
        assertTrue(html.contains("Video not found"));
        assertFalse(html.contains("<video"));
    }

    @Test
    void binaryExpandsPreviewAndLink() {
        ObjectId id = new ObjectId();
        ResourceEntity pdf = image(id, "/user/res/binaries/resume.pdf", ResourceType.BINARY);
        ResourceData preview = new ResourceData();
        preview.setType(ResourceType.IMAGE);
        preview.setPathPublic("/user/res/binaries/resume.pdf.low-res.jpg");
        pdf.setPreviewData(preview);
        CseEmbedProcessor processor = processor(pdf);
        String byId = processor.expandHtml("<cse-binary id=\"" + id + "\"></cse-binary>");
        assertTrue(byId.contains("class=\"cse-embed cse-embed-binary\""));
        assertTrue(byId.contains("cse-embed-media"));
        assertTrue(byId.contains("href=\"./content-protected/user/res/binaries/resume.pdf\""));
        assertTrue(byId.contains("src=\"./content-protected/user/res/binaries/resume.pdf.low-res.jpg\""));
        assertTrue(byId.contains("target=\"_blank\""));
        assertFalse(byId.contains("cse-embed-title"));
        assertFalse(byId.contains("<cse-binary"));
        String byPath = processor.expandHtml("<cse-binary id=\"/user/res/binaries/resume.pdf\" />");
        assertTrue(byPath.contains("href=\"./content-protected/user/res/binaries/resume.pdf\""));
        assertFalse(byId.contains("width:400px"));
        assertFalse(byId.contains("textOverride"));
    }

    @Test
    void binaryHonorsWidthHeightAndTextOverride() {
        ObjectId id = new ObjectId();
        ResourceEntity pdf = image(id, "/user/res/binaries/resume.pdf", ResourceType.BINARY);
        ResourceData preview = new ResourceData();
        preview.setType(ResourceType.IMAGE);
        preview.setPathPublic("/user/res/binaries/resume.pdf.low-res.jpg");
        pdf.setPreviewData(preview);
        CseEmbedProcessor processor = processor(pdf);
        String html = processor.expandHtml(
                "<cse-binary id=\"" + id + "\" width=\"400px\" height=\"300\" textOverride=\"CV\"></cse-binary>");
        assertTrue(html.contains("max-width:400px"));
        assertTrue(html.contains("max-height:300px"));
        assertFalse(html.contains("width:400px;height:300px"));
        assertTrue(html.contains("class=\"cse-embed-title\">CV</span>"));
        assertFalse(html.contains("class=\"cse-embed-title\">resume.pdf</span>"));
        assertFalse(html.contains("<cse-binary"));
    }

    @Test
    void binarySizeMIsCompactRowWithTitle() {
        ObjectId id = new ObjectId();
        ResourceEntity pdf = image(id, "/user/res/binaries/resume.pdf", ResourceType.BINARY);
        ResourceData preview = new ResourceData();
        preview.setType(ResourceType.IMAGE);
        preview.setPathPublic("/user/res/binaries/resume.pdf.low-res.jpg");
        pdf.setPreviewData(preview);
        CseEmbedProcessor processor = processor(pdf);
        String html = processor.expandHtml(
                "<cse-binary id=\"" + id + "\" size=\"m\" width=\"400px\"></cse-binary>");
        assertTrue(html.contains("cse-embed-binary"));
        assertTrue(html.contains("cse-embed-m\""));
        assertTrue(html.contains("class=\"cse-embed-title\">resume.pdf</span>"));
        assertFalse(html.contains("width:400px"));
        assertFalse(html.contains("<cse-binary"));
        String named = processor.expandHtml(
                "<cse-binary id=\"" + id + "\" size=\"m\" textOverride=\"CV\"></cse-binary>");
        assertTrue(named.contains("class=\"cse-embed-title\">CV</span>"));
        assertFalse(named.contains("class=\"cse-embed-title\">resume.pdf</span>"));
    }

    @Test
    void binaryMaxPxParsesOptionalCssPixels() {
        assertNull(CseEmbedProcessor.optionalMaxPx(null));
        assertNull(CseEmbedProcessor.optionalMaxPx(""));
        assertNull(CseEmbedProcessor.optionalMaxPx("wide"));
        assertEquals(400, CseEmbedProcessor.optionalMaxPx("400px"));
        assertEquals(16, CseEmbedProcessor.optionalMaxPx("8"));
        assertEquals(4096, CseEmbedProcessor.optionalMaxPx("4096px"));
    }

    @Test
    void imageResourceDoesNotExpandAsBinary() {
        ObjectId id = new ObjectId();
        ResourceEntity image = image(id, "/user/res/images/cover.jpg", ResourceType.IMAGE);
        CseEmbedProcessor processor = processor(image);
        String html = processor.expandHtml("<cse-binary id=\"" + id + "\"></cse-binary>");
        assertTrue(html.contains("cse-embed-missing"));
        assertTrue(html.contains("File not found"));
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
        assertFalse(pageHtml.contains("cse-embed-m\""));

        String compact = processor.expandHtml("<cse-page id=\"hello\" size=\"m\"></cse-page>");
        assertTrue(compact.contains("cse-embed-m\""));
        assertTrue(compact.contains("cse-embed-title"));
        assertTrue(compact.contains("Hello"));
        assertFalse(compact.contains("A greeting"));
        assertFalse(compact.contains(">Page<"));
        assertFalse(compact.contains("cse-embed-desc"));
        assertFalse(compact.contains("cse-embed-meta"));

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

    @Test
    void cvImgCardExpandsCompanyRoleIntervalAndImage() {
        ObjectId id = new ObjectId();
        ResourceEntity image = image(id, "/user/res/images/zvuk.png", ResourceType.IMAGE);
        CseEmbedProcessor processor = processor(image);
        String html = processor.expandHtml(
                "<cv-img-card imageId=\"" + id + "\" imageRectangle=\"256px\""
                        + " company=\"Zvuk\" role=\"Senior Big Data Engineer\""
                        + " interval=\"2025-09-01;2025-10-01\" location=\"Moscow\"></cv-img-card>");
        assertTrue(html.contains("class=\"cv-img-card\""));
        assertTrue(html.contains("class=\"cv-img-card-media\""));
        assertTrue(html.contains("width:256px;height:256px"));
        assertTrue(html.contains("src=\"./content-protected/user/res/images/zvuk.png\""));
        assertTrue(html.contains("<h3>Zvuk</h3>"));
        assertTrue(html.contains("class=\"cv-card-role\">Senior Big Data Engineer</strong>"));
        assertTrue(html.contains("September 2025 – October (1 month)"));
        assertTrue(html.contains("class=\"cv-card-location\">Moscow</span>"));
        assertFalse(html.contains("<cv-img-card"));
    }

    @Test
    void cseIntervalExpandsOngoingAndFinished() {
        CseEmbedProcessor processor = processor(null);
        String ongoing = processor.expandHtml(
                "<cse-interval interval=\"2025-09-01;Now\"></cse-interval>");
        assertTrue(ongoing.contains("class=\"cse-interval\""));
        assertTrue(ongoing.contains("September 2025 – Present"));
        assertFalse(ongoing.contains("<cse-interval"));

        String finished = processor.expandHtml(
                "<cse-interval interval=\"2025-09-01;2025-10-01\" />");
        assertEquals("<span class=\"cse-interval\">September 2025 – October (1 month)</span>",
                finished);

        String invalid = processor.expandHtml(
                "before <cse-interval interval=\"never\"></cse-interval> after");
        assertEquals("before  after", invalid);
    }

    @Test
    void cseMdExpandsInsideEmbedProcessor() {
        CseEmbedProcessor processor = processor(null);
        String html = processor.expandHtml("<cse-md paddingLeft=\"250px\">**Hi**</cse-md>");
        assertTrue(html.contains("class=\"cse-md\""));
        assertTrue(html.contains("padding-left:250px"));
        assertTrue(html.contains("<strong>Hi</strong>"));
        assertFalse(html.contains("<cse-md"));
    }

    @Test
    void cvImgCardWithoutImageKeepsTextAndDefaultSize() {
        CseEmbedProcessor processor = processor(null);
        String html = processor.expandHtml(
                "<cv-img-card company=\"Zvuk\" role=\"Engineer\" interval=\"2025-09-01;2025-10-01\"></cv-img-card>");
        assertTrue(html.contains("cv-img-card-placeholder"));
        assertTrue(html.contains("width:64px;height:64px"));
        assertTrue(html.contains("<h3>Zvuk</h3>"));
        assertFalse(html.contains("<img"));
    }

    @Test
    void cvCardOmitsImageAndKeepsText() {
        CseEmbedProcessor processor = processor(null);
        String html = processor.expandHtml(
                "<cv-card company=\"Zvuk\" role=\"Senior Big Data Engineer\""
                        + " interval=\"2022-05-01;2025-03-31\" location=\"Moscow\"></cv-card>");
        assertTrue(html.contains("class=\"cv-card\""));
        assertTrue(html.contains("<h3>Zvuk</h3>"));
        assertTrue(html.contains("class=\"cv-card-role\">Senior Big Data Engineer</strong>"));
        assertTrue(html.contains("May 2022 – March 2025"));
        assertTrue(html.contains("class=\"cv-card-location\">Moscow</span>"));
        assertFalse(html.contains("cv-img-card"));
        assertFalse(html.contains("cv-img-card-media"));
        assertFalse(html.contains("placeholder"));
        assertFalse(html.contains("<img"));
        assertFalse(html.contains("<cv-card"));
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

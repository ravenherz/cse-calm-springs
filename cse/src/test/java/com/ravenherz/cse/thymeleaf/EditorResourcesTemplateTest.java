package com.ravenherz.cse.thymeleaf;

import com.ravenherz.cse.dal.dto.CategoryEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.PlaylistEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.RoleEntity;
import com.ravenherz.cse.dal.dto.UrlTemplateEntity;
import com.ravenherz.cse.dal.dto.basic.CategoryData;
import com.ravenherz.cse.dal.dto.basic.EntityAccessConstants;
import com.ravenherz.cse.dal.dto.basic.PageData;
import com.ravenherz.cse.dal.dto.basic.PlaylistData;
import com.ravenherz.cse.dal.dto.basic.ResourceData;
import com.ravenherz.cse.dal.dto.basic.SecurityData;
import com.ravenherz.cse.dal.dto.basic.UrlTemplateData;
import com.ravenherz.cse.dal.dto.basic.enums.ResourceType;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import com.ravenherz.cse.present.AppDisplayDTO;
import com.ravenherz.cse.present.ResourceGroupDisplayDTO;
import com.ravenherz.cse.present.ResourceTreeFile;
import com.ravenherz.cse.present.ThemeDisplayDTO;
import com.ravenherz.cse.util.video.VideoStatus;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorResourcesTemplateTest {

    @Test
    void resourcesTabShowsTreePaneAndPlusTile() {
        ResourceGroupDisplayDTO content = group("content", "Content", "Content", 1);
        content.setVirtual(true);
        content.setHref("/editor/resources?group=content");
        ResourceGroupDisplayDTO apps = group("content-apps", "Apps", "Content / Apps", 2);
        apps.setVirtual(true);
        apps.setHref("/editor/resources?group=content-apps");
        ResourceGroupDisplayDTO categories = group("content-categories", "Categories", "Content / Categories", 2);
        categories.setVirtual(true);
        categories.setHref("/editor/resources?group=content-categories");
        ResourceGroupDisplayDTO music = group("category-music", "music", "Content / Categories / music", 3);
        music.setHref("/editor/resources?group=category-music");
        music.setCategoryItemName("music");
        music.setTreeFiles(List.of(
                new ResourceTreeFile("page-host", "Host (2015)", "/editor/edit?name=host",
                        ResourceTreeFile.Mark.PAGE).withKey("host"),
                new ResourceTreeFile("page-home", "Home", "/editor/edit?name=home",
                        ResourceTreeFile.Mark.ALBUM).withKey("home")));
        categories.getChildren().add(music);
        content.getChildren().add(apps);
        content.getChildren().add(categories);
        ResourceGroupDisplayDTO unsorted = group("default-id", "Unsorted", "Unsorted", 1);
        unsorted.setDefaultGroup(true);
        unsorted.setHref("/editor/resources?group=default-id");
        ResourceGroupDisplayDTO travel = group("travel", "Travel", "Travel", 1);
        travel.setHref("/editor/resources?group=travel");
        ResourceGroupDisplayDTO y2024 = group("y2024", "2024", "Travel / 2024", 2);
        y2024.setParentId("travel");
        ResourceGroupDisplayDTO iceland = group("iceland", "Iceland", "Travel / 2024 / Iceland", 3);
        iceland.setParentId("y2024");
        y2024.getChildren().add(iceland);
        travel.getChildren().add(y2024);
        travel.setDescendantFileCount(2);
        travel.setDescendantTotalSize(100);
        y2024.setDescendantFileCount(1);
        y2024.setDescendantTotalSize(40);
        y2024.setTreeFiles(List.of(new ResourceTreeFile("file1", "aurora.jpg", null,
                ResourceTreeFile.Mark.NONE, true).withKey("/u/res/image/aurora.jpg")));
        travel.setSubtreeHeight(1);

        SpringTemplateEngine engine = engine();
        MockServletContext servletContext = new MockServletContext();
        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(servletContext);
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setContextPath("/rhz-we");
        request.setRequestURI("/rhz-we/editor/resources");
        MockHttpServletResponse response = new MockHttpServletResponse();
        WebContext context = new WebContext(application.buildExchange(request, response));
        context.setVariable("resourceGroupTree", List.of(content, unsorted, travel));
        context.setVariable("assignableGroups", List.of(unsorted, travel, y2024));
        context.setVariable("selectedGroup", y2024);
        context.setVariable("selectedLeafId", null);
        context.setVariable("defaultGroupId", "default-id");
        context.setVariable("cseContextPath", "/rhz-we");
        context.setVariable("username", "owner");
        context.setVariable("navApi", false);
        context.setVariable("navResources", true);
        context.setVariable("navApps", false);
        context.setVariable("navThemes", false);
        context.setVariable("navCategoriesActive", false);
        context.setVariable("navPages", false);
        context.setVariable("navPlaylists", false);
        context.setVariable("navAccounts", false);
        context.setVariable("navRoles", false);
        context.setVariable("navSettings", false);
        context.setVariable("navSiteData", false);
        context.setVariable("navLogs", false);

        String html = engine.process("admin/editor-resources", context);

        assertFalse(html.contains("${"), html);
        assertFalse(html.contains("page-header"), html);
        assertFalse(html.contains("upload-section"), html);
        assertTrue(html.contains("Catalog"), html);
        String nav = html.substring(html.indexOf("class=\"editor-nav\""), html.indexOf("sidebar-foot"));
        assertTrue(nav.contains("/rhz-we/editor/resources"), nav);
        assertFalse(nav.contains("/editor/categories"), nav);
        assertFalse(nav.contains("/editor/playlists"), nav);
        assertFalse(nav.contains("/editor/pages"), nav);
        assertFalse(nav.contains("/editor/apps"), nav);
        assertFalse(nav.contains("/editor/themes"), nav);
        assertTrue(html.contains("resource-tree"), html);
        assertTrue(html.contains("resource-browser"), html);
        assertTrue(html.contains("cse-editor.js"), html);
        assertTrue(html.contains("cse-editor-tree.js"), html);
        assertTrue(html.contains("resource-tree-origin"), html);
        assertTrue(html.contains("resource-tree-root-label"), html);
        assertTrue(html.contains("id=\"group-default-id\""), html);
        assertTrue(html.contains("resource-lock-overlay"), html);
        assertTrue(html.contains("resource-lock"), html);
        assertTrue(html.contains("resource-tree-folder-host is-locked"), html);
        assertTrue(html.contains("resource-asterisk-overlay"), html);
        assertTrue(html.contains("resource-asterisk"), html);
        assertTrue(html.contains("group=default-id"), html);
        assertTrue(html.contains("resource-tree-folder-closed"), html);
        assertTrue(html.contains("resource-tree-folder-open"), html);
        assertTrue(html.contains("resource-tree-file-icon"), html);
        assertTrue(html.contains("resource-tree-file-mark"), html);
        assertTrue(html.contains("is-page"), html);
        assertTrue(html.contains("is-album"), html);
        assertTrue(html.contains(">Page<"), html);
        assertTrue(html.contains(">Album<"), html);
        assertTrue(html.contains("aurora.jpg"), html);
        assertTrue(html.contains("resource-tree-file-preview"), html);
        assertTrue(html.contains("/rhz-we/editor/resources/tree-preview/file1"), html);
        assertTrue(html.contains("id=\"group-content\""), html);
        assertTrue(html.contains("/rhz-we/editor/resources?group=content"), html);
        assertTrue(html.contains("/rhz-we/editor/resources?group=content-apps"), html);
        assertTrue(html.contains("/rhz-we/editor/resources?group=content-categories"), html);
        assertTrue(html.contains("/rhz-we/editor/resources?group=category-music"), html);
        assertTrue(html.contains("resource-plus-tile"), html);
        assertTrue(html.contains("placeResourceTip"), html);
        assertTrue(html.contains("id=\"file\""), html);
        assertTrue(html.contains("multiple"), html);
        assertTrue(html.contains(".mp4"), html);
        assertTrue(html.contains("/rhz-we/editor/video/progress"), html);
        assertTrue(html.contains("data-video-progress-url"), html);
        assertFalse(html.contains("thumb-tag-processing'))"), html);
        assertTrue(html.contains("id=\"group-travel\""), html);
        assertTrue(html.contains("id=\"group-y2024\""), html);
        assertFalse(html.contains("id=\"group-ungrouped\""), html);
        assertTrue(html.contains("data-default-group-id=\"default-id\""), html);
        assertTrue(html.contains(">Travel / 2024</h2>"), html);
        assertTrue(html.contains("40 bytes"), html);
        assertFalse(html.contains("aria-label=\"Parent group\""), html);
        assertFalse(html.contains("class=\"delete-btn\""), html);
        assertTrue(html.contains("/editor/resources/group/delete"), html);
        assertTrue(html.contains("resource-folder-mark"), html);
        assertTrue(html.contains("/editor/resources/group/move"), html);
        assertTrue(html.contains("data-action=\"new-group\""), html);
        assertTrue(html.contains(">New group</button>"), html);
        assertTrue(html.contains("data-action=\"new-category\""), html);
        assertTrue(html.contains(">New category</button>"), html);
        assertTrue(html.contains("data-action=\"edit\""), html);
        assertTrue(html.contains(">Edit</button>"), html);
        assertTrue(html.contains("data-action=\"new-page\""), html);
        assertTrue(html.contains("data-action=\"new-page-wrap\""), html);
        assertTrue(html.contains("id=\"resource-tree-page-submenu\""), html);
        assertTrue(html.contains("data-create-page-menu=\"true\""), html);
        assertTrue(html.contains(">New page</button>"), html);
        assertTrue(html.contains("data-create-page-href=\"/rhz-we/editor/create?categoryId=music\""), html);
        assertTrue(html.contains("data-action=\"new-album\""), html);
        assertTrue(html.contains("data-action=\"new-album-wrap\""), html);
        assertTrue(html.contains("id=\"resource-tree-album-submenu\""), html);
        assertTrue(html.contains("data-create-album-menu=\"true\""), html);
        assertTrue(html.contains(">New album</button>"), html);
        assertTrue(html.contains("data-create-album-href=\"/rhz-we/editor/album/create?categoryId=music\""), html);
        assertTrue(html.contains("data-edit-href=\"/rhz-we/editor/category/edit?id=music\""), html);
        assertTrue(html.contains("data-edit-href=\"/rhz-we/editor/edit?name=host\""), html);
        assertTrue(html.contains("data-action=\"delete\""), html);
        assertTrue(html.contains("data-action=\"download\""), html);
        assertTrue(html.contains("data-action=\"open\""), html);
        assertTrue(html.contains("data-action=\"activate\""), html);
        assertTrue(html.contains("id=\"catalog-delete-form\""), html);
        assertTrue(html.contains("id=\"catalog-batch-delete-form\""), html);
        assertTrue(html.contains("/editor/batch-delete"), html);
        assertTrue(html.contains("id=\"resourceMultiSelect\""), html);
        assertTrue(html.contains(">Multi-select</button>"), html);
        assertTrue(html.contains("id=\"catalog-activate-form\""), html);
        assertTrue(html.contains("id=\"resource-category-delete-form\""), html);
        assertTrue(html.contains("/editor/category/delete"), html);
        assertTrue(html.contains("data-can-delete-category=\"true\""), html);
        assertTrue(html.contains("data-embed-tag=\"cse-category\""), html);
        assertTrue(html.contains("data-embed-id=\"music\""), html);
        assertTrue(html.contains("data-embed-tag=\"cse-page\""), html);
        assertTrue(html.contains("data-embed-id=\"host\""), html);
        assertTrue(html.contains("data-embed-tag=\"cse-image\""), html);
        assertTrue(html.contains("data-embed-id=\"file1\""), html);
        assertTrue(html.contains("data-category-id=\"music\""), html);
        assertTrue(html.contains("data-kind=\"category\""), html);
        assertTrue(html.contains("data-kind=\"group\""), html);
        assertTrue(html.contains("data-kind=\"resource\""), html);
        assertTrue(html.contains("data-kind=\"page\""), html);
        assertTrue(html.contains("data-id=\"host\""), html);
        assertTrue(html.contains("/editor/resources/download"), html);
        assertTrue(html.contains("data-create-href=\"/rhz-we/editor/category/create\""), html);
        assertTrue(html.contains("data-action=\"rename\""), html);
        assertTrue(html.contains(">Rename</button>"), html);
        assertTrue(html.contains("/editor/catalog/rename"), html);
        assertTrue(html.contains("id=\"treeRenameKind\""), html);
        assertTrue(html.contains("data-can-rename=\"true\""), html);
        assertTrue(html.contains("id=\"resource-assign-form\""), html);
        assertTrue(html.contains("/editor/resources/group/assign"), html);
        assertFalse(html.contains("thumb-group"), html);
        assertTrue(html.contains("id=\"resource-group-move-form\""), html);
        assertTrue(html.contains("data-can-drag=\"true\""), html);
        assertTrue(html.matches("(?s).*resource-tree-link\".*?draggable=\"true\".*"), html);
        assertTrue(html.contains("resource-folder-link"), html);
        assertTrue(html.contains("draggable=\"true\""), html);
        int originAt = html.indexOf("resource-tree-origin");
        int contentAt = html.indexOf("id=\"group-content\"");
        int travelAt = html.indexOf("id=\"group-travel\"");
        int childAt = html.indexOf("id=\"group-y2024\"");
        assertTrue(originAt > 0 && contentAt > originAt && travelAt > contentAt && childAt > travelAt, html);

        context.setVariable("cseContextPath", "/");
        String rootHtml = engine.process("admin/editor-resources", context);
        assertTrue(rootHtml.contains("href=\"/editor/resources?group=content-apps\""), rootHtml);
        assertTrue(rootHtml.contains("href=\"/editor/resources?group=content\""), rootHtml);
        assertTrue(rootHtml.contains("data-create-href=\"/editor/category/create\""), rootHtml);
        assertTrue(rootHtml.contains("data-create-page-href=\"/editor/create?categoryId=music\""), rootHtml);
        assertTrue(rootHtml.contains("data-action=\"new-page\""), rootHtml);
        assertTrue(rootHtml.contains("data-create-album-href=\"/editor/album/create?categoryId=music\""), rootHtml);
        assertTrue(rootHtml.contains("data-action=\"new-album\""), rootHtml);
        assertFalse(rootHtml.contains("data-create-album-href=\"//editor/"), rootHtml);
        assertTrue(rootHtml.contains("data-edit-href=\"/editor/category/edit?id=music\""), rootHtml);
        assertTrue(rootHtml.contains("data-edit-href=\"/editor/edit?name=host\""), rootHtml);
        assertFalse(rootHtml.contains("href=\"//editor/"), rootHtml);
        assertFalse(rootHtml.contains("data-create-href=\"//editor/"), rootHtml);
        assertFalse(rootHtml.contains("data-create-page-href=\"//editor/"), rootHtml);
        assertFalse(rootHtml.contains("data-edit-href=\"//editor/"), rootHtml);

        context.setVariable("cseContextPath", "/rhz-we");
        context.setVariable("selectedGroup", music);
        PageData hostData = new PageData();
        hostData.setTitle("Host (2015)");
        ItemEntity host = new ItemEntity("host", hostData, null);
        host.setId(new ObjectId());
        context.setVariable("panePages", List.of(host));
        String categoryHtml = engine.process("admin/editor-resources", context);
        assertTrue(categoryHtml.contains("page-row"), categoryHtml);
        assertTrue(categoryHtml.contains("data-kind=\"page\""), categoryHtml);
        assertTrue(categoryHtml.contains("data-can-rename=\"true\""), categoryHtml);
        assertTrue(categoryHtml.contains(">PAGE<"), categoryHtml);
        assertTrue(categoryHtml.contains(">Host (2015)<"), categoryHtml);
        assertTrue(categoryHtml.contains("/rhz-we/editor/edit?name=host"), categoryHtml);
        assertTrue(categoryHtml.contains("resource-plus-tile"), categoryHtml);
        assertTrue(categoryHtml.contains("/rhz-we/editor/create?categoryId=music"), categoryHtml);
        assertTrue(categoryHtml.contains("New page"), categoryHtml);
        assertTrue(categoryHtml.contains("name=\"returnGroup\""), categoryHtml);
        assertTrue(categoryHtml.contains("value=\"category-music\""), categoryHtml);
        assertTrue(categoryHtml.contains("/editor/delete"), categoryHtml);
        assertFalse(categoryHtml.contains("resource-page-tile"), categoryHtml);
        assertFalse(categoryHtml.contains("No editable pages found"), categoryHtml);

        AppDisplayDTO app = new AppDisplayDTO();
        app.setSlug("fretlab");
        app.setName("Fretboard Lab");
        app.setBundled(true);
        app.setPublicUrl("/apps/fretlab/");
        context.setVariable("selectedGroup", apps);
        context.setVariable("panePages", null);
        context.setVariable("paneApps", List.of(app));
        String appsHtml = engine.process("admin/editor-resources", context);
        assertTrue(appsHtml.contains("app-row"), appsHtml);
        assertTrue(appsHtml.contains("data-kind=\"app\""), appsHtml);
        assertTrue(appsHtml.contains("data-open-href="), appsHtml);
        assertTrue(appsHtml.contains("resource-plus-tile"), appsHtml);
        assertTrue(appsHtml.contains("/editor/apps/upload"), appsHtml);
        app.setProductLogo(true);
        app.setSizeInBytes(4096);
        context.setVariable("paneApps", List.of(app));
        String appsLogoHtml = engine.process("admin/editor-resources", context);
        assertTrue(appsLogoHtml.contains("/apps/fretlab/product-logo.jpg"), appsLogoHtml);
        assertTrue(appsLogoHtml.contains("v=4096"), appsLogoHtml);
        assertTrue(appsHtml.contains("accept=\".cseapp\""), appsHtml);
        assertTrue(appsHtml.contains(">WAR<"), appsHtml);
        assertTrue(appsHtml.contains(">Fretboard Lab<"), appsHtml);
        assertFalse(appsHtml.contains("resource-page-tile"), appsHtml);
        app.setBundled(false);
        context.setVariable("paneApps", List.of(app));
        String installedHtml = engine.process("admin/editor-resources", context);
        assertFalse(installedHtml.contains("/editor/apps/store"), installedHtml);
        app.setStoreRequested(true);
        context.setVariable("paneApps", List.of(app));
        String storeHtml = engine.process("admin/editor-resources", context);
        assertTrue(storeHtml.contains("/editor/apps/store"), storeHtml);
        assertTrue(storeHtml.contains("Allow data store"), storeHtml);
        assertTrue(storeHtml.contains("Save storage"), storeHtml);
        assertTrue(storeHtml.contains("Document max (KB)"), storeHtml);

        ThemeDisplayDTO theme = new ThemeDisplayDTO();
        theme.setThemeId("modern");
        theme.setTitle("modern");
        theme.setShell("modern");
        theme.setBuiltin(true);
        theme.setActive(true);
        theme.setHasPreview(true);
        theme.setSizeInBytes(2048);
        theme.setDefaultSchema("linen");
        theme.setSchemas(List.of("linen"));
        ThemeDisplayDTO jsTheme = new ThemeDisplayDTO();
        jsTheme.setThemeId("client");
        jsTheme.setTitle("client");
        jsTheme.setShell("js");
        jsTheme.setBuiltin(false);
        jsTheme.setActive(false);
        jsTheme.setDefaultSchema("studio");
        jsTheme.setSchemas(List.of("studio"));
        context.setVariable("selectedGroup", group("content-themes", "Themes", "Content / Themes", 2));
        context.setVariable("paneApps", null);
        context.setVariable("paneThemes", List.of(theme, jsTheme));
        String themesHtml = engine.process("admin/editor-resources", context);
        assertTrue(themesHtml.contains("data-theme-id=\"modern\""), themesHtml);
        assertTrue(themesHtml.contains("data-kind=\"theme\""), themesHtml);
        assertTrue(themesHtml.contains("data-can-activate=\"true\""), themesHtml);
        assertTrue(themesHtml.contains("id=\"themeGrid\""), themesHtml);
        assertTrue(themesHtml.contains("/editor/themes/reorder"), themesHtml);
        assertTrue(themesHtml.contains("resource-plus-tile"), themesHtml);
        assertTrue(themesHtml.contains("/editor/themes/upload"), themesHtml);
        assertTrue(themesHtml.contains("accept=\".csetheme\""), themesHtml);
        assertTrue(themesHtml.contains(">thymeleaf<"), themesHtml);
        assertTrue(themesHtml.contains(">js<"), themesHtml);
        assertTrue(themesHtml.contains(">ON<"), themesHtml);
        assertTrue(themesHtml.contains("name=\"returnGroup\""), themesHtml);
        assertTrue(themesHtml.contains("/content-public/themes/modern/preview.jpg"), themesHtml);
        assertTrue(themesHtml.contains("v=2048"), themesHtml);

        CategoryEntity shots = new CategoryEntity(new CategoryData("shots", "Shots", "", true, true), null);
        shots.setId(new ObjectId());
        context.setVariable("selectedGroup", categories);
        context.setVariable("paneThemes", null);
        context.setVariable("paneCategories", List.of(shots));
        String catsHtml = engine.process("admin/editor-resources", context);
        assertTrue(catsHtml.contains("category-row"), catsHtml);
        assertTrue(catsHtml.contains("data-kind=\"category\""), catsHtml);
        assertTrue(catsHtml.contains("data-can-rename=\"true\""), catsHtml);
        assertTrue(catsHtml.contains("resource-plus-tile"), catsHtml);
        assertTrue(catsHtml.contains("/editor/category/create"), catsHtml);
        assertTrue(catsHtml.contains(">New category<"), catsHtml);
        assertTrue(catsHtml.contains(">shots<"), catsHtml);
        assertTrue(catsHtml.contains(">CAT<"), catsHtml);
        assertTrue(catsHtml.contains("Nav title"), catsHtml);
        assertTrue(catsHtml.contains("thumb-delete-btn"), catsHtml);
        assertTrue(catsHtml.contains("Delete this category and all its pages?"), catsHtml);
        assertTrue(catsHtml.contains("name=\"returnGroup\""), catsHtml);
        assertFalse(catsHtml.contains("entry-list"), catsHtml);
        assertFalse(catsHtml.contains("resource-folder-tile"), catsHtml);

        PlaylistData playlistData = new PlaylistData();
        playlistData.setTitle("Ocean Blue");
        PlaylistEntity playlist = new PlaylistEntity("ocean-blue", playlistData, null);
        playlist.setId(new ObjectId());
        context.setVariable("selectedGroup", group("content-playlists", "Playlists", "Content / Playlists", 2));
        context.setVariable("paneCategories", null);
        context.setVariable("panePlaylists", List.of(playlist));
        String playlistsHtml = engine.process("admin/editor-resources", context);
        assertTrue(playlistsHtml.contains("playlist-row"), playlistsHtml);
        assertTrue(playlistsHtml.contains("data-kind=\"playlist\""), playlistsHtml);
        assertTrue(playlistsHtml.contains("data-can-rename=\"true\""), playlistsHtml);
        assertTrue(playlistsHtml.contains("resource-plus-tile"), playlistsHtml);
        assertTrue(playlistsHtml.contains("/editor/playlist/create"), playlistsHtml);
        assertTrue(playlistsHtml.contains(">New playlist<"), playlistsHtml);
        assertTrue(playlistsHtml.contains(">Ocean Blue<"), playlistsHtml);
        assertTrue(playlistsHtml.contains("cse-playlist"), playlistsHtml);
        assertTrue(playlistsHtml.contains(">PLAYLIST<"), playlistsHtml);
        assertTrue(playlistsHtml.contains("thumb-delete-btn"), playlistsHtml);
        assertFalse(playlistsHtml.contains("entry-list"), playlistsHtml);

        UrlTemplateData templateData = new UrlTemplateData();
        templateData.setUrlPattern("https://youtube.com/%s");
        templateData.setUrlDefaultText("Find more videos on my YouTube channel: %s");
        UrlTemplateEntity template = new UrlTemplateEntity("youtube", templateData, null);
        template.setId(new ObjectId());
        context.setVariable("selectedGroup", group("content-url-templates", "URL Templates",
                "Content / URL Templates", 2));
        context.setVariable("panePlaylists", null);
        context.setVariable("paneUrlTemplates", List.of(template));
        String templatesHtml = engine.process("admin/editor-resources", context);
        assertTrue(templatesHtml.contains("url-template-row"), templatesHtml);
        assertTrue(templatesHtml.contains("data-kind=\"url-template\""), templatesHtml);
        assertTrue(templatesHtml.contains("data-can-rename=\"true\""), templatesHtml);
        assertTrue(templatesHtml.contains("resource-plus-tile"), templatesHtml);
        assertTrue(templatesHtml.contains("/editor/url-template/create"), templatesHtml);
        assertTrue(templatesHtml.contains(">New URL template<"), templatesHtml);
        assertTrue(templatesHtml.contains(">youtube<"), templatesHtml);
        assertTrue(templatesHtml.contains("cse-url"), templatesHtml);
        assertTrue(templatesHtml.contains(">URL<"), templatesHtml);
        assertTrue(templatesHtml.contains("thumb-delete-btn"), templatesHtml);
        assertFalse(templatesHtml.contains("entry-list"), templatesHtml);
    }

    @Test
    void albumCreateSelectShowsPathLabels() {
        ResourceGroupDisplayDTO nested = group("y2024", "2024", "Travel / 2024", 2);
        SpringTemplateEngine engine = engine();
        MockServletContext servletContext = new MockServletContext();
        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(servletContext);
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setContextPath("/rhz-we");
        request.setRequestURI("/rhz-we/editor/album/create");
        MockHttpServletResponse response = new MockHttpServletResponse();
        WebContext context = new WebContext(application.buildExchange(request, response));
        ResourceGroupDisplayDTO categories = group("content-categories", "Categories", "Content / Categories", 2);
        categories.setVirtual(true);
        categories.setHref("/editor/resources?group=content-categories");
        context.setVariable("resourceGroups", List.of(nested));
        context.setVariable("resourceGroupTree", List.of(categories));
        context.setVariable("selectedGroup", categories);
        context.setVariable("selectedLeafId", null);
        context.setVariable("defaultGroupId", "");
        context.setVariable("categories", List.of());
        context.setVariable("cseContextPath", "/rhz-we");
        context.setVariable("username", "owner");
        context.setVariable("navApi", false);
        context.setVariable("navResources", true);
        context.setVariable("navApps", false);
        context.setVariable("navThemes", false);
        context.setVariable("navCategoriesActive", false);
        context.setVariable("navPages", false);
        context.setVariable("navPlaylists", false);
        context.setVariable("navAccounts", false);
        context.setVariable("navRoles", false);
        context.setVariable("navSettings", false);
        context.setVariable("navSiteData", false);
        context.setVariable("navLogs", false);

        String html = engine.process("admin/editor-album-create", context);

        assertFalse(html.contains("${"), html);
        assertTrue(html.contains("resource-tree"), html);
        assertTrue(html.contains("resource-pane"), html);
        assertTrue(html.contains(">New album<"), html);
        assertTrue(html.contains(">Travel / 2024<"), html);
        assertTrue(html.contains("href=\"/rhz-we/editor/resources?group=content-categories\""), html);
        assertFalse(html.contains("/editor/pages"), html);
        assertFalse(html.contains("page-header"), html);
        assertFalse(html.contains("resourceGroupData"), html);
    }

    @Test
    void pageAndCategoryEditSitInCatalogPane() {
        ResourceGroupDisplayDTO categories = group("content-categories", "Categories", "Content / Categories", 2);
        categories.setVirtual(true);
        categories.setHref("/editor/resources?group=content-categories");
        ResourceGroupDisplayDTO music = group("category-music", "music", "Content / Categories / music", 3);
        music.setHref("/editor/resources?group=category-music");
        music.setTreeFiles(List.of(
                new ResourceTreeFile("page-host", "Host (2015)", "/editor/edit?name=host",
                        ResourceTreeFile.Mark.PAGE).withKey("host")));
        categories.getChildren().add(music);

        PageData hostData = new PageData();
        hostData.setTitle("Host (2015)");
        ItemEntity host = new ItemEntity("host", hostData, null);
        CategoryData catData = new CategoryData("music", "music", "everything related to music", true, true);
        CategoryEntity category = new CategoryEntity(catData, null);
        category.setId(new ObjectId());

        SpringTemplateEngine engine = engine();
        MockServletContext servletContext = new MockServletContext();
        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(servletContext);
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setContextPath("/rhz-we");
        request.setRequestURI("/rhz-we/editor/edit");
        MockHttpServletResponse response = new MockHttpServletResponse();
        WebContext context = new WebContext(application.buildExchange(request, response));
        context.setVariable("resourceGroupTree", List.of(categories));
        context.setVariable("selectedGroup", music);
        context.setVariable("selectedLeafId", "page-host");
        context.setVariable("defaultGroupId", "");
        context.setVariable("cseContextPath", "/rhz-we");
        context.setVariable("username", "owner");
        context.setVariable("item", host);
        context.setVariable("categories", List.of(category));
        context.setVariable("images", List.of());
        context.setVariable("navResources", true);
        context.setVariable("navPages", false);
        context.setVariable("navCategoriesActive", false);
        context.setVariable("navApi", false);
        context.setVariable("navApps", false);
        context.setVariable("navThemes", false);
        context.setVariable("navPlaylists", false);
        context.setVariable("navAccounts", false);
        context.setVariable("navRoles", false);
        context.setVariable("navSettings", false);
        context.setVariable("navSiteData", false);
        context.setVariable("navLogs", false);

        String pageHtml = engine.process("admin/editor-page-edit", context);
        assertFalse(pageHtml.contains("${"), pageHtml);
        assertTrue(pageHtml.contains("resource-tree"), pageHtml);
        assertTrue(pageHtml.contains("resource-pane"), pageHtml);
        assertTrue(pageHtml.contains("name=\"title\""), pageHtml);
        assertTrue(pageHtml.contains("resource-tree-file is-selected")
                || pageHtml.contains("resource-tree-file is-selected\""), pageHtml);
        assertTrue(pageHtml.contains("href=\"/rhz-we/editor/resources?group=category-music\""), pageHtml);
        assertFalse(pageHtml.contains("page-header"), pageHtml);
        assertTrue(pageHtml.contains("cse-embed-drop"), pageHtml);
        assertTrue(pageHtml.contains("cse-page"), pageHtml);

        context.setVariable("selectedLeafId", null);
        context.setVariable("category", category);
        String categoryHtml = engine.process("admin/editor-category-edit", context);
        assertFalse(categoryHtml.contains("${"), categoryHtml);
        assertTrue(categoryHtml.contains("resource-tree"), categoryHtml);
        assertTrue(categoryHtml.contains("resource-pane"), categoryHtml);
        assertTrue(categoryHtml.contains("name=\"itemName\""), categoryHtml);
        assertTrue(categoryHtml.contains("name=\"displayCount\""), categoryHtml);

        context.setVariable("selectedCategoryId", category.getId().toString());
        String createHtml = engine.process("admin/editor-page-create", context);
        assertFalse(createHtml.contains("${"), createHtml);
        assertTrue(createHtml.contains("resource-tree"), createHtml);
        assertTrue(createHtml.contains("resource-pane"), createHtml);
        assertTrue(createHtml.contains("name=\"name\""), createHtml);
        assertTrue(createHtml.contains(">New page<"), createHtml);
        assertTrue(createHtml.contains("Create page"), createHtml);
        assertTrue(createHtml.contains("cse-embed-drop"), createHtml);

        context.setVariable("resourceGroups", List.of());
        String albumHtml = engine.process("admin/editor-album-create", context);
        assertFalse(albumHtml.contains("${"), albumHtml);
        assertTrue(albumHtml.contains("resource-tree"), albumHtml);
        assertTrue(albumHtml.contains("resource-pane"), albumHtml);
        assertTrue(albumHtml.contains(">New album<"), albumHtml);
        assertTrue(albumHtml.contains("Create album"), albumHtml);
        assertTrue(albumHtml.contains("name=\"resourceGroupId\""), albumHtml);

        context.setVariable("selectedGroup", categories);
        String categoryCreateHtml = engine.process("admin/editor-category-create", context);
        assertFalse(categoryCreateHtml.contains("${"), categoryCreateHtml);
        assertTrue(categoryCreateHtml.contains("resource-tree"), categoryCreateHtml);
        assertTrue(categoryCreateHtml.contains("resource-pane"), categoryCreateHtml);
        assertTrue(categoryCreateHtml.contains(">New category<"), categoryCreateHtml);
        assertTrue(categoryCreateHtml.contains("Create category"), categoryCreateHtml);
        assertFalse(categoryCreateHtml.contains("page-header"), categoryCreateHtml);
        assertTrue(categoryCreateHtml.contains("href=\"/rhz-we/editor/resources?group=content-categories\""),
                categoryCreateHtml);

        ResourceGroupDisplayDTO playlists = group("content-playlists", "Playlists", "Content / Playlists", 2);
        playlists.setVirtual(true);
        playlists.setHref("/editor/resources?group=content-playlists");
        PlaylistData ocean = new PlaylistData();
        ocean.setTitle("Ocean Blue");
        PlaylistEntity playlist = new PlaylistEntity("ocean-blue", ocean, null);
        playlist.setId(new ObjectId());
        playlists.setTreeFiles(List.of(new ResourceTreeFile(
                "playlist-" + playlist.getId(), "Ocean Blue",
                "/editor/playlist/edit?id=" + playlist.getId(),
                ResourceTreeFile.Mark.PLAYLIST).withKey(playlist.getId().toString())
                        .withEmbedId("ocean-blue")));
        context.setVariable("selectedGroup", playlists);
        context.setVariable("resourceGroupTree", List.of(playlists));
        context.setVariable("audioLibrary", List.of());
        context.setVariable("selectedTracks", List.of());
        context.setVariable("selectedIds", List.of());
        context.setVariable("playlist", null);
        String playlistCreateHtml = engine.process("admin/editor-playlist-create", context);
        assertFalse(playlistCreateHtml.contains("${"), playlistCreateHtml);
        assertTrue(playlistCreateHtml.contains("resource-tree"), playlistCreateHtml);
        assertTrue(playlistCreateHtml.contains("resource-pane"), playlistCreateHtml);
        assertTrue(playlistCreateHtml.contains(">New playlist<"), playlistCreateHtml);
        assertTrue(playlistCreateHtml.contains("Create playlist"), playlistCreateHtml);
        assertFalse(playlistCreateHtml.contains("page-header"), playlistCreateHtml);
        assertTrue(playlistCreateHtml.contains("href=\"/rhz-we/editor/resources?group=content-playlists\""),
                playlistCreateHtml);
        assertTrue(playlistCreateHtml.contains("data-embed-tag=\"cse-playlist\""), playlistCreateHtml);
        assertTrue(playlistCreateHtml.contains("data-embed-id=\"ocean-blue\""), playlistCreateHtml);

        context.setVariable("playlist", playlist);
        context.setVariable("selectedLeafId", "playlist-" + playlist.getId());
        String playlistEditHtml = engine.process("admin/editor-playlist-edit", context);
        assertFalse(playlistEditHtml.contains("${"), playlistEditHtml);
        assertTrue(playlistEditHtml.contains("resource-tree"), playlistEditHtml);
        assertTrue(playlistEditHtml.contains("resource-pane"), playlistEditHtml);
        assertTrue(playlistEditHtml.contains(">Ocean Blue<"), playlistEditHtml);
        assertTrue(playlistEditHtml.contains("name=\"id\""), playlistEditHtml);
        assertFalse(playlistEditHtml.contains("page-header"), playlistEditHtml);
        assertTrue(playlistEditHtml.contains("resource-tree-file is-selected")
                || playlistEditHtml.contains("resource-tree-file is-selected\""), playlistEditHtml);

        ResourceGroupDisplayDTO urlTemplates = group("content-url-templates", "URL Templates",
                "Content / URL Templates", 2);
        urlTemplates.setVirtual(true);
        urlTemplates.setHref("/editor/resources?group=content-url-templates");
        UrlTemplateData youtubeData = new UrlTemplateData();
        youtubeData.setUrlPattern("https://youtube.com/%s");
        youtubeData.setUrlDefaultText("Find more videos on my YouTube channel: %s");
        UrlTemplateEntity youtube = new UrlTemplateEntity("youtube", youtubeData, null);
        youtube.setId(new ObjectId());
        urlTemplates.setTreeFiles(List.of(new ResourceTreeFile(
                "url-template-" + youtube.getId(), "youtube",
                "/editor/url-template/edit?id=" + youtube.getId(),
                ResourceTreeFile.Mark.URL_TEMPLATE).withKey(youtube.getId().toString())
                        .withEmbedId("youtube")));
        context.setVariable("selectedGroup", urlTemplates);
        context.setVariable("resourceGroupTree", List.of(urlTemplates));
        context.setVariable("template", null);
        context.setVariable("selectedLeafId", null);
        String urlCreateHtml = engine.process("admin/editor-url-template-create", context);
        assertFalse(urlCreateHtml.contains("${"), urlCreateHtml);
        assertTrue(urlCreateHtml.contains("resource-tree"), urlCreateHtml);
        assertTrue(urlCreateHtml.contains("resource-pane"), urlCreateHtml);
        assertTrue(urlCreateHtml.contains(">New URL template<"), urlCreateHtml);
        assertTrue(urlCreateHtml.contains("Create URL template"), urlCreateHtml);
        assertTrue(urlCreateHtml.contains("href=\"/rhz-we/editor/resources?group=content-url-templates\""),
                urlCreateHtml);
        assertTrue(urlCreateHtml.contains("data-embed-tag=\"cse-url\""), urlCreateHtml);
        assertTrue(urlCreateHtml.contains("data-embed-id=\"youtube\""), urlCreateHtml);

        context.setVariable("template", youtube);
        context.setVariable("selectedLeafId", "url-template-" + youtube.getId());
        String urlEditHtml = engine.process("admin/editor-url-template-edit", context);
        assertFalse(urlEditHtml.contains("${"), urlEditHtml);
        assertTrue(urlEditHtml.contains("resource-tree"), urlEditHtml);
        assertTrue(urlEditHtml.contains("resource-pane"), urlEditHtml);
        assertTrue(urlEditHtml.contains(">youtube<"), urlEditHtml);
        assertTrue(urlEditHtml.contains("name=\"id\""), urlEditHtml);
        assertTrue(urlEditHtml.contains("resource-tree-file is-selected")
                || urlEditHtml.contains("resource-tree-file is-selected\""), urlEditHtml);
    }

    @Test
    void catalogEmbedsAccessOnFileTipNotVirtualPane() {
        RoleEntity member = new RoleEntity();
        member.setId(new ObjectId());
        member.setSlug("member");
        member.setName("Member");

        ResourceGroupDisplayDTO content = group("content", "Content", "Content", 1);
        content.setVirtual(true);
        content.setHref("/editor/resources?group=content");
        ResourceGroupDisplayDTO travel = group("travel", "Travel", "Travel", 1);
        travel.setHref("/editor/resources?group=travel");
        travel.setGuestDenied(true);
        ResourceGroupDisplayDTO y2024 = group("y2024", "2024", "Travel / 2024", 2);
        y2024.setParentId("travel");
        y2024.setAccessCanEdit(true);
        ResourceGroupDisplayDTO iceland = group("iceland", "Iceland", "Travel / 2024 / Iceland", 3);
        iceland.setParentId("y2024");
        iceland.setGuestDenied(true);
        y2024.getChildren().add(iceland);
        travel.getChildren().add(y2024);

        ResourceData data = new ResourceData();
        data.setPathPublic("/u/res/image/aurora.jpg");
        data.setType(ResourceType.IMAGE);
        ResourceEntity file = new ResourceEntity(data, null);
        file.setId(new ObjectId());
        file.setSecurityData(new SecurityData(EntityAccessConstants.forLevel(SecurityLevel.OPERATOR)));
        y2024.setResources(List.of(file));

        SpringTemplateEngine engine = engine();
        MockServletContext servletContext = new MockServletContext();
        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(servletContext);
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setContextPath("/rhz-we");
        request.setRequestURI("/rhz-we/editor/resources");
        MockHttpServletResponse response = new MockHttpServletResponse();
        WebContext context = new WebContext(application.buildExchange(request, response));
        context.setVariable("resourceGroupTree", List.of(content, travel));
        context.setVariable("assignableGroups", List.of(travel, y2024));
        context.setVariable("selectedGroup", y2024);
        context.setVariable("selectedLeafId", null);
        context.setVariable("defaultGroupId", "default-id");
        context.setVariable("cseContextPath", "/rhz-we");
        context.setVariable("username", "owner");
        context.setVariable("accessRoles", List.of(member));
        context.setVariable("accessAccounts", List.of());
        context.setVariable("navResources", true);

        String html = engine.process("admin/editor-resources", context);
        assertFalse(html.contains("${"), html);
        assertTrue(html.contains("name=\"accessPosted\""), html);
        assertTrue(html.contains("access-disclosure"), html);
        assertTrue(html.contains("access-summary"), html);
        assertFalse(html.contains("access-disclosure\" open"), html);
        assertTrue(html.contains("access-tabs"), html);
        assertTrue(html.contains("access-check-dropdown"), html);
        assertTrue(html.contains("access-check-summary"), html);
        assertTrue(html.contains("data-access-tab=\"read\""), html);
        assertTrue(html.contains("data-access-tab=\"edit\""), html);
        assertTrue(html.contains("data-access-tab=\"delete\""), html);
        assertTrue(html.contains("data-access-panel=\"read\""), html);
        assertTrue(html.contains("/editor/resources/access"), html);
        assertTrue(html.contains("/editor/resources/group/access"), html);
        assertTrue(html.contains("Not readable by Guest"), html);
        assertTrue(html.contains("tip-access-form"), html);
        assertTrue(html.contains("Save access"), html);

        context.setVariable("selectedGroup", content);
        String contentHtml = engine.process("admin/editor-resources", context);
        assertFalse(contentHtml.contains("name=\"accessPosted\""), contentHtml);
        assertFalse(contentHtml.contains("/editor/resources/group/access"), contentHtml);
        assertFalse(contentHtml.contains("tip-access-form"), contentHtml);
    }

    @Test
    void processingVideoShowsPercentBarWithoutReload() {
        ResourceGroupDisplayDTO unsorted = group("default-id", "Unsorted", "Unsorted", 1);
        unsorted.setDefaultGroup(true);
        unsorted.setHref("/editor/resources?group=default-id");
        ResourceData data = new ResourceData();
        data.setType(ResourceType.VIDEO);
        data.setPathPublic("/u/res/video/clip.mp4");
        data.setSizeInBytes(123000000);
        VideoStatus.set(data, VideoStatus.PROCESSING);
        ResourceEntity video = new ResourceEntity(data, null);
        video.setId(new ObjectId());
        unsorted.setResources(List.of(video));

        SpringTemplateEngine engine = engine();
        MockServletContext servletContext = new MockServletContext();
        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(servletContext);
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setContextPath("/rhz-we");
        request.setRequestURI("/rhz-we/editor/resources");
        MockHttpServletResponse response = new MockHttpServletResponse();
        WebContext context = new WebContext(application.buildExchange(request, response));
        context.setVariable("resourceGroupTree", List.of(unsorted));
        context.setVariable("assignableGroups", List.of(unsorted));
        context.setVariable("selectedGroup", unsorted);
        context.setVariable("defaultGroupId", "default-id");
        context.setVariable("cseContextPath", "/rhz-we");
        context.setVariable("username", "owner");
        context.setVariable("navResources", true);

        String html = engine.process("admin/editor-resources", context);
        assertTrue(html.contains("data-video-status=\"processing\""), html);
        assertTrue(html.contains("thumb-progress"), html);
        assertTrue(html.contains("data-video-percent"), html);
        assertTrue(html.contains(">0%<"), html);
        assertTrue(html.contains("/rhz-we/editor/video/progress"), html);
        assertFalse(html.contains(">Processing<"), html);
        assertFalse(html.contains("if (document.querySelector('.thumb-tag-processing'))"), html);
    }

    private static ResourceGroupDisplayDTO group(String id, String name, String path, int depth) {
        ResourceGroupDisplayDTO dto = new ResourceGroupDisplayDTO();
        dto.setId(id);
        dto.setHumanReadableId(name);
        dto.setPathLabel(path);
        dto.setDepth(depth);
        return dto;
    }

    private static SpringTemplateEngine engine() {
        Path src = adminSrc();
        FileTemplateResolver pages = new FileTemplateResolver();
        pages.setPrefix(src.toString() + "/");
        pages.setSuffix(".html");
        pages.setTemplateMode(TemplateMode.HTML);
        pages.setCharacterEncoding("UTF-8");
        pages.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(pages);
        engine.setEnableSpringELCompiler(true);
        return engine;
    }

    private static Path adminSrc() {
        Path fromCse = Path.of("..", "app-admin", "src").toAbsolutePath().normalize();
        if (Files.isRegularFile(fromCse.resolve("admin").resolve("editor-resources.html"))) {
            return fromCse;
        }
        Path fromRoot = Path.of("app-admin", "src").toAbsolutePath().normalize();
        if (Files.isRegularFile(fromRoot.resolve("admin").resolve("editor-resources.html"))) {
            return fromRoot;
        }
        throw new IllegalStateException("app-admin/src/admin/editor-resources.html not found");
    }
}

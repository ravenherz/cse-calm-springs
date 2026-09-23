package com.ravenherz.cse.present;

import com.ravenherz.cse.dal.EntityId;

import com.ravenherz.cse.dal.dto.CategoryEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.PlaylistEntity;
import com.ravenherz.cse.dal.dto.UrlTemplateEntity;
import com.ravenherz.cse.dal.dto.ResourceGroupEntity;
import com.ravenherz.cse.dal.dto.basic.AlbumData;
import com.ravenherz.cse.dal.dto.basic.CategoryData;
import com.ravenherz.cse.dal.dto.basic.PageData;
import com.ravenherz.cse.dal.dto.basic.ResourceGroupData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorTreeTest {

    @Test
    void contentSitsFirstWithLockedVirtualChildren() {
        ResourceGroupEntity unsorted = group("Unsorted");
        ResourceGroupEntity music = group("music");
        ResourceGroupTreeView.Assembled resources = ResourceGroupTreeView.assembleStats(
                List.of(unsorted, music), List.of());
        CategoryEntity shots = category("Shots", 2);
        ItemEntity inShots = page("aurora", "Aurora", shots.getId());
        ItemEntity loose = album("home", "Home", null);

        List<ResourceGroupDisplayDTO> roots = EditorTree.attach(
                resources, List.of(shots), List.of(inShots, loose));

        assertEquals(List.of("Content", "Unsorted", "music"),
                roots.stream().map(ResourceGroupDisplayDTO::getHumanReadableId).toList());
        ResourceGroupDisplayDTO content = roots.get(0);
        assertTrue(content.isVirtual());
        assertTrue(content.isLocked());
        assertFalse(content.canCreateChild());
        assertEquals(EditorTree.CONTENT_HREF, content.getHref());
        assertEquals(List.of("Apps", "Categories", "Playlists", "Themes", "URL Templates"),
                content.getChildren().stream().map(ResourceGroupDisplayDTO::getHumanReadableId).toList());
        for (ResourceGroupDisplayDTO child : content.getChildren()) {
            assertTrue(child.isVirtual());
            assertTrue(child.isLocked());
            assertFalse(child.canCreateChild());
            assertFalse(child.canAcceptFiles());
        }
        ResourceGroupDisplayDTO unsortedDto = roots.get(1);
        assertTrue(unsortedDto.isDefaultGroup());
        assertFalse(unsortedDto.canCreateChild());
        assertTrue(unsortedDto.canAcceptFiles());
        assertEquals("/editor/resources?group=" + unsorted.getId(), unsortedDto.getHref());

        ResourceGroupDisplayDTO cats = content.getChildren().get(1);
        assertTrue(cats.canCreateCategory());
        assertTrue(cats.canCreatePageMenu());
        assertFalse(content.canCreateCategory());
        assertFalse(content.canCreatePageMenu());
        assertEquals(1, cats.getChildren().size());
        ResourceGroupDisplayDTO shotsNode = cats.getChildren().get(0);
        assertTrue(shotsNode.isCategoryNode());
        assertTrue(shotsNode.canDeleteCategory());
        assertTrue(shotsNode.canEditCategory());
        assertEquals("/editor/category/edit?id=" + shots.getId(), shotsNode.categoryEditPath());
        assertEquals(shots.getId().toString(), shotsNode.categoryObjectId());
        assertEquals("shots", shotsNode.getCategoryItemName());
        assertEquals("cse-category", shotsNode.embedTag());
        assertEquals("shots", shotsNode.embedId());
        assertTrue(shotsNode.isEmbeddable());
        assertEquals("cse-page", shotsNode.getTreeFiles().get(0).embedTag());
        assertEquals("aurora", shotsNode.getTreeFiles().get(0).embedId());
        assertTrue(shotsNode.getTreeFiles().get(0).isEmbeddable());
        assertEquals("cse-page", cats.getTreeFiles().get(0).embedTag());
        assertEquals("home", cats.getTreeFiles().get(0).embedId());
        assertFalse(cats.canEditCategory());
        assertFalse(shotsNode.isLocked());
        assertFalse(shotsNode.canCreateChild());
        assertFalse(shotsNode.canCreateCategory());
        assertTrue(shotsNode.canCreatePage());
        assertFalse(shotsNode.canCreatePageMenu());
        assertEquals("/editor/create?categoryId=" + shots.getId(), shotsNode.pageCreatePath());
        assertEquals("/editor/album/create?categoryId=" + shots.getId(), shotsNode.albumCreatePath());
        assertFalse(cats.canCreatePage());
        assertFalse(cats.canDeleteCategory());
        assertFalse(content.canDeleteCategory());
        assertFalse(content.getChildren().get(0).canDeleteCategory());
        assertEquals("/editor/resources?group=category-" + shots.getId(), shotsNode.getHref());
        assertEquals("/editor/resources?group=content-categories", cats.getHref());
        assertEquals(1, shotsNode.getTreeFiles().size());
        assertEquals("Aurora", shotsNode.getTreeFiles().get(0).name());
        assertTrue(shotsNode.getTreeFiles().get(0).isPage());
        assertTrue(shotsNode.getTreeFiles().get(0).isContentPage());
        assertTrue(shotsNode.getTreeFiles().get(0).canEdit());
        assertFalse(shotsNode.getTreeFiles().get(0).isAlbum());
        assertEquals(1, cats.getTreeFiles().size());
        assertEquals("Home", cats.getTreeFiles().get(0).name());
        assertTrue(cats.getTreeFiles().get(0).isAlbum());
        assertTrue(cats.getTreeFiles().get(0).canEdit());
        assertFalse(cats.getTreeFiles().get(0).isContentPage());
        assertTrue(cats.getTreeFiles().get(0).href().contains("/editor/edit?name=home"));
        assertEquals("page-" + inShots.getId(), EditorTree.pageLeafId(inShots));
        assertEquals("page-" + inShots.getId(), shotsNode.getTreeFiles().get(0).id());
        assertEquals("page-" + loose.getId(), EditorTree.pageLeafId(loose));
        ItemEntity unnamed = page("draft", "Draft", null);
        unnamed.setId(null);
        assertEquals("page-draft", EditorTree.pageLeafId(unnamed));
        assertNull(EditorTree.pageLeafId(null));
        assertEquals("/editor/resources?group=content-apps", content.getChildren().get(0).getHref());
        assertEquals("/editor/resources?group=content-playlists", content.getChildren().get(2).getHref());
        assertEquals("/editor/resources?group=content-themes", content.getChildren().get(3).getHref());
        assertEquals("/editor/resources?group=content-url-templates", content.getChildren().get(4).getHref());
    }

    @Test
    void contentFoldersHoldAppPlaylistAndThemeLeaves() {
        ResourceGroupTreeView.Assembled resources = ResourceGroupTreeView.assembleStats(List.of(), List.of());
        List<ResourceGroupDisplayDTO> roots = EditorTree.attach(resources,
                EditorTree.contentBranch(List.of(), List.of(),
                        List.of(new ResourceTreeFile("app-fretlab", "Fretboard Lab", "/apps/fretlab/")),
                        List.of(new ResourceTreeFile("playlist-1", "Ocean Blue", "/editor/playlist/edit?id=1")),
                        List.of(new ResourceTreeFile("theme-modern", "modern", "/editor/themes"))));
        ResourceGroupDisplayDTO content = roots.get(0);
        assertEquals("Fretboard Lab", content.getChildren().get(0).getTreeFiles().get(0).name());
        assertEquals("Ocean Blue", content.getChildren().get(2).getTreeFiles().get(0).name());
        assertEquals("modern", content.getChildren().get(3).getTreeFiles().get(0).name());
        assertTrue(content.getChildren().get(0).hasExpandableChildren());
    }

    @Test
    void selectionFollowsEditorAliasesAndKeepsParentOnEdit() {
        assertEquals(EditorTree.CONTENT_ID,
                EditorTree.selectionId("/editor/content", null, null, null, null, null));
        assertEquals(EditorTree.APPS_ID,
                EditorTree.selectionId("/editor/apps", null, null, null, null, null));
        assertEquals(EditorTree.THEMES_ID,
                EditorTree.selectionId("/editor/themes", null, null, null, null, null));
        assertEquals(EditorTree.PLAYLISTS_ID,
                EditorTree.selectionId("/editor/playlists", null, null, null, null, null));
        assertEquals(EditorTree.PLAYLISTS_ID,
                EditorTree.selectionId("/editor/playlist/create", null, null, null, null, null));
        assertEquals(EditorTree.PLAYLISTS_ID,
                EditorTree.selectionId("/editor/playlist/edit", null, null, null, null, null));
        assertEquals(EditorTree.URL_TEMPLATES_ID,
                EditorTree.selectionId("/editor/url-templates", null, null, null, null, null));
        assertEquals(EditorTree.URL_TEMPLATES_ID,
                EditorTree.selectionId("/editor/url-template/create", null, null, null, null, null));
        assertEquals(EditorTree.URL_TEMPLATES_ID,
                EditorTree.selectionId("/editor/url-template/edit", null, null, null, null, null));
        assertEquals(EditorTree.CATEGORIES_ID,
                EditorTree.selectionId("/editor/categories", null, null, null, null, null));
        assertEquals(EditorTree.CATEGORIES_ID,
                EditorTree.selectionId("/editor/category/create", null, null, null, null, null));
        assertEquals(EditorTree.CATEGORIES_ID,
                EditorTree.selectionId("/editor/pages", null, null, null, null, null));
        assertEquals("category-abc",
                EditorTree.selectionId("/editor/pages", null, "abc", null, null, null));
        assertEquals("category-abc",
                EditorTree.selectionId("/editor/category/edit", null, null, null, "abc", null));
        assertEquals("category-abc",
                EditorTree.selectionId("/editor/edit", null, null, "aurora", null, "abc"));
        assertEquals(EditorTree.CATEGORIES_ID,
                EditorTree.selectionId("/editor/edit", null, null, "loose", null, null));
        assertEquals(EditorTree.CATEGORIES_ID,
                EditorTree.selectionId("/editor/create", null, null, null, null, null));
        assertEquals("category-abc",
                EditorTree.selectionId("/editor/create", null, "abc", null, null, null));
        assertEquals("/editor/create?categoryId=abc", EditorTree.pageCreatePath("abc"));
        assertEquals("/editor/create", EditorTree.pageCreatePath(null));
        assertEquals("/editor/album/create?categoryId=abc", EditorTree.albumCreatePath("abc"));
        assertEquals("/editor/album/create", EditorTree.albumCreatePath(null));
        assertEquals("category-abc",
                EditorTree.selectionId("/editor/album/create", null, "abc", null, null, null));
        assertEquals(EditorTree.CATEGORIES_ID,
                EditorTree.selectionId("/editor/album/create", null, null, null, null, null));
        assertEquals("travel",
                EditorTree.selectionId("/editor/resources", "travel", null, null, null, null));
        assertNull(EditorTree.selectionId("/editor/settings", null, null, null, null, null));
        PlaylistEntity playlist = new PlaylistEntity();
        playlist.setId(EntityId.generate());
        assertEquals(EditorTree.PLAYLIST_PREFIX + playlist.getId(), EditorTree.playlistLeafId(playlist));
        assertNull(EditorTree.playlistLeafId(null));
        UrlTemplateEntity urlTemplate = new UrlTemplateEntity();
        urlTemplate.setId(EntityId.generate());
        assertEquals(EditorTree.URL_TEMPLATE_PREFIX + urlTemplate.getId(),
                EditorTree.urlTemplateLeafId(urlTemplate));
        assertNull(EditorTree.urlTemplateLeafId(null));
        assertEquals("/editor/resources?group=category-abc", EditorTree.hrefOf("category-abc"));
        assertEquals("/editor/resources?group=content", EditorTree.hrefOf(EditorTree.CONTENT_ID));
        assertEquals("/editor/resources?group=content-categories", EditorTree.hrefOf(EditorTree.CATEGORIES_ID));
        assertEquals("/editor/resources?group=content-apps", EditorTree.hrefOf(EditorTree.APPS_ID));
        assertEquals("/editor/resources?group=content-playlists", EditorTree.hrefOf(EditorTree.PLAYLISTS_ID));
        assertEquals("/editor/resources?group=content-url-templates",
                EditorTree.hrefOf(EditorTree.URL_TEMPLATES_ID));
        assertEquals("/editor/resources?group=content-themes", EditorTree.hrefOf(EditorTree.THEMES_ID));
        assertEquals("/editor/resources?group=content-apps",
                EditorTree.packUploadReturnHref(EditorTree.APPS_ID));
        assertEquals("/editor/resources?group=content-themes",
                EditorTree.packUploadReturnHref(EditorTree.THEMES_ID));
        assertNull(EditorTree.packUploadReturnHref("moments"));
        assertEquals(EditorTree.CATEGORIES_HREF,
                EditorTree.categoryDeleteReturnHref(EditorTree.CATEGORIES_ID));
        assertEquals(EditorTree.CATEGORIES_HREF,
                EditorTree.categoryDeleteReturnHref("category-abc"));
        assertEquals(EditorTree.CATEGORIES_HREF, EditorTree.categoryDeleteReturnHref(null));
        assertEquals(EditorTree.CATEGORIES_HREF, EditorTree.categoryDeleteReturnHref("moments"));
        assertEquals("/editor/resources?group=category-abc",
                EditorTree.pageDeleteReturnHref("category-abc", "ignored"));
        assertEquals("/editor/resources?group=category-abc",
                EditorTree.pageDeleteReturnHref(null, "abc"));
        assertEquals(EditorTree.CATEGORIES_HREF, EditorTree.pageDeleteReturnHref(null, null));
        assertEquals(EditorTree.APPS_HREF, EditorTree.catalogReturnHref(null, EditorTree.APPS_ID));
        assertEquals(EditorTree.THEMES_HREF,
                EditorTree.catalogReturnHref(EditorTree.THEMES_ID, EditorTree.APPS_ID));
        assertEquals("/editor/resources?group=content-apps&notice=Cannot+delete",
                EditorTree.withNotice(EditorTree.APPS_HREF, "Cannot delete"));
        assertTrue(EditorTree.browseInResources("content-categories"));
        assertTrue(EditorTree.browseInResources("category-abc"));
        assertTrue(EditorTree.browseInResources(EditorTree.APPS_ID));
        assertTrue(EditorTree.browseInResources(EditorTree.PLAYLISTS_ID));
        assertTrue(EditorTree.browseInResources(EditorTree.URL_TEMPLATES_ID));
        assertTrue(EditorTree.browseInResources(EditorTree.THEMES_ID));
    }

    @Test
    void findWalksVirtualAndRealNodes() {
        ResourceGroupEntity unsorted = group("Unsorted");
        List<ResourceGroupDisplayDTO> roots = EditorTree.attach(
                ResourceGroupTreeView.assembleStats(List.of(unsorted), List.of()),
                List.of(), List.of());
        assertEquals("Content", EditorTree.find(roots, EditorTree.CONTENT_ID).getHumanReadableId());
        assertEquals("Apps", EditorTree.find(roots, EditorTree.APPS_ID).getHumanReadableId());
        assertEquals("Unsorted", EditorTree.find(roots, unsorted.getId().toString()).getHumanReadableId());
        assertNull(EditorTree.find(roots, "missing"));
    }

    private static ResourceGroupEntity group(String name) {
        ResourceGroupEntity entity = new ResourceGroupEntity(new ResourceGroupData(name), null);
        entity.setId(EntityId.generate());
        return entity;
    }

    private static CategoryEntity category(String name, int priority) {
        CategoryData data = new CategoryData(name.toLowerCase(), name, "", true, true);
        data.setDisplayPriority(priority);
        CategoryEntity entity = new CategoryEntity(data, null);
        entity.setId(EntityId.generate());
        return entity;
    }

    private static ItemEntity page(String uri, String title, EntityId categoryId) {
        PageData data = new PageData();
        data.setHeader(title);
        ItemEntity item = new ItemEntity(uri, data, null);
        item.setId(EntityId.generate());
        if (categoryId != null) {
            CategoryEntity ref = new CategoryEntity();
            ref.setId(categoryId);
            item.setRefCategory(ref);
        }
        return item;
    }

    private static ItemEntity album(String uri, String title, EntityId categoryId) {
        AlbumData data = new AlbumData();
        data.setHeader(title);
        ItemEntity item = new ItemEntity(uri, data, null);
        item.setId(EntityId.generate());
        if (categoryId != null) {
            CategoryEntity ref = new CategoryEntity();
            ref.setId(categoryId);
            item.setRefCategory(ref);
        }
        return item;
    }
}

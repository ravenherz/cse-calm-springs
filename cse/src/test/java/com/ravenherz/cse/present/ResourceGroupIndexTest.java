package com.ravenherz.cse.present;

import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.StoredIds;

import com.ravenherz.cse.dal.dao.AppService;
import com.ravenherz.cse.dal.dao.CategoryService;
import com.ravenherz.cse.dal.dao.ItemService;
import com.ravenherz.cse.dal.dao.ResourceGroupService;
import com.ravenherz.cse.dal.dao.ResourceService;
import com.ravenherz.cse.dal.dto.AppEntity;
import com.ravenherz.cse.dal.dto.CategoryEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.ResourceGroupEntity;
import com.ravenherz.cse.dal.dto.basic.AppData;
import com.ravenherz.cse.dal.dto.basic.CategoryData;
import com.ravenherz.cse.dal.dto.basic.PageData;
import com.ravenherz.cse.dal.dto.basic.ResourceData;
import com.ravenherz.cse.dal.dto.basic.ResourceGroupData;
import com.ravenherz.cse.dal.dto.basic.ResourcePreviewSource;
import com.ravenherz.cse.dal.dto.basic.ResourceSizeHint;
import com.ravenherz.cse.dal.dto.basic.enums.ResourceType;
import com.ravenherz.cse.util.staticapps.StaticAppDeployer;
import com.ravenherz.cse.util.themes.ThemeCatalog;
import com.ravenherz.cse.util.themes.ThemeInfo;
import com.ravenherz.cse.util.themes.ThemeSelection;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceGroupIndexTest {

    @Test
    void firstViewLoadsHintsOnceAndLaterDeltasSkipMongo() {
        ResourceGroupEntity travel = group("Travel");
        ResourceGroupService groups = mock(ResourceGroupService.class);
        ResourceService resources = mock(ResourceService.class);
        when(groups.getAllGroups()).thenReturn(List.of(travel));
        when(resources.listSizeHints()).thenReturn(List.of(new ResourceSizeHint(StoredIds.objectId(travel.getId()), 50)));
        ResourceGroupIndex index = new ResourceGroupIndex(groups, resources);

        ResourceGroupDisplayDTO first = index.view().roots().get(0);
        assertEquals(1, first.getOwnFileCount());
        assertEquals(50, first.getTotalSize());
        index.fileAdded(travel.getId().toString(), 10);
        ResourceGroupDisplayDTO after = index.view().roots().get(0);
        assertEquals(2, after.getOwnFileCount());
        assertEquals(60, after.getTotalSize());
        assertEquals(2, after.getDescendantFileCount());
        verify(resources, times(1)).listSizeHints();
        verify(groups, times(1)).getAllGroups();
    }

    @Test
    void viewAttachesTreeFileNamesFromHints() {
        ResourceGroupEntity travel = group("Travel");
        ObjectId fileId = new ObjectId();
        ResourceGroupService groups = mock(ResourceGroupService.class);
        ResourceService resources = mock(ResourceService.class);
        when(groups.getAllGroups()).thenReturn(List.of(travel));
        when(resources.listSizeHints()).thenReturn(List.of(
                new ResourceSizeHint(fileId, StoredIds.objectId(travel.getId()), "/u/res/image/cat.jpg", 50)));
        ResourceGroupIndex index = new ResourceGroupIndex(groups, resources);
        ResourceGroupDisplayDTO first = index.view().roots().get(0);
        assertEquals("cat.jpg", first.getTreeFiles().get(0).name());
        assertEquals(fileId.toString(), first.getTreeFiles().get(0).id());
        assertFalse(first.getTreeFiles().get(0).preview());
    }

    @Test
    void viewStoresTreePreviewsFromSources() throws Exception {
        ResourceGroupEntity travel = group("Travel");
        ObjectId fileId = new ObjectId();
        ResourceGroupService groups = mock(ResourceGroupService.class);
        ResourceService resources = mock(ResourceService.class);
        when(groups.getAllGroups()).thenReturn(List.of(travel));
        when(resources.listSizeHints()).thenReturn(List.of(
                new ResourceSizeHint(fileId, StoredIds.objectId(travel.getId()), "/u/res/image/cat.jpg", 50)));
        stubPreviewSources(resources, new ResourcePreviewSource(fileId, tinyJpeg()));
        ResourceGroupIndex index = new ResourceGroupIndex(groups, resources);
        ResourceGroupDisplayDTO first = index.view().roots().get(0);
        assertTrue(first.getTreeFiles().get(0).preview());
        byte[] stored = index.treePreview(fileId.toString());
        assertNotNull(stored);
        BufferedImage out = ImageIO.read(new ByteArrayInputStream(stored));
        assertEquals(TreePreviews.SIZE, out.getWidth());
        assertEquals(TreePreviews.SIZE, out.getHeight());
        verify(resources, times(1)).forEachPreviewSource(any());
        index.fileAdded(travel.getId().toString(), 10);
        assertNotNull(index.treePreview(fileId.toString()));
        verify(resources, times(1)).forEachPreviewSource(any());
    }

    @Test
    void fileAddedStoresTreePreviewWithoutReloadingMongo() {
        ResourceGroupEntity travel = group("Travel");
        ResourceGroupService groups = mock(ResourceGroupService.class);
        ResourceService resources = mock(ResourceService.class);
        when(groups.getAllGroups()).thenReturn(List.of(travel));
        when(resources.listSizeHints()).thenReturn(List.of());
        stubPreviewSources(resources);
        ResourceGroupIndex index = new ResourceGroupIndex(groups, resources);
        index.view();
        ResourceEntity file = imageWithPreview(travel, tinyJpeg());
        index.fileAdded(file);
        ResourceGroupDisplayDTO after = index.view().roots().get(0);
        assertEquals(1, after.getTreeFiles().size());
        assertTrue(after.getTreeFiles().get(0).preview());
        assertNotNull(index.treePreview(file.getId().toString()));
        verify(resources, times(1)).forEachPreviewSource(any());
    }

    @Test
    void fileRenamedUpdatesTreeFileNameWithoutReloadingMongo() {
        ResourceGroupEntity travel = group("Travel");
        ObjectId fileId = new ObjectId();
        ResourceGroupService groups = mock(ResourceGroupService.class);
        ResourceService resources = mock(ResourceService.class);
        when(groups.getAllGroups()).thenReturn(List.of(travel));
        when(resources.listSizeHints()).thenReturn(List.of(
                new ResourceSizeHint(fileId, StoredIds.objectId(travel.getId()), "/u/res/image/cat.jpg", 50)));
        ResourceGroupIndex index = new ResourceGroupIndex(groups, resources);
        index.view();
        ResourceEntity file = imageWithPreview(travel, tinyJpeg());
        file.setId(EntityId.of(fileId.toHexString()));
        file.getResourceData().setPathPublic("/u/res/image/dog.jpg");
        index.fileRenamed(file);
        ResourceGroupDisplayDTO after = index.view().roots().get(0);
        assertEquals(1, after.getTreeFiles().size());
        assertEquals("dog.jpg", after.getTreeFiles().get(0).name());
        assertEquals("/u/res/image/dog.jpg", after.getTreeFiles().get(0).key());
        assertEquals(1, after.getOwnFileCount());
        assertEquals(50, after.getTotalSize());
        verify(resources, times(1)).listSizeHints();
        verify(groups, times(1)).getAllGroups();
    }

    @Test
    void fileRemovedDropsTreePreview() {
        ResourceGroupEntity travel = group("Travel");
        ObjectId fileId = new ObjectId();
        ResourceGroupService groups = mock(ResourceGroupService.class);
        ResourceService resources = mock(ResourceService.class);
        when(groups.getAllGroups()).thenReturn(List.of(travel));
        when(resources.listSizeHints()).thenReturn(List.of(
                new ResourceSizeHint(fileId, StoredIds.objectId(travel.getId()), "/u/res/image/cat.jpg", 50)));
        stubPreviewSources(resources, new ResourcePreviewSource(fileId, tinyJpeg()));
        ResourceGroupIndex index = new ResourceGroupIndex(groups, resources);
        index.view();
        ResourceEntity file = imageWithPreview(travel, tinyJpeg());
        file.setId(EntityId.of(fileId.toHexString()));
        index.fileRemoved(file);
        assertNull(index.treePreview(fileId.toString()));
        assertTrue(index.view().roots().get(0).getTreeFiles().isEmpty());
    }

    @Test
    void pageLeafUsesFeaturedImagePreview() {
        ResourceGroupEntity unsorted = group("Unsorted");
        ObjectId fileId = new ObjectId();
        CategoryEntity shots = new CategoryEntity(new CategoryData("shots", "Shots", "", true, true), null);
        shots.setId(EntityId.generate());
        ItemEntity page = new ItemEntity("aurora", new PageData(), null);
        page.setId(EntityId.generate());
        page.getPageData().setHeader("Aurora");
        ResourceEntity featured = new ResourceEntity();
        featured.setId(EntityId.of(fileId.toHexString()));
        page.getPageData().setRefImageId(featured.getId());
        CategoryEntity ref = new CategoryEntity();
        ref.setId(shots.getId());
        page.setRefCategory(ref);

        ResourceGroupService groups = mock(ResourceGroupService.class);
        ResourceService resources = mock(ResourceService.class);
        CategoryService categories = mock(CategoryService.class);
        ItemService items = mock(ItemService.class);
        when(groups.getAllGroups()).thenReturn(List.of(unsorted));
        when(resources.listSizeHints()).thenReturn(List.of(
                new ResourceSizeHint(fileId, StoredIds.objectId(unsorted.getId()), "/u/res/image/aurora.jpg", 50)));
        stubPreviewSources(resources, new ResourcePreviewSource(fileId, tinyJpeg()));
        when(categories.getAllCategories()).thenReturn(List.of(shots));
        when(items.getAll()).thenReturn(List.of(page));
        ResourceGroupIndex index = new ResourceGroupIndex(groups, resources, categories, items);
        index.editorRoots();
        assertNotNull(index.treePreview(EditorTree.pageLeafId(page)));
        ResourceGroupDisplayDTO cats = index.editorRoots().get(0).getChildren().get(1);
        assertTrue(cats.getChildren().get(0).getTreeFiles().get(0).preview());
    }

    @Test
    void structureChangeRebuildsFromMongo() {
        ResourceGroupEntity travel = group("Travel");
        ResourceGroupEntity studio = group("Studio");
        ResourceGroupService groups = mock(ResourceGroupService.class);
        ResourceService resources = mock(ResourceService.class);
        when(groups.getAllGroups()).thenReturn(List.of(travel), List.of(travel, studio));
        when(resources.listSizeHints()).thenReturn(List.of(), List.of());
        ResourceGroupIndex index = new ResourceGroupIndex(groups, resources);
        assertEquals(1, index.view().roots().size());
        index.structureChanged();
        assertEquals(2, index.view().roots().size());
        verify(groups, times(2)).getAllGroups();
    }

    @Test
    void fileAddedOnColdIndexRebuildsWithoutDoubleCount() {
        ResourceGroupEntity travel = group("Travel");
        ResourceGroupService groups = mock(ResourceGroupService.class);
        ResourceService resources = mock(ResourceService.class);
        when(groups.getAllGroups()).thenReturn(List.of(travel));
        when(resources.listSizeHints()).thenReturn(List.of(new ResourceSizeHint(StoredIds.objectId(travel.getId()), 50)));
        ResourceGroupIndex index = new ResourceGroupIndex(groups, resources);
        index.fileAdded(travel.getId().toString(), 50);
        assertEquals(1, index.view().roots().get(0).getOwnFileCount());
        assertEquals(50, index.view().roots().get(0).getTotalSize());
    }

    @Test
    void filesForUngroupedQueriesNullGroupId() {
        ResourceGroupService groups = mock(ResourceGroupService.class);
        ResourceService resources = mock(ResourceService.class);
        ResourceEntity loose = new ResourceEntity(new ResourceData(), null);
        when(resources.listForEditor(null)).thenReturn(List.of(loose));
        ResourceGroupIndex index = new ResourceGroupIndex(groups, resources);
        ResourceGroupDisplayDTO ungrouped = new ResourceGroupDisplayDTO();
        ungrouped.setId("ungrouped");
        List<ResourceEntity> files = index.filesFor(ungrouped);
        assertEquals(1, files.size());
        assertTrue(files.contains(loose));
        verify(resources).listForEditor(null);
    }

    @Test
    void filesForDefaultMergesOrphans() {
        ResourceGroupEntity defaults = group("Default");
        ResourceGroupService groups = mock(ResourceGroupService.class);
        ResourceService resources = mock(ResourceService.class);
        ResourceEntity grouped = new ResourceEntity(new ResourceData(), null);
        ResourceEntity loose = new ResourceEntity(new ResourceData(), null);
        when(resources.listForEditor(StoredIds.objectId(defaults.getId()))).thenReturn(List.of(grouped));
        when(resources.listForEditor(null)).thenReturn(List.of(loose));
        ResourceGroupIndex index = new ResourceGroupIndex(groups, resources);
        ResourceGroupDisplayDTO defaultDto = new ResourceGroupDisplayDTO();
        defaultDto.setId(defaults.getId().toString());
        defaultDto.setDefaultGroup(true);
        List<ResourceEntity> files = index.filesFor(defaultDto);
        assertEquals(2, files.size());
        assertTrue(files.contains(grouped));
        assertTrue(files.contains(loose));
        verify(resources).listForEditor(StoredIds.objectId(defaults.getId()));
        verify(resources).listForEditor(null);
    }

    @Test
    void editorRootsCachesContentUntilContentChanges() {
        ResourceGroupEntity unsorted = group("Unsorted");
        CategoryEntity shots = new CategoryEntity(new CategoryData("shots", "Shots", "", true, true), null);
        shots.setId(EntityId.generate());
        ItemEntity page = new ItemEntity("aurora", new PageData(), null);
        page.setId(EntityId.generate());
        page.getPageData().setHeader("Aurora");
        CategoryEntity ref = new CategoryEntity();
        ref.setId(shots.getId());
        page.setRefCategory(ref);

        ResourceGroupService groups = mock(ResourceGroupService.class);
        ResourceService resources = mock(ResourceService.class);
        CategoryService categories = mock(CategoryService.class);
        ItemService items = mock(ItemService.class);
        when(groups.getAllGroups()).thenReturn(List.of(unsorted));
        when(resources.listSizeHints()).thenReturn(List.of());
        when(categories.getAllCategories()).thenReturn(List.of(shots));
        when(items.getAll()).thenReturn(List.of(page));
        ResourceGroupIndex index = new ResourceGroupIndex(groups, resources, categories, items);

        List<ResourceGroupDisplayDTO> first = index.editorRoots();
        assertEquals("Content", first.get(0).getHumanReadableId());
        ResourceGroupDisplayDTO cats = first.get(0).getChildren().get(1);
        assertEquals("/editor/resources?group=content-categories", cats.getHref());
        assertEquals("Shots", cats.getChildren().get(0).getHumanReadableId());
        assertEquals(1, index.categories().size());
        assertEquals(1, index.itemsInCategory(shots.getId().toString()).size());
        assertEquals("Aurora", index.itemsInCategory(shots.getId().toString()).get(0).getPageData().getHeader());
        index.editorRoots();
        verify(categories, times(1)).getAllCategories();
        verify(items, times(1)).getAll();

        CategoryEntity nights = new CategoryEntity(new CategoryData("nights", "Nights", "", true, true), null);
        nights.setId(EntityId.generate());
        when(categories.getAllCategories()).thenReturn(List.of(shots, nights));
        index.contentChanged();
        List<ResourceGroupDisplayDTO> after = index.editorRoots();
        assertEquals(2, after.get(0).getChildren().get(1).getChildren().size());
        verify(categories, times(2)).getAllCategories();
    }

    @Test
    void contentChangedRereadsAppProductLogoFromDisk() {
        AppData data = new AppData();
        data.setSlug("fretlab");
        data.setAppName("Fretboard Lab");
        AppEntity stored = new AppEntity(data, null);
        AppService apps = mock(AppService.class);
        StaticAppDeployer deployer = mock(StaticAppDeployer.class);
        ThemeCatalog themes = mock(ThemeCatalog.class);
        when(apps.getAllApps()).thenReturn(List.of(stored));
        when(deployer.hasValidProductLogo("fretlab")).thenReturn(false);
        when(themes.list()).thenReturn(List.of());
        when(themes.resolve()).thenReturn(ThemeSelection.fallback());
        ResourceGroupIndex index = catalogIndex(apps, deployer, themes);

        assertFalse(appNamed(index, "fretlab").isProductLogo());
        when(deployer.hasValidProductLogo("fretlab")).thenReturn(true);
        index.contentChanged();
        assertTrue(appNamed(index, "fretlab").isProductLogo());
    }

    @Test
    void contentChangedRereadsWhichThemeIsActive() {
        ThemeInfo modern = new ThemeInfo("modern", "modern", null, null, "modern", "linen",
                List.of("linen"), true);
        ThemeInfo client = new ThemeInfo("client", "JS", null, null, "js", "studio",
                List.of("studio"), false);
        ThemeCatalog themes = mock(ThemeCatalog.class);
        when(themes.list()).thenReturn(List.of(modern, client));
        when(themes.resolve()).thenReturn(new ThemeSelection("modern", "linen", "modern"));
        ResourceGroupIndex index = catalogIndex(mock(AppService.class), mock(StaticAppDeployer.class), themes);

        assertTrue(themeNamed(index, "modern").isActive());
        assertFalse(themeNamed(index, "client").isActive());
        when(themes.resolve()).thenReturn(new ThemeSelection("client", "studio", "client"));
        index.contentChanged();
        assertFalse(themeNamed(index, "modern").isActive());
        assertTrue(themeNamed(index, "client").isActive());
        ResourceTreeFile clientLeaf = themeLeaf(index, "client");
        assertNotNull(clientLeaf);
        assertFalse(clientLeaf.canActivate());
        ResourceTreeFile modernLeaf = themeLeaf(index, "modern");
        assertNotNull(modernLeaf);
        assertTrue(modernLeaf.canActivate());
    }

    private static ResourceGroupIndex catalogIndex(AppService apps, StaticAppDeployer deployer,
            ThemeCatalog themes) {
        ResourceGroupService groups = mock(ResourceGroupService.class);
        ResourceService resources = mock(ResourceService.class);
        CategoryService categories = mock(CategoryService.class);
        ItemService items = mock(ItemService.class);
        when(groups.getAllGroups()).thenReturn(List.of());
        when(resources.listSizeHints()).thenReturn(List.of());
        when(categories.getAllCategories()).thenReturn(List.of());
        when(items.getAll()).thenReturn(List.of());
        return new ResourceGroupIndex(groups, resources, categories, items, apps, null, themes,
                deployer, null);
    }

    private static AppDisplayDTO appNamed(ResourceGroupIndex index, String slug) {
        return index.apps().stream()
                .filter(app -> slug.equals(app.getSlug()))
                .findFirst()
                .orElseThrow();
    }

    private static ThemeDisplayDTO themeNamed(ResourceGroupIndex index, String id) {
        return index.themes().stream()
                .filter(theme -> id.equals(theme.getThemeId()))
                .findFirst()
                .orElseThrow();
    }

    private static ResourceTreeFile themeLeaf(ResourceGroupIndex index, String themeId) {
        ResourceGroupDisplayDTO content = index.contentView();
        ResourceGroupDisplayDTO themes = content.getChildren().stream()
                .filter(child -> EditorTree.THEMES_ID.equals(child.getId()))
                .findFirst()
                .orElseThrow();
        return themes.getTreeFiles().stream()
                .filter(file -> ("theme-" + themeId).equals(file.id()))
                .findFirst()
                .orElse(null);
    }

    private static void stubPreviewSources(ResourceService resources, ResourcePreviewSource... sources) {
        doAnswer(invocation -> {
            Consumer<ResourcePreviewSource> consumer = invocation.getArgument(0);
            if (consumer == null) {
                return null;
            }
            for (ResourcePreviewSource source : sources) {
                consumer.accept(source);
            }
            return null;
        }).when(resources).forEachPreviewSource(any());
    }

    private static ResourceGroupEntity group(String name) {
        ResourceGroupEntity entity = new ResourceGroupEntity(new ResourceGroupData(name), null);
        entity.setId(EntityId.generate());
        return entity;
    }

    private static ResourceEntity imageWithPreview(ResourceGroupEntity group, byte[] jpeg) {
        ResourceData data = new ResourceData();
        data.setType(ResourceType.IMAGE);
        data.setPathPublic("/u/res/image/cat.jpg");
        data.setSizeInBytes(jpeg == null ? 0 : jpeg.length);
        ResourceEntity entity = new ResourceEntity(data, null);
        entity.setId(EntityId.generate());
        entity.setRefResourceGroup(group);
        ResourceData preview = new ResourceData();
        preview.setContentRaw(Base64.getEncoder().encodeToString(jpeg));
        entity.setPreviewData(preview);
        return entity;
    }

    private static byte[] tinyJpeg() {
        try {
            BufferedImage src = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = src.createGraphics();
            graphics.setColor(Color.GREEN);
            graphics.fillRect(0, 0, 8, 8);
            graphics.dispose();
            return com.ravenherz.cse.util.imaging.JpegImages.encode(src, 0.9f);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

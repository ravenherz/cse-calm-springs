package com.ravenherz.cse.controller;

import com.ravenherz.cse.dal.ResourceGroupTree;
import com.ravenherz.cse.dal.dto.CategoryEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.PlaylistEntity;
import com.ravenherz.cse.dal.dto.UrlTemplateEntity;
import com.ravenherz.cse.present.EditorTree;
import com.ravenherz.cse.present.ResourceGroupDisplayDTO;
import com.ravenherz.cse.present.ResourceGroupIndex;
import com.ravenherz.cse.present.ResourceGroupTreeView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;

import java.util.List;

final class EditorInline {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditorInline.class);

    private EditorInline() {
    }

    static void putTreeForItem(Model model, ResourceGroupIndex index, ItemEntity item) {
        String categoryId = item == null || item.getRefCategoryId() == null
                ? null : item.getRefCategoryId().toString();
        String name = item == null ? null : item.getUniqueUriName();
        putTree(model, index,
                EditorTree.selectionId("/editor/edit", null, null, name, null, categoryId),
                EditorTree.pageLeafId(item));
    }

    static void putTreeForCategory(Model model, ResourceGroupIndex index, CategoryEntity category) {
        String id = category == null || category.getId() == null ? null : category.getId().toString();
        putTree(model, index, EditorTree.categoryNodeId(id), null);
    }

    static void putTreeForCategoryCreate(Model model, ResourceGroupIndex index) {
        putTree(model, index, EditorTree.CATEGORIES_ID, null);
    }

    static void putTreeForPlaylistCreate(Model model, ResourceGroupIndex index) {
        putTree(model, index, EditorTree.PLAYLISTS_ID, null);
    }

    static void putTreeForPlaylist(Model model, ResourceGroupIndex index, PlaylistEntity playlist) {
        putTree(model, index, EditorTree.PLAYLISTS_ID, EditorTree.playlistLeafId(playlist));
    }

    static void putTreeForUrlTemplateCreate(Model model, ResourceGroupIndex index) {
        putTree(model, index, EditorTree.URL_TEMPLATES_ID, null);
    }

    static void putTreeForUrlTemplate(Model model, ResourceGroupIndex index, UrlTemplateEntity template) {
        putTree(model, index, EditorTree.URL_TEMPLATES_ID, EditorTree.urlTemplateLeafId(template));
    }

    static void putTreeForCreate(Model model, ResourceGroupIndex index, String categoryId) {
        putTree(model, index,
                EditorTree.selectionId("/editor/create", null, categoryId, null, null, null),
                null);
    }

    static void putTreeForAlbumCreate(Model model, ResourceGroupIndex index, String categoryId) {
        putTree(model, index,
                EditorTree.selectionId("/editor/album", null, categoryId, null, null, null),
                null);
    }

    static void putTree(Model model, ResourceGroupIndex index, String selectedId, String selectedLeafId) {
        ResourceGroupDisplayDTO emptyHome = new ResourceGroupDisplayDTO();
        emptyHome.setHumanReadableId(ResourceGroupTree.DEFAULT_NAME);
        emptyHome.setPathLabel(ResourceGroupTree.DEFAULT_NAME);
        emptyHome.setDefaultGroup(true);
        model.addAttribute("resourceGroupTree", List.of());
        model.addAttribute("selectedGroup", emptyHome);
        model.addAttribute("defaultGroupId", "");
        model.addAttribute("selectedLeafId", selectedLeafId);
        if (index == null) {
            return;
        }
        try {
            ResourceGroupTreeView.Assembled tree = index.view();
            List<ResourceGroupDisplayDTO> chrome = index.editorRoots();
            String homeId = ResourceGroupTreeView.homeGroupId(tree);
            model.addAttribute("resourceGroupTree", chrome);
            model.addAttribute("defaultGroupId", homeId == null ? "" : homeId);
            ResourceGroupDisplayDTO selected = EditorTree.find(chrome, selectedId);
            if (selected == null && selectedId != null && !selectedId.isBlank()
                    && !EditorTree.isChromeId(selectedId)) {
                selected = ResourceGroupTreeView.find(tree, selectedId);
            }
            if (selected != null) {
                model.addAttribute("selectedGroup", selected);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load editor tree: " + e.getMessage(), e);
        }
    }
}

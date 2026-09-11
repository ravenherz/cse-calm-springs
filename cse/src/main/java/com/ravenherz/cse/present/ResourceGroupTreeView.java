package com.ravenherz.cse.present;

import com.ravenherz.cse.dal.EntityAccess;
import com.ravenherz.cse.dal.ResourceGroupTree;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.ResourceGroupEntity;
import com.ravenherz.cse.dal.dto.basic.SecurityData;
import com.ravenherz.cse.dal.dto.basic.ResourceSizeHint;
import com.ravenherz.cse.dal.dto.basic.enums.AccessType;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the editor tree from the in-memory group list. Missing parent = root.
 */
public final class ResourceGroupTreeView {

    public static final String UNGROUPED_ID = "ungrouped";

    private ResourceGroupTreeView() {
    }

    public record Assembled(
            List<ResourceGroupDisplayDTO> roots,
            List<ResourceGroupDisplayDTO> assignableGroups,
            ResourceGroupDisplayDTO ungrouped
    ) {
    }

    public record OwnStats(int fileCount, long byteSize) {
        public static final OwnStats EMPTY = new OwnStats(0, 0);

        public OwnStats {
            fileCount = Math.max(fileCount, 0);
            byteSize = Math.max(byteSize, 0);
        }

        public OwnStats plus(int dCount, long dBytes) {
            return new OwnStats(fileCount + dCount, byteSize + dBytes);
        }
    }

    public static Assembled assemble(List<ResourceGroupEntity> groups, List<ResourceEntity> resources) {
        Map<String, OwnStats> own = new HashMap<>();
        Map<ObjectId, List<ResourceEntity>> filesByGroup = new HashMap<>();
        List<ResourceEntity> ungroupedFiles = new ArrayList<>();
        OwnStats ungrouped = OwnStats.EMPTY;
        if (resources != null) {
            for (ResourceEntity resource : resources) {
                if (resource == null) {
                    continue;
                }
                long bytes = resource.getResourceData() == null ? 0L : resource.getResourceData().getSizeInBytes();
                ObjectId groupId = resource.refResourceGroupObjectId();
                if (groupId == null) {
                    ungroupedFiles.add(resource);
                    ungrouped = ungrouped.plus(1, bytes);
                } else {
                    filesByGroup.computeIfAbsent(groupId, id -> new ArrayList<>()).add(resource);
                    String key = groupId.toString();
                    own.put(key, own.getOrDefault(key, OwnStats.EMPTY).plus(1, bytes));
                }
            }
        }
        Assembled tree = assembleStats(groups, own, ungrouped, filesByGroupTree(filesByGroup), treeFilesOf(ungroupedFiles));
        for (ResourceGroupDisplayDTO dto : tree.assignableGroups()) {
            ObjectId id = parseObjectId(dto.getId());
            dto.setResources(id == null ? List.of() : filesByGroup.getOrDefault(id, List.of()));
        }
        ResourceGroupDisplayDTO defaults = defaultGroup(tree);
        if (defaults != null) {
            List<ResourceEntity> merged = new ArrayList<>(defaults.getResources());
            merged.addAll(ungroupedFiles);
            defaults.setResources(merged);
            tree.ungrouped().setResources(List.of());
        } else {
            tree.ungrouped().setResources(ungroupedFiles);
        }
        return tree;
    }

    public static Assembled assembleStats(List<ResourceGroupEntity> groups, List<ResourceSizeHint> hints) {
        Map<String, OwnStats> own = new HashMap<>();
        OwnStats ungrouped = OwnStats.EMPTY;
        Map<String, List<ResourceTreeFile>> treeFiles = new HashMap<>();
        List<ResourceTreeFile> ungroupedFiles = new ArrayList<>();
        if (hints != null) {
            for (ResourceSizeHint hint : hints) {
                if (hint == null) {
                    continue;
                }
                ResourceTreeFile leaf = ResourceTreeFile.from(hint);
                if (hint.groupId() == null) {
                    ungrouped = ungrouped.plus(1, hint.sizeInBytes());
                    if (leaf != null) {
                        ungroupedFiles.add(leaf);
                    }
                    continue;
                }
                String key = hint.groupId().toString();
                own.put(key, own.getOrDefault(key, OwnStats.EMPTY).plus(1, hint.sizeInBytes()));
                if (leaf != null) {
                    treeFiles.computeIfAbsent(key, id -> new ArrayList<>()).add(leaf);
                }
            }
        }
        return assembleStats(groups, own, ungrouped, treeFiles, ungroupedFiles);
    }

    public static Assembled assembleStats(List<ResourceGroupEntity> groups, Map<String, OwnStats> ownByGroupId,
            OwnStats ungroupedOwn) {
        return assembleStats(groups, ownByGroupId, ungroupedOwn, Map.of(), List.of());
    }

    public static Assembled assembleStats(List<ResourceGroupEntity> groups, Map<String, OwnStats> ownByGroupId,
            OwnStats ungroupedOwn, Map<String, List<ResourceTreeFile>> treeFilesByGroupId,
            List<ResourceTreeFile> ungroupedFiles) {
        List<ResourceGroupEntity> all = groups == null ? List.of() : groups;
        Map<String, OwnStats> own = ownByGroupId == null ? Map.of() : ownByGroupId;
        Map<String, ResourceGroupDisplayDTO> byId = new HashMap<>();
        for (ResourceGroupEntity group : all) {
            if (group == null || group.getId() == null) {
                continue;
            }
            ObjectId parentId = ResourceGroupTree.parentId(group);
            if (parentId != null && ResourceGroupTree.find(all, parentId) == null) {
                parentId = null;
            }
            OwnStats ownStats = own.getOrDefault(group.getId().toString(), OwnStats.EMPTY);
            ResourceGroupDisplayDTO dto = new ResourceGroupDisplayDTO();
            dto.setId(group.getId().toString());
            dto.setParentId(parentId == null ? null : parentId.toString());
            dto.setDefaultGroup(ResourceGroupTree.isDefault(group));
            dto.setHumanReadableId(dto.isDefaultGroup()
                    ? ResourceGroupTree.DEFAULT_NAME : ResourceGroupTree.nameOf(group));
            dto.setDepth(ResourceGroupTree.depthOf(all, group.getId()));
            dto.setSubtreeHeight(ResourceGroupTree.heightOf(all, group.getId()));
            applyOwn(dto, ownStats);
            dto.setTreeFiles(sortedTreeFiles(
                    treeFilesByGroupId == null ? null : treeFilesByGroupId.get(dto.getId())));
            dto.setSecurityData(group.getSecurityData() == null ? new SecurityData() : group.getSecurityData());
            dto.setGuestDenied(!EntityAccess.isAccessible(group, AccessType.ACCESS_READ, null));
            byId.put(dto.getId(), dto);
        }

        for (ResourceGroupDisplayDTO dto : byId.values()) {
            dto.setPathLabel(pathLabel(dto, byId));
        }

        List<ResourceGroupDisplayDTO> roots = new ArrayList<>();
        for (ResourceGroupDisplayDTO dto : byId.values()) {
            if (dto.getParentId() == null) {
                roots.add(dto);
                continue;
            }
            ResourceGroupDisplayDTO parent = byId.get(dto.getParentId());
            if (parent == null) {
                roots.add(dto);
            } else {
                parent.getChildren().add(dto);
            }
        }

        Comparator<ResourceGroupDisplayDTO> order = Comparator
                .comparing((ResourceGroupDisplayDTO group) -> !group.isDefaultGroup())
                .thenComparing(group -> group.getHumanReadableId() == null ? "" : group.getHumanReadableId(),
                        String.CASE_INSENSITIVE_ORDER);
        roots.sort(order);
        for (ResourceGroupDisplayDTO dto : byId.values()) {
            dto.getChildren().sort(order);
        }

        ResourceGroupDisplayDTO defaults = defaultIn(byId);
        boolean absorbed = false;
        if (defaults != null) {
            OwnStats extra = ungroupedOwn == null ? OwnStats.EMPTY : ungroupedOwn;
            applyOwn(defaults, new OwnStats(
                    defaults.getOwnFileCount() + extra.fileCount(),
                    defaults.getTotalSize() + extra.byteSize()));
            List<ResourceTreeFile> merged = new ArrayList<>(defaults.getTreeFiles());
            if (ungroupedFiles != null) {
                merged.addAll(ungroupedFiles);
            }
            defaults.setTreeFiles(sortedTreeFiles(merged));
            absorbed = true;
        }
        rollUp(roots);

        List<ResourceGroupDisplayDTO> assignable = new ArrayList<>();
        flatten(roots, assignable);
        return new Assembled(roots, assignable, leftoverUngrouped(ungroupedOwn, ungroupedFiles, absorbed));
    }

    public static void applyPreviews(Assembled tree, Set<String> previewIds) {
        if (tree == null) {
            return;
        }
        Set<String> ids = previewIds == null ? Set.of() : previewIds;
        for (ResourceGroupDisplayDTO group : tree.assignableGroups()) {
            stampFiles(group, ids);
        }
        stampFiles(tree.ungrouped(), ids);
    }

    public static void applyPreviews(ResourceGroupDisplayDTO node, Set<String> previewIds) {
        applyPreviewsRecursive(node, previewIds == null ? Set.of() : previewIds);
    }

    private static void applyPreviewsRecursive(ResourceGroupDisplayDTO node, Set<String> ids) {
        if (node == null) {
            return;
        }
        stampFiles(node, ids);
        if (node.getChildren() == null) {
            return;
        }
        for (ResourceGroupDisplayDTO child : node.getChildren()) {
            applyPreviewsRecursive(child, ids);
        }
    }

    private static void stampFiles(ResourceGroupDisplayDTO node, Set<String> ids) {
        if (node == null) {
            return;
        }
        List<ResourceTreeFile> files = node.getTreeFiles();
        if (files == null || files.isEmpty()) {
            return;
        }
        List<ResourceTreeFile> next = new ArrayList<>(files.size());
        for (ResourceTreeFile file : files) {
            if (file == null) {
                continue;
            }
            next.add(file.withPreview(file.id() != null && ids.contains(file.id())));
        }
        node.setTreeFiles(next);
    }

    public static Assembled copy(Assembled src) {
        if (src == null) {
            return assembleStats(List.of(), Map.of(), OwnStats.EMPTY);
        }
        Map<String, ResourceGroupDisplayDTO> copies = new HashMap<>();
        for (ResourceGroupDisplayDTO group : src.assignableGroups()) {
            copies.put(group.getId(), copyNode(group));
        }
        for (ResourceGroupDisplayDTO group : src.assignableGroups()) {
            ResourceGroupDisplayDTO copy = copies.get(group.getId());
            for (ResourceGroupDisplayDTO child : group.getChildren()) {
                ResourceGroupDisplayDTO childCopy = copies.get(child.getId());
                if (childCopy != null) {
                    copy.getChildren().add(childCopy);
                }
            }
        }
        List<ResourceGroupDisplayDTO> roots = new ArrayList<>();
        for (ResourceGroupDisplayDTO root : src.roots()) {
            ResourceGroupDisplayDTO copy = copies.get(root.getId());
            if (copy != null) {
                roots.add(copy);
            }
        }
        List<ResourceGroupDisplayDTO> assignable = new ArrayList<>();
        flatten(roots, assignable);
        return new Assembled(roots, assignable, copyNode(src.ungrouped()));
    }

    /**
     * Shallow copy for one request: same child folders, caller-supplied file tiles.
     */
    public static ResourceGroupDisplayDTO withResources(ResourceGroupDisplayDTO src, List<ResourceEntity> files) {
        if (src == null) {
            return null;
        }
        ResourceGroupDisplayDTO copy = copyNode(src);
        copy.setChildren(src.getChildren());
        copy.setResources(files == null ? List.of() : files);
        return copy;
    }

    public static void applyFileDelta(Assembled tree, String groupId, int dCount, long dBytes) {
        if (tree == null || (dCount == 0 && dBytes == 0)) {
            return;
        }
        ResourceGroupDisplayDTO node = nodeForDelta(tree, groupId);
        if (node == null) {
            return;
        }
        applyOwn(node, new OwnStats(node.getOwnFileCount() + dCount, node.getTotalSize() + dBytes));
        ResourceGroupDisplayDTO walk = node;
        while (walk != null) {
            walk.setDescendantFileCount(Math.max(0, walk.getDescendantFileCount() + dCount));
            walk.setDescendantTotalSize(Math.max(0, walk.getDescendantTotalSize() + dBytes));
            if (walk.isUngrouped() || walk.getParentId() == null) {
                break;
            }
            walk = lookup(tree, walk.getParentId());
        }
    }

    public static ResourceGroupDisplayDTO find(Assembled tree, String id) {
        if (tree == null) {
            return null;
        }
        if (isHomeAlias(id)) {
            return homeGroup(tree);
        }
        ResourceGroupDisplayDTO found = lookup(tree, id.trim());
        return found != null ? found : homeGroup(tree);
    }

    public static boolean containsGroup(Assembled tree, String id) {
        if (tree == null) {
            return false;
        }
        if (isHomeAlias(id)) {
            return homeGroup(tree) != null;
        }
        return lookup(tree, id.trim()) != null;
    }

    public static boolean isHomeAlias(String id) {
        return id == null || id.isBlank() || UNGROUPED_ID.equalsIgnoreCase(id.trim());
    }

    public static ResourceGroupDisplayDTO defaultGroup(Assembled tree) {
        if (tree == null) {
            return null;
        }
        for (ResourceGroupDisplayDTO group : tree.assignableGroups()) {
            if (group.isDefaultGroup()) {
                return group;
            }
        }
        return null;
    }

    public static String defaultGroupId(Assembled tree) {
        ResourceGroupDisplayDTO defaults = defaultGroup(tree);
        return defaults == null ? null : defaults.getId();
    }

    public static ResourceGroupDisplayDTO homeGroup(Assembled tree) {
        if (tree == null) {
            return null;
        }
        ResourceGroupDisplayDTO defaults = defaultGroup(tree);
        if (defaults != null) {
            return defaults;
        }
        if (!tree.roots().isEmpty()) {
            return tree.roots().get(0);
        }
        return tree.ungrouped();
    }

    public static String homeGroupId(Assembled tree) {
        ResourceGroupDisplayDTO home = homeGroup(tree);
        if (home == null || home.isUngrouped() || home.getId() == null || home.getId().isBlank()) {
            return null;
        }
        return home.getId();
    }

    public static String homeKey(Assembled tree, String groupId) {
        if (isHomeAlias(groupId)) {
            String home = defaultGroupId(tree);
            return home != null ? home : UNGROUPED_ID;
        }
        return groupId.trim();
    }

    public static String statsKey(ObjectId groupId) {
        return groupId == null ? UNGROUPED_ID : groupId.toString();
    }

    public static String statsKey(ResourceEntity resource) {
        if (resource == null) {
            return UNGROUPED_ID;
        }
        return statsKey(resource.refResourceGroupObjectId());
    }

    public static long byteSize(ResourceEntity resource) {
        if (resource == null || resource.getResourceData() == null) {
            return 0L;
        }
        return resource.getResourceData().getSizeInBytes();
    }

    private static ResourceGroupDisplayDTO nodeForDelta(Assembled tree, String groupId) {
        if (isHomeAlias(groupId)) {
            return homeGroup(tree);
        }
        return lookup(tree, groupId.trim());
    }

    private static ResourceGroupDisplayDTO lookup(Assembled tree, String id) {
        if (tree == null || id == null) {
            return null;
        }
        for (ResourceGroupDisplayDTO group : tree.assignableGroups()) {
            if (id.equals(group.getId())) {
                return group;
            }
        }
        return null;
    }

    private static ResourceGroupDisplayDTO defaultIn(Map<String, ResourceGroupDisplayDTO> byId) {
        for (ResourceGroupDisplayDTO dto : byId.values()) {
            if (dto.isDefaultGroup()) {
                return dto;
            }
        }
        return null;
    }

    private static ResourceGroupDisplayDTO leftoverUngrouped(OwnStats own, List<ResourceTreeFile> files,
            boolean absorbed) {
        ResourceGroupDisplayDTO ungrouped = new ResourceGroupDisplayDTO();
        ungrouped.setId(UNGROUPED_ID);
        ungrouped.setHumanReadableId("Ungrouped");
        ungrouped.setPathLabel("Ungrouped");
        ungrouped.setDepth(1);
        if (absorbed) {
            applyOwn(ungrouped, OwnStats.EMPTY);
            ungrouped.setTreeFiles(List.of());
        } else {
            applyOwn(ungrouped, own == null ? OwnStats.EMPTY : own);
            ungrouped.setTreeFiles(sortedTreeFiles(files));
        }
        ungrouped.setDescendantFileCount(ungrouped.getOwnFileCount());
        ungrouped.setDescendantTotalSize(ungrouped.getTotalSize());
        return ungrouped;
    }

    private static void applyOwn(ResourceGroupDisplayDTO dto, OwnStats stats) {
        OwnStats safe = stats == null ? OwnStats.EMPTY : stats;
        dto.setOwnFileCount(safe.fileCount());
        dto.setTotalSize(safe.byteSize());
    }

    private static ResourceGroupDisplayDTO copyNode(ResourceGroupDisplayDTO src) {
        ResourceGroupDisplayDTO copy = new ResourceGroupDisplayDTO();
        copy.setId(src.getId());
        copy.setParentId(src.getParentId());
        copy.setHumanReadableId(src.getHumanReadableId());
        copy.setPathLabel(src.getPathLabel());
        copy.setDepth(src.getDepth());
        copy.setSubtreeHeight(src.getSubtreeHeight());
        copy.setDefaultGroup(src.isDefaultGroup());
        copy.setVirtual(src.isVirtual());
        copy.setHref(src.getHref());
        copy.setOwnFileCount(src.getOwnFileCount());
        copy.setTotalSize(src.getTotalSize());
        copy.setDescendantFileCount(src.getDescendantFileCount());
        copy.setDescendantTotalSize(src.getDescendantTotalSize());
        copy.setTreeFiles(new ArrayList<>(src.getTreeFiles()));
        copy.setGuestDenied(src.isGuestDenied());
        copy.setSecurityData(src.getSecurityData());
        copy.setAccessCanEdit(src.isAccessCanEdit());
        copy.setCategoryItemName(src.getCategoryItemName());
        return copy;
    }

    public static void putOwnFile(Assembled tree, String groupId, ResourceTreeFile file) {
        ResourceGroupDisplayDTO node = nodeForDelta(tree, groupId);
        if (node == null || file == null || file.id() == null) {
            return;
        }
        List<ResourceTreeFile> next = new ArrayList<>();
        for (ResourceTreeFile existing : node.getTreeFiles()) {
            if (!file.id().equals(existing.id())) {
                next.add(existing);
            }
        }
        next.add(file);
        node.setTreeFiles(sortedTreeFiles(next));
    }

    public static void dropOwnFile(Assembled tree, String groupId, String resourceId) {
        ResourceGroupDisplayDTO node = nodeForDelta(tree, groupId);
        if (node == null || resourceId == null) {
            return;
        }
        List<ResourceTreeFile> next = new ArrayList<>();
        for (ResourceTreeFile existing : node.getTreeFiles()) {
            if (!resourceId.equals(existing.id())) {
                next.add(existing);
            }
        }
        node.setTreeFiles(next);
    }

    private static Map<String, List<ResourceTreeFile>> filesByGroupTree(Map<ObjectId, List<ResourceEntity>> filesByGroup) {
        Map<String, List<ResourceTreeFile>> out = new HashMap<>();
        if (filesByGroup == null) {
            return out;
        }
        for (Map.Entry<ObjectId, List<ResourceEntity>> entry : filesByGroup.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            out.put(entry.getKey().toString(), treeFilesOf(entry.getValue()));
        }
        return out;
    }

    private static List<ResourceTreeFile> treeFilesOf(List<ResourceEntity> resources) {
        List<ResourceTreeFile> files = new ArrayList<>();
        if (resources == null) {
            return files;
        }
        for (ResourceEntity resource : resources) {
            ResourceTreeFile file = ResourceTreeFile.from(resource);
            if (file != null) {
                files.add(file);
            }
        }
        return files;
    }

    private static List<ResourceTreeFile> sortedTreeFiles(List<ResourceTreeFile> files) {
        List<ResourceTreeFile> next = new ArrayList<>();
        if (files != null) {
            for (ResourceTreeFile file : files) {
                if (file != null && file.id() != null && !file.id().isBlank()) {
                    next.add(file);
                }
            }
        }
        next.sort(Comparator
                .comparing((ResourceTreeFile file) -> file.name() == null ? "" : file.name(),
                        String.CASE_INSENSITIVE_ORDER)
                .thenComparing(file -> file.id() == null ? "" : file.id()));
        return next;
    }

    private static void rollUp(List<ResourceGroupDisplayDTO> nodes) {
        if (nodes == null) {
            return;
        }
        for (ResourceGroupDisplayDTO node : nodes) {
            rollUp(node.getChildren());
            int count = node.getOwnFileCount();
            long size = node.getTotalSize();
            for (ResourceGroupDisplayDTO child : node.getChildren()) {
                count += child.getDescendantFileCount();
                size += child.getDescendantTotalSize();
            }
            node.setDescendantFileCount(count);
            node.setDescendantTotalSize(size);
        }
    }

    private static void flatten(List<ResourceGroupDisplayDTO> nodes, List<ResourceGroupDisplayDTO> out) {
        if (nodes == null) {
            return;
        }
        for (ResourceGroupDisplayDTO node : nodes) {
            out.add(node);
            flatten(node.getChildren(), out);
        }
    }

    private static String pathLabel(ResourceGroupDisplayDTO dto, Map<String, ResourceGroupDisplayDTO> byId) {
        List<String> parts = new ArrayList<>();
        ResourceGroupDisplayDTO walk = dto;
        Set<String> seen = new HashSet<>();
        while (walk != null && walk.getId() != null && seen.add(walk.getId())) {
            String name = walk.getHumanReadableId();
            parts.add(0, name == null || name.isBlank() ? walk.getId() : name);
            if (walk.getParentId() == null) {
                break;
            }
            walk = byId.get(walk.getParentId());
        }
        return String.join(" / ", parts);
    }

    private static ObjectId parseObjectId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new ObjectId(raw.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

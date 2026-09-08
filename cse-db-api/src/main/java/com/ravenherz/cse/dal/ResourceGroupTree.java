package com.ravenherz.cse.dal;

import com.ravenherz.cse.dal.dto.ResourceGroupEntity;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Parent-of-group rules. Missing {@code refParentGroup} is a root.
 * Depth 1 is a stored root. Nesting is unlimited; cycles are refused.
 */
public final class ResourceGroupTree {

    public static final String DEFAULT_NAME = "Unsorted";
    public static final String LEGACY_DEFAULT_NAME = "Default";

    private ResourceGroupTree() {
    }

    public static boolean isDefaultName(String name) {
        if (name == null) {
            return false;
        }
        String trimmed = name.trim();
        return DEFAULT_NAME.equalsIgnoreCase(trimmed)
                || LEGACY_DEFAULT_NAME.equalsIgnoreCase(trimmed);
    }

    public static boolean isDefault(ResourceGroupEntity group) {
        return group != null && isDefaultName(nameOf(group));
    }

    public static String nameOf(ResourceGroupEntity group) {
        if (group == null || group.getResourceGroupData() == null
                || group.getResourceGroupData().getHumanReadableId() == null) {
            return "";
        }
        return group.getResourceGroupData().getHumanReadableId();
    }

    public static ObjectId parentId(ResourceGroupEntity group) {
        return group == null ? null : group.refParentGroupObjectId();
    }

    public static ResourceGroupEntity find(List<ResourceGroupEntity> all, ObjectId id) {
        if (all == null || id == null) {
            return null;
        }
        for (ResourceGroupEntity group : all) {
            if (group != null && id.equals(group.getId())) {
                return group;
            }
        }
        return null;
    }

    public static List<ResourceGroupEntity> childrenOf(List<ResourceGroupEntity> all, ObjectId parentId) {
        List<ResourceGroupEntity> children = new ArrayList<>();
        if (all == null) {
            return children;
        }
        for (ResourceGroupEntity group : all) {
            if (group == null) {
                continue;
            }
            if (Objects.equals(parentId(group), parentId)) {
                children.add(group);
            }
        }
        return children;
    }

    /**
     * 0 for the virtual root ({@code groupId == null}); 1 for a stored root.
     */
    public static int depthOf(List<ResourceGroupEntity> all, ObjectId groupId) {
        if (groupId == null) {
            return 0;
        }
        Map<ObjectId, ResourceGroupEntity> byId = index(all);
        int depth = 0;
        ObjectId walk = groupId;
        Set<ObjectId> seen = new HashSet<>();
        while (walk != null && seen.add(walk)) {
            ResourceGroupEntity group = byId.get(walk);
            if (group == null) {
                break;
            }
            depth++;
            ObjectId parent = parentId(group);
            if (parent != null && parent.equals(walk)) {
                break;
            }
            walk = parent;
        }
        return Math.max(depth, 1);
    }

    /**
     * Extra levels below this node. A leaf is 0.
     */
    public static int heightOf(List<ResourceGroupEntity> all, ObjectId groupId) {
        return heightOf(all, groupId, new HashSet<>());
    }

    private static int heightOf(List<ResourceGroupEntity> all, ObjectId groupId, Set<ObjectId> seen) {
        if (groupId == null || !seen.add(groupId)) {
            return 0;
        }
        int height = 0;
        for (ResourceGroupEntity child : childrenOf(all, groupId)) {
            if (child.getId() == null) {
                continue;
            }
            height = Math.max(height, 1 + heightOf(all, child.getId(), seen));
        }
        return height;
    }

    public static boolean nameTakenAmongSiblings(List<ResourceGroupEntity> all, String name,
            ObjectId parentId, ObjectId excludeId) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String needle = name.trim();
        if (all == null) {
            return false;
        }
        for (ResourceGroupEntity group : all) {
            if (group == null) {
                continue;
            }
            if (excludeId != null && excludeId.equals(group.getId())) {
                continue;
            }
            if (!Objects.equals(parentId(group), parentId)) {
                continue;
            }
            if (nameOf(group).equalsIgnoreCase(needle)) {
                return true;
            }
        }
        return false;
    }

    public static boolean wouldCycle(List<ResourceGroupEntity> all, ObjectId groupId, ObjectId newParentId) {
        if (groupId == null || newParentId == null) {
            return false;
        }
        if (groupId.equals(newParentId)) {
            return true;
        }
        Map<ObjectId, ResourceGroupEntity> byId = index(all);
        ObjectId walk = newParentId;
        Set<ObjectId> seen = new HashSet<>();
        while (walk != null && seen.add(walk)) {
            if (groupId.equals(walk)) {
                return true;
            }
            ResourceGroupEntity node = byId.get(walk);
            if (node == null) {
                return false;
            }
            ObjectId parent = parentId(node);
            if (parent != null && parent.equals(walk)) {
                return true;
            }
            walk = parent;
        }
        return false;
    }

    public static String refuseCreate(List<ResourceGroupEntity> all, String name, ObjectId parentId) {
        if (name == null || name.isBlank()) {
            return "invalid_group";
        }
        if (isDefaultName(name)) {
            if (parentId != null) {
                return "default_immovable";
            }
            if (all != null) {
                for (ResourceGroupEntity existing : all) {
                    if (isDefault(existing)) {
                        return "group_exists";
                    }
                }
            }
        }
        if (parentId != null) {
            ResourceGroupEntity parent = find(all, parentId);
            if (parent == null) {
                return "invalid_parent";
            }
            if (isDefault(parent)) {
                return "default_no_children";
            }
        }
        if (nameTakenAmongSiblings(all, name, parentId, null)) {
            return "group_exists";
        }
        return null;
    }

    public static String refuseMove(List<ResourceGroupEntity> all, ResourceGroupEntity group, ObjectId newParentId) {
        if (group == null || group.getId() == null) {
            return "invalid_group";
        }
        if (isDefault(group) && newParentId != null) {
            return "default_immovable";
        }
        if (newParentId != null) {
            ResourceGroupEntity parent = find(all, newParentId);
            if (parent == null) {
                return "invalid_parent";
            }
            if (isDefault(parent)) {
                return "default_no_children";
            }
            if (wouldCycle(all, group.getId(), newParentId)) {
                return "cycle";
            }
        }
        if (nameTakenAmongSiblings(all, nameOf(group), newParentId, group.getId())) {
            return "group_exists";
        }
        return null;
    }

    public static String refuseRename(List<ResourceGroupEntity> all, ResourceGroupEntity group, String newName) {
        if (group == null || group.getId() == null) {
            return "invalid_group";
        }
        if (newName == null || newName.isBlank()) {
            return "invalid_group";
        }
        String trimmed = newName.trim();
        if (nameOf(group).equalsIgnoreCase(trimmed)) {
            return null;
        }
        if (isDefault(group)) {
            return "default_rename";
        }
        if (isDefaultName(trimmed)) {
            if (parentId(group) != null) {
                return "default_immovable";
            }
            if (all != null) {
                for (ResourceGroupEntity existing : all) {
                    if (existing != group && isDefault(existing)) {
                        return "group_exists";
                    }
                }
            }
        }
        if (nameTakenAmongSiblings(all, trimmed, parentId(group), group.getId())) {
            return "group_exists";
        }
        return null;
    }

    public static String refuseDelete(List<ResourceGroupEntity> all, ResourceGroupEntity group) {
        if (group == null || group.getId() == null) {
            return "invalid_group";
        }
        if (isDefault(group)) {
            return "default_immovable";
        }
        if (!childrenOf(all, group.getId()).isEmpty()) {
            return "has_children";
        }
        return null;
    }

    public static String errorMessage(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return switch (code) {
            case "group_exists" -> "A group with that name already exists under the same parent.";
            case "invalid_parent" -> "That parent group was not found.";
            case "cycle" -> "A group cannot be moved under itself or its descendants.";
            case "default_immovable" -> "The Unsorted group stays at the root.";
            case "default_no_children" -> "The Unsorted group cannot contain folders.";
            case "default_rename" -> "The Unsorted group cannot be renamed.";
            case "has_children" -> "Move or delete child groups first.";
            case "invalid_group" -> "Could not update that group.";
            case "invalid_name" -> "That name is not allowed.";
            case "name_taken" -> "That name is already in use.";
            case "invalid_item" -> "Could not update that item.";
            default -> null;
        };
    }

    private static Map<ObjectId, ResourceGroupEntity> index(List<ResourceGroupEntity> all) {
        Map<ObjectId, ResourceGroupEntity> byId = new HashMap<>();
        if (all == null) {
            return byId;
        }
        for (ResourceGroupEntity group : all) {
            if (group != null && group.getId() != null) {
                byId.put(group.getId(), group);
            }
        }
        return byId;
    }
}

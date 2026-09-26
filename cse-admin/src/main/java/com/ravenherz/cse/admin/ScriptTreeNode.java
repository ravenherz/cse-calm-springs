package com.ravenherz.cse.admin;

import java.util.List;

/**
 * One node in the script tree. A folder has no entity id.
 */
public final class ScriptTreeNode {

    private final String name;
    private final String entityId;
    private final boolean folder;
    private final boolean selected;
    private final List<ScriptTreeNode> children;

    public ScriptTreeNode(String name, String entityId, boolean folder, boolean selected,
            List<ScriptTreeNode> children) {
        this.name = name;
        this.entityId = entityId;
        this.folder = folder;
        this.selected = selected;
        this.children = children == null ? List.of() : List.copyOf(children);
    }

    public String getName() {
        return name;
    }

    public String getEntityId() {
        return entityId;
    }

    public boolean isFolder() {
        return folder;
    }

    public boolean isSelected() {
        return selected;
    }

    public List<ScriptTreeNode> getChildren() {
        return children;
    }
}

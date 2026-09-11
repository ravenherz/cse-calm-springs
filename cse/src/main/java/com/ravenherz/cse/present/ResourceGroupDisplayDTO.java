package com.ravenherz.cse.present;

import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.basic.SecurityData;
import com.ravenherz.cse.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ResourceGroupDisplayDTO {
    private String id;
    private String parentId;
    private String humanReadableId;
    private String pathLabel;
    private int depth;
    private int subtreeHeight;
    private boolean defaultGroup;
    private boolean virtual;
    private String href;
    private List<ResourceEntity> resources = new ArrayList<>();
    private List<ResourceTreeFile> treeFiles = new ArrayList<>();
    private List<ResourceGroupDisplayDTO> children = new ArrayList<>();
    private int ownFileCount;
    private long totalSize;
    private int descendantFileCount;
    private long descendantTotalSize;
    private boolean guestDenied;
    private SecurityData securityData = new SecurityData();
    private boolean accessCanEdit;
    private String categoryItemName;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getHumanReadableId() {
        return humanReadableId;
    }

    public void setHumanReadableId(String humanReadableId) {
        this.humanReadableId = humanReadableId;
    }

    public String getPathLabel() {
        return pathLabel;
    }

    public void setPathLabel(String pathLabel) {
        this.pathLabel = pathLabel;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public int getSubtreeHeight() {
        return subtreeHeight;
    }

    public void setSubtreeHeight(int subtreeHeight) {
        this.subtreeHeight = subtreeHeight;
    }

    public boolean isDefaultGroup() {
        return defaultGroup;
    }

    public void setDefaultGroup(boolean defaultGroup) {
        this.defaultGroup = defaultGroup;
    }

    public List<ResourceEntity> getResources() {
        return resources;
    }

    public void setResources(List<ResourceEntity> resources) {
        this.resources = resources == null ? new ArrayList<>() : resources;
    }

    public List<ResourceTreeFile> getTreeFiles() {
        return treeFiles;
    }

    public void setTreeFiles(List<ResourceTreeFile> treeFiles) {
        this.treeFiles = treeFiles == null ? new ArrayList<>() : treeFiles;
    }

    public boolean hasExpandableChildren() {
        return (children != null && !children.isEmpty())
                || (treeFiles != null && !treeFiles.isEmpty());
    }

    public List<ResourceGroupDisplayDTO> getChildren() {
        return children;
    }

    public void setChildren(List<ResourceGroupDisplayDTO> children) {
        this.children = children == null ? new ArrayList<>() : children;
    }

    public int getOwnFileCount() {
        return ownFileCount;
    }

    public void setOwnFileCount(int ownFileCount) {
        this.ownFileCount = Math.max(ownFileCount, 0);
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = Math.max(totalSize, 0);
    }

    public String getTotalSizeLabel() {
        return StringUtils.formatByteSize(totalSize);
    }

    public int getDescendantFileCount() {
        return descendantFileCount;
    }

    public void setDescendantFileCount(int descendantFileCount) {
        this.descendantFileCount = descendantFileCount;
    }

    public long getDescendantTotalSize() {
        return descendantTotalSize;
    }

    public void setDescendantTotalSize(long descendantTotalSize) {
        this.descendantTotalSize = descendantTotalSize;
    }

    public String getDescendantTotalSizeLabel() {
        return StringUtils.formatByteSize(descendantTotalSize);
    }

    public boolean containsInSubtree(String otherId) {
        if (otherId == null) {
            return false;
        }
        if (otherId.equals(id)) {
            return true;
        }
        if (children == null) {
            return false;
        }
        for (ResourceGroupDisplayDTO child : children) {
            if (child.containsInSubtree(otherId)) {
                return true;
            }
        }
        return false;
    }

    public boolean canReparentUnder(ResourceGroupDisplayDTO parent) {
        if (parent == null || parent.getId() == null) {
            return true;
        }
        return !containsInSubtree(parent.getId());
    }

    public boolean isUngrouped() {
        return "ungrouped".equals(id);
    }

    public boolean isVirtual() {
        return virtual;
    }

    public void setVirtual(boolean virtual) {
        this.virtual = virtual;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public boolean isCategoryNode() {
        return id != null && id.startsWith("category-");
    }

    public boolean canCreateChild() {
        return !isUngrouped() && !isDefaultGroup() && !isVirtual() && !isCategoryNode();
    }

    public boolean canCreateCategory() {
        return EditorTree.CATEGORIES_ID.equals(id);
    }

    public boolean canCreatePage() {
        return isCategoryNode();
    }

    public boolean canCreatePageMenu() {
        return canCreateCategory();
    }

    public String pageCreatePath() {
        return canCreatePage() ? EditorTree.pageCreatePath(categoryObjectId()) : "";
    }

    public String albumCreatePath() {
        return canCreatePage() ? EditorTree.albumCreatePath(categoryObjectId()) : "";
    }

    public boolean canDeleteCategory() {
        return isCategoryNode();
    }

    public boolean canEditCategory() {
        return isCategoryNode();
    }

    public String categoryObjectId() {
        if (!isCategoryNode()) {
            return "";
        }
        return id.substring(EditorTree.CATEGORY_PREFIX.length());
    }

    public String categoryEditPath() {
        if (!canEditCategory()) {
            return "";
        }
        return "/editor/category/edit?id=" + categoryObjectId();
    }

    public boolean canMove() {
        return !isUngrouped() && !isDefaultGroup() && !isVirtual() && !isCategoryNode();
    }

    public boolean canRename() {
        return canMove();
    }

    public boolean canDelete() {
        return canMove();
    }

    public boolean canAcceptFiles() {
        return !isUngrouped() && !isVirtual() && !isCategoryNode();
    }

    public boolean canEditAccess() {
        return canAcceptFiles() && !isDefaultGroup();
    }

    public boolean isLocked() {
        return isUngrouped() || isDefaultGroup() || isVirtual();
    }

    public boolean isGuestDenied() {
        return guestDenied;
    }

    public void setGuestDenied(boolean guestDenied) {
        this.guestDenied = guestDenied;
    }

    public SecurityData getSecurityData() {
        if (securityData == null) {
            securityData = new SecurityData();
        }
        return securityData;
    }

    public void setSecurityData(SecurityData securityData) {
        this.securityData = securityData == null ? new SecurityData() : securityData;
    }

    public boolean isAccessCanEdit() {
        return accessCanEdit;
    }

    public void setAccessCanEdit(boolean accessCanEdit) {
        this.accessCanEdit = accessCanEdit;
    }

    public String getCategoryItemName() {
        return categoryItemName;
    }

    public void setCategoryItemName(String categoryItemName) {
        this.categoryItemName = categoryItemName == null || categoryItemName.isBlank()
                ? null : categoryItemName.trim();
    }

    public String embedTag() {
        return isEmbeddable() ? "cse-category" : "";
    }

    public String embedId() {
        return categoryItemName == null ? "" : categoryItemName;
    }

    public boolean isEmbeddable() {
        return isCategoryNode() && categoryItemName != null && !categoryItemName.isBlank();
    }
}

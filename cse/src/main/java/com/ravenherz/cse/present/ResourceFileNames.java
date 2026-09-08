package com.ravenherz.cse.present;

import com.ravenherz.cse.dal.dto.basic.enums.ResourceType;

/**
 * Public-path filename changes for catalog resources.
 */
public final class ResourceFileNames {

    private ResourceFileNames() {
    }

    public static String nextPathPublic(String currentPath, String requestedName, ResourceType type) {
        if (currentPath == null || currentPath.isBlank() || requestedName == null) {
            return null;
        }
        String next = requestedName.trim();
        if (next.isEmpty() || next.contains("/") || next.contains("\\") || next.contains("..")
                || next.indexOf('\0') >= 0) {
            return null;
        }
        String currentName = fileName(currentPath);
        String ext = extension(currentName);
        if (!next.contains(".")) {
            if (ext.isEmpty()) {
                return null;
            }
            next = next + "." + ext;
        }
        ResourceType nextType = ResourceType.getByFileName(next);
        if (nextType == ResourceType.INVALID) {
            return null;
        }
        if (type != null && nextType != type) {
            return null;
        }
        int slash = currentPath.lastIndexOf('/');
        String dir = slash >= 0 ? currentPath.substring(0, slash + 1) : "/";
        return dir + next;
    }

    public static String fileName(String pathPublic) {
        if (pathPublic == null || pathPublic.isEmpty()) {
            return "";
        }
        int slash = pathPublic.lastIndexOf('/');
        return slash >= 0 ? pathPublic.substring(slash + 1) : pathPublic;
    }

    private static String extension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1);
    }
}

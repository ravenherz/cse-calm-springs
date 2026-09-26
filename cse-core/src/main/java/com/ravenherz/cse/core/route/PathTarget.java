package com.ravenherz.cse.core.route;

/**
 * Where a context-relative path should send the browser. The WAR prepends the servlet context.
 */
public final class PathTarget {

    public static final int MOVED_PERMANENTLY = 301;
    public static final int FOUND = 302;

    private final String targetPath;
    private final int status;
    private final boolean preserveQuery;

    public PathTarget(String targetPath, int status, boolean preserveQuery) {
        if (targetPath == null || targetPath.isBlank() || !targetPath.startsWith("/")) {
            throw new IllegalArgumentException("target path must start with /");
        }
        if (status != MOVED_PERMANENTLY && status != FOUND) {
            throw new IllegalArgumentException("status must be 301 or 302");
        }
        this.targetPath = targetPath;
        this.status = status;
        this.preserveQuery = preserveQuery;
    }

    public String targetPath() {
        return targetPath;
    }

    public int status() {
        return status;
    }

    public boolean preserveQuery() {
        return preserveQuery;
    }
}

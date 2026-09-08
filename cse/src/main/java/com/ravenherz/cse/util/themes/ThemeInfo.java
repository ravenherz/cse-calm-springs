package com.ravenherz.cse.util.themes;

import java.util.ArrayList;
import java.util.List;

public final class ThemeInfo {

    private final String id;
    private final String title;
    private final String author;
    private final String description;
    private final String shell;
    private final String defaultSchema;
    private final List<String> schemas;
    private final boolean builtin;
    private final int sortOrder;
    private final boolean hasPreview;

    public ThemeInfo(String id, String title, String author, String description, String shell,
            String defaultSchema, List<String> schemas, boolean builtin) {
        this(id, title, author, description, shell, defaultSchema, schemas, builtin, 0, false);
    }

    public ThemeInfo(String id, String title, String author, String description, String shell,
            String defaultSchema, List<String> schemas, boolean builtin, int sortOrder, boolean hasPreview) {
        this.id = id;
        this.title = title == null || title.isBlank() ? id : title;
        this.author = author;
        this.description = description;
        this.shell = shell;
        this.defaultSchema = defaultSchema;
        this.schemas = schemas == null ? List.of() : List.copyOf(schemas);
        this.builtin = builtin;
        this.sortOrder = sortOrder;
        this.hasPreview = hasPreview;
    }

    public ThemeInfo withSortOrder(int sortOrder) {
        return new ThemeInfo(id, title, author, description, shell, defaultSchema, schemas, builtin,
                sortOrder, hasPreview);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getDescription() {
        return description;
    }

    public String getShell() {
        return shell;
    }

    public String getDefaultSchema() {
        return defaultSchema;
    }

    public List<String> getSchemas() {
        return schemas;
    }

    public boolean isBuiltin() {
        return builtin;
    }

    public boolean isOwnShell() {
        return ThemeCatalog.isOwnShell(shell);
    }

    public boolean isJsShell() {
        return ThemeCatalog.isJsShell(shell);
    }

    /** Tile label: {@code js} or {@code thymeleaf}. */
    public String getShellKind() {
        return isJsShell() ? "js" : "thymeleaf";
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean hasPreview() {
        return hasPreview;
    }

    public boolean hasSchema(String schema) {
        if (schema == null) {
            return false;
        }
        for (String candidate : schemas) {
            if (schema.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    public String schemasLabel() {
        if (schemas.isEmpty()) {
            return "—";
        }
        return String.join(", ", schemas);
    }

    public String getSchemasLabel() {
        return schemasLabel();
    }

    public List<String> schemaList() {
        return new ArrayList<>(schemas);
    }
}

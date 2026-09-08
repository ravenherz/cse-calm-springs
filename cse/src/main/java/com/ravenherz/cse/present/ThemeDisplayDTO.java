package com.ravenherz.cse.present;

import com.ravenherz.cse.util.StringUtils;
import com.ravenherz.cse.util.themes.ThemeCatalog;

import java.util.ArrayList;
import java.util.List;

public class ThemeDisplayDTO {

    private String id;
    private String themeId;
    private String title;
    private String author;
    private String description;
    private String shell;
    private String defaultSchema;
    private String originalFilename;
    private long sizeInBytes;
    private List<String> schemas = new ArrayList<>();
    private boolean active;
    private boolean builtin;
    private boolean hasPreview;
    private int sortOrder;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getThemeId() {
        return themeId;
    }

    public void setThemeId(String themeId) {
        this.themeId = themeId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getShell() {
        return shell;
    }

    public void setShell(String shell) {
        this.shell = shell;
    }

    public String getDefaultSchema() {
        return defaultSchema;
    }

    public void setDefaultSchema(String defaultSchema) {
        this.defaultSchema = defaultSchema;
    }

    /** Tile label: {@code js} or {@code thymeleaf}. */
    public String getShellKind() {
        return ThemeCatalog.isJsShell(shell) ? "js" : "thymeleaf";
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    public void setSizeInBytes(long sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }

    public String getSizeLabel() {
        return StringUtils.formatByteSize(sizeInBytes);
    }

    public List<String> getSchemas() {
        return schemas;
    }

    public void setSchemas(List<String> schemas) {
        this.schemas = schemas == null ? new ArrayList<>() : schemas;
    }

    public String getSchemasLabel() {
        if (schemas == null || schemas.isEmpty()) {
            return "—";
        }
        return String.join(", ", schemas);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isBuiltin() {
        return builtin;
    }

    public void setBuiltin(boolean builtin) {
        this.builtin = builtin;
    }

    public boolean isHasPreview() {
        return hasPreview;
    }

    public void setHasPreview(boolean hasPreview) {
        this.hasPreview = hasPreview;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}

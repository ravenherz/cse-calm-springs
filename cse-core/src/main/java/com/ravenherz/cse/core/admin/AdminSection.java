package com.ravenherz.cse.core.admin;

import java.util.List;

/**
 * One generic editor screen. The handler reads this value and does not see the feature entity.
 */
public final class AdminSection {

    private final String id;
    private final String title;
    private final List<AdminField> fields;
    private final AdminPresentation presentation;

    public AdminSection(String id, String title, List<AdminField> fields) {
        this(id, title, fields, null);
    }

    public AdminSection(String id, String title, List<AdminField> fields, AdminPresentation presentation) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("section id is required");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("section title is required");
        }
        if (fields == null || fields.isEmpty()) {
            throw new IllegalArgumentException("section requires fields");
        }
        this.id = id;
        this.title = title;
        this.fields = List.copyOf(fields);
        this.presentation = presentation == null
                ? new AdminPresentation("", TreeGlyph.FILE) : presentation;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public List<AdminField> fields() {
        return fields;
    }

    public AdminPresentation presentation() {
        return presentation;
    }

    public String treeLabel(java.util.Map<String, String> row) {
        String label = presentation.treeLabel(fields, row);
        return label.isBlank() ? title : label;
    }
}

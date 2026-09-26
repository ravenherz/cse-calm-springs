package com.ravenherz.cse.core.admin;

import java.util.List;
import java.util.Map;

/**
 * One generic editor screen. The handler reads this value and does not see the feature entity.
 */
public final class AdminSection {

    private final String id;
    private final String title;
    private final List<AdminField> fields;
    private final AdminPresentation presentation;
    private final String runsHref;
    private final String runHref;

    public AdminSection(String id, String title, List<AdminField> fields) {
        this(id, title, fields, null, null, null);
    }

    public AdminSection(String id, String title, List<AdminField> fields, AdminPresentation presentation) {
        this(id, title, fields, presentation, null, null);
    }

    public AdminSection(String id, String title, List<AdminField> fields, AdminPresentation presentation,
            String runsHref, String runHref) {
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
        this.runsHref = blank(runsHref);
        this.runHref = blank(runHref);
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

    public String runsHref() {
        return runsHref;
    }

    public String runHref() {
        return runHref;
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public String treeLabel(Map<String, String> row) {
        String label = presentation.treeLabel(fields, row);
        return label.isBlank() ? title : label;
    }

    public String cardImage(Map<String, String> row) {
        if (row == null) {
            return null;
        }
        for (AdminField field : fields) {
            if (field.cardPlace() != CardPlace.IMAGE) {
                continue;
            }
            String value = row.get(field.name());
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}

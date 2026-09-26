package com.ravenherz.cse.core.admin;

import java.util.List;
import java.util.Map;

/**
 * How a module asks the catalog to show its rows. The host renders this and does not
 * know the feature. Card tags come from {@link CardPlace} on each field.
 */
public final class AdminPresentation {

    private final String treePattern;
    private final TreeGlyph treeGlyph;

    public AdminPresentation(String treePattern, TreeGlyph treeGlyph) {
        this.treePattern = treePattern == null ? "" : treePattern;
        this.treeGlyph = treeGlyph == null ? TreeGlyph.FILE : treeGlyph;
    }

    public TreeGlyph treeGlyph() {
        return treeGlyph;
    }

    public String treeLabel(List<AdminField> fields, Map<String, String> row) {
        if (treePattern.isBlank() || fields == null) {
            return "";
        }
        String text = treePattern;
        for (AdminField field : fields) {
            String value = row == null ? "" : row.get(field.name());
            text = text.replace("{" + field.name() + "}", value == null ? "" : value);
        }
        return text.trim();
    }
}

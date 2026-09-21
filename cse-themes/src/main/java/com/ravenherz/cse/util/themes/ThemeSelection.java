package com.ravenherz.cse.util.themes;

public final class ThemeSelection {

    public static final String FALLBACK_THEME = "modern";
    public static final String FALLBACK_SHELL = "modern";
    public static final String FALLBACK_SCHEMA = "linen";

    private final String cssId;
    private final String schemaId;
    private final String shellId;

    public ThemeSelection(String cssId, String schemaId, String shellId) {
        this.cssId = cssId;
        this.schemaId = schemaId;
        this.shellId = shellId;
    }

    public static ThemeSelection fallback() {
        return new ThemeSelection(FALLBACK_THEME, FALLBACK_SCHEMA, FALLBACK_SHELL);
    }

    public String getCssId() {
        return cssId;
    }

    public String getSchemaId() {
        return schemaId;
    }

    public String getShellId() {
        return shellId;
    }
}

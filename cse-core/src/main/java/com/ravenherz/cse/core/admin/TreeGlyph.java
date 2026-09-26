package com.ravenherz.cse.core.admin;

/**
 * Icon the host draws for a catalog leaf. The module names the glyph. The host owns the SVG.
 */
public enum TreeGlyph {
    FILE,
    ARROW,
    JS;

    public String token() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}

package com.ravenherz.cse.scripting;

import com.ravenherz.cse.dal.EntityId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ScriptRulesTest {

    @Test
    void idMustStartWithALetter() {
        assertNull(ScriptRules.idError("on-save"));
        assertEquals("on-save", ScriptRules.normalizeId(" On-Save "));
        assertEquals("Use a lowercase id that starts with a letter", ScriptRules.idError("2024"));
        assertEquals("Use a lowercase id that starts with a letter", ScriptRules.idError("has space"));
    }

    @Test
    void folderKeepsSlashSegments() {
        assertEquals("", ScriptRules.normalizeFolder("  "));
        assertEquals("hooks/pages", ScriptRules.normalizeFolder("/Hooks/pages/"));
        assertNull(ScriptRules.normalizeFolder("hooks/../pages"));
        assertEquals("Use a folder like hooks/pages", ScriptRules.folderError("hooks/../pages"));
        assertNull(ScriptRules.folderError(null));
    }

    @Test
    void sourceHasALengthCap() {
        assertNull(ScriptRules.sourceError("function onSave() {}"));
        assertEquals("Script is too long", ScriptRules.sourceError("x".repeat(ScriptRules.MAX_SOURCE + 1)));
    }
}

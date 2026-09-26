package com.ravenherz.cse.scripting;

import com.ravenherz.cse.dal.EntityId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptTreeTest {

    @Test
    void foldersGroupScriptsAndMarkTheSelection() {
        EntityId footer = EntityId.generate();
        EntityId hook = EntityId.generate();
        ScriptEntity footerScript = new ScriptEntity("footer", "", "1", null);
        footerScript.setId(footer);
        ScriptEntity hookScript = new ScriptEntity("on-save", "hooks/pages", "2", null);
        hookScript.setId(hook);

        List<ScriptTree.Branch> roots = ScriptTree.of(List.of(hookScript, footerScript), hook.toHexString());

        assertEquals(2, roots.size());
        assertEquals("hooks", roots.get(0).name());
        assertTrue(roots.get(0).folder());
        assertEquals("footer", roots.get(1).name());
        assertFalse(roots.get(1).folder());
        ScriptTree.Branch pages = roots.get(0).children().get(0);
        assertEquals("pages", pages.name());
        ScriptTree.Branch file = pages.children().get(0);
        assertEquals("on-save", file.name());
        assertTrue(file.selected());
        assertEquals(hook.toHexString(), file.entityId());
    }
}

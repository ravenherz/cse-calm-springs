package com.ravenherz.cse.admin;

import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.security.AccountAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EditorScriptingControllerTest {

    @Test
    void editorAppliesChromeAndTheTree() throws Exception {
        ScriptEditorPage page = new ScriptEditorPage(List.of(), "", "", "", "", "", false);
        ExtendedModelMap model = new ExtendedModelMap();
        EditorScriptingController controller = controller(account(), page, List.of());

        String view = controller.editor(null, null, new MockHttpServletRequest(), null);

        assertEquals("redirect:/editor/sections/scripts", view);
    }

    @Test
    void runsListsTheDeskRows() throws Exception {
        ScriptRunRow row = new ScriptRunRow("on-save", "Done", "2026-09-26 12:00:00", "");
        ExtendedModelMap model = new ExtendedModelMap();
        EditorScriptingController controller = controller(account(), null, List.of(row));

        String view = controller.runs(model, new MockHttpServletRequest(), null);

        assertEquals("/admin/editor-scripting", view);
        assertEquals("runs", model.get("scriptTab"));
        assertEquals(List.of(row), model.get("scriptRuns"));
    }

    @Test
    void saveStaysOnTheFormWhenTheIdIsRejected() throws Exception {
        ExtendedModelMap model = new ExtendedModelMap();
        EditorScriptingController controller = controller(account(), null, List.of());

        String view = controller.save("", "2024", "hooks", "var x = 1;", model,
                new MockHttpServletRequest(), null);

        assertEquals("/admin/editor-scripting", view);
        assertEquals("Use a lowercase id that starts with a letter", model.get("scriptError"));
        assertEquals(Boolean.TRUE, model.get("scriptForm"));
    }

    @Test
    void saveRedirectsAfterTheRowIsStored() throws Exception {
        EditorScriptingController controller = controller(account(), null, List.of());

        String view = controller.save("", "on-save", "hooks", "function onSave() {}",
                new ExtendedModelMap(), new MockHttpServletRequest(), null);

        assertEquals("redirect:/editor/scripting?script=aaaaaaaaaaaaaaaaaaaaaaaa", view);
    }

    @Test
    void missingSessionStops() throws Exception {
        EditorScriptingController controller = controller(null, null, List.of());

        assertNull(controller.editor(null, null, new MockHttpServletRequest(), null));
        assertNull(controller.runs(new ExtendedModelMap(), new MockHttpServletRequest(), null));
    }

    private static EditorScriptingController controller(AccountEntity account, ScriptEditorPage open,
            List<ScriptRunRow> runs) {
        AccountAccessor accessor = (request, response) -> account;
        EditorChrome chrome = (model, accessorAccount) -> model.addAttribute("username",
                accessorAccount.getAccountData().getLogin());
        ScriptDesk desk = new ScriptDesk() {
            @Override
            public ScriptEditorPage open(String entityId, boolean creating) {
                return open;
            }

            @Override
            public ScriptEditorPage save(String entityId, String scriptId, String folder, String source,
                    EntityId actorId) {
                if (scriptId == null || !scriptId.matches("^[a-z][a-z0-9-]{0,62}$")) {
                    return new ScriptEditorPage(List.of(), entityId, scriptId, folder, source,
                            "Use a lowercase id that starts with a letter", true);
                }
                return new ScriptEditorPage(List.of(), "aaaaaaaaaaaaaaaaaaaaaaaa", scriptId, folder, source, "", true);
            }

            @Override
            public void delete(String entityId) {
            }

            @Override
            public String run(String entityId) {
                return null;
            }

            @Override
            public List<ScriptRunRow> runs() {
                return runs;
            }
        };
        return new EditorScriptingController(accessor, chrome, desk);
    }

    private static AccountEntity account() {
        AccountData data = new AccountData();
        data.setLogin("ada");
        return new AccountEntity(data);
    }
}

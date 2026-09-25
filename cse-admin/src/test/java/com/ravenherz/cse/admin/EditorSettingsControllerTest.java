package com.ravenherz.cse.admin;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.security.AccountAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EditorSettingsControllerTest {

    @Test
    void pageAppliesChromeThenFillsTheForm() throws Exception {
        List<String> steps = new ArrayList<>();
        ExtendedModelMap model = new ExtendedModelMap();
        EditorSettingsController controller = controller(account(), steps);

        String view = controller.settingsPage(model, null, null);

        assertEquals("/admin/editor-settings", view);
        assertEquals(List.of("chrome", "fill"), steps);
        assertEquals("ada", model.get("username"));
    }

    @Test
    void saveWritesBeforeChromeAndFill() throws Exception {
        List<String> steps = new ArrayList<>();
        EditorSettingsController controller = controller(account(), steps);

        String view = controller.saveSettings(Map.of("view::title", "Hello"), new ExtendedModelMap(), null, null);

        assertEquals("/admin/editor-settings", view);
        assertEquals(List.of("save", "chrome", "fill"), steps);
    }

    @Test
    void pageStopsWhenThereIsNoSession() throws Exception {
        EditorSettingsController controller = controller(null, new ArrayList<>());

        assertNull(controller.settingsPage(new ExtendedModelMap(), null, null));
    }

    private static EditorSettingsController controller(AccountEntity account, List<String> steps) {
        AccountAccessor accessor = (request, response) -> account;
        EditorChrome chrome = (model, accessorAccount) -> {
            steps.add("chrome");
            model.addAttribute("username", accessorAccount.getAccountData().getLogin());
        };
        SettingsForm form = new SettingsForm() {
            @Override
            public void fill(Model model) {
                steps.add("fill");
            }

            @Override
            public void save(Map<String, String> params, Model model) {
                steps.add("save");
            }
        };
        return new EditorSettingsController(accessor, chrome, form);
    }

    private static AccountEntity account() {
        AccountData data = new AccountData();
        data.setLogin("ada");
        return new AccountEntity(data);
    }
}

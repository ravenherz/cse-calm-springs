package com.ravenherz.cse.admin;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.security.AccountAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class EditorInstanceControllerTest {

    @Test
    void pageAppliesChromeAndKeepsTheSnapshot() throws Exception {
        Object snapshot = new Object();
        ExtendedModelMap model = new ExtendedModelMap();
        EditorInstanceController controller = controller(account(), snapshot);

        String view = controller.page(model, null, null);

        assertEquals("/admin/editor-instance", view);
        assertSame(snapshot, model.get("snapshot"));
        assertEquals("ada", model.get("username"));
    }

    @Test
    void pageStopsWhenThereIsNoSession() throws Exception {
        EditorInstanceController controller = controller(null, "snapshot");

        assertNull(controller.page(new ExtendedModelMap(), null, null));
    }

    @Test
    void snapshotEndpointReturnsTheCapture() {
        Object snapshot = new Object();
        EditorInstanceController controller = controller(account(), snapshot);

        assertSame(snapshot, controller.snapshot().getBody());
    }

    private static EditorInstanceController controller(AccountEntity account, Object snapshot) {
        AccountAccessor accessor = (request, response) -> account;
        EditorChrome chrome = (model, accessorAccount) -> model.addAttribute("username",
                accessorAccount.getAccountData().getLogin());
        InstanceCapture capture = () -> snapshot;
        return new EditorInstanceController(accessor, chrome, capture);
    }

    private static AccountEntity account() {
        AccountData data = new AccountData();
        data.setLogin("ada");
        return new AccountEntity(data);
    }
}

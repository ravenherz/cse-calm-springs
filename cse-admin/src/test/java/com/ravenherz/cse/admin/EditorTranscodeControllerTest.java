package com.ravenherz.cse.admin;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.security.AccountAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class EditorTranscodeControllerTest {

    @Test
    void pageAppliesChromeAndKeepsTheQueue() throws Exception {
        TranscodeQueue.QueueSnapshot snapshot = new TranscodeQueue.QueueSnapshot(
                1, 0, 0, 0, 1, 0, List.of());
        ExtendedModelMap model = new ExtendedModelMap();
        EditorTranscodeController controller = controller(account(), snapshot);

        String view = controller.page(model, new MockHttpServletRequest(), null);

        assertEquals("/admin/editor-transcode", view);
        assertSame(snapshot, model.get("queue"));
        assertEquals("ada", model.get("username"));
    }

    @Test
    void pageStopsWhenThereIsNoSession() throws Exception {
        EditorTranscodeController controller = controller(null, null);

        assertNull(controller.page(new ExtendedModelMap(), new MockHttpServletRequest(), null));
    }

    private static EditorTranscodeController controller(AccountEntity account,
            TranscodeQueue.QueueSnapshot snapshot) {
        AccountAccessor accessor = (request, response) -> account;
        EditorChrome chrome = (model, accessorAccount) -> model.addAttribute("username",
                accessorAccount.getAccountData().getLogin());
        TranscodeQueue queue = request -> snapshot;
        return new EditorTranscodeController(accessor, chrome, queue);
    }

    private static AccountEntity account() {
        AccountData data = new AccountData();
        data.setLogin("ada");
        return new AccountEntity(data);
    }
}

package com.ravenherz.cse.admin;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.security.AccountAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EditorLogsControllerTest {

    @Test
    void pageShowsTheWindowForASignedInAccount() throws Exception {
        EditorLogsController controller = controller(account("ada"), window());
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.page(model, null, null);

        assertEquals("/admin/editor-logs", view);
        assertEquals("ada", model.get("username"));
        assertEquals(true, model.get("logFound"));
        assertEquals("/logs/cse.log", model.get("logPath"));
        assertEquals("line", model.get("logText"));
        assertEquals("256 KB", model.get("logWindowLabel"));
    }

    @Test
    void pageStopsWhenThereIsNoSession() throws Exception {
        EditorLogsController controller = controller(null, window());

        assertNull(controller.page(new ExtendedModelMap(), null, null));
    }

    @Test
    void tailAndDownloadUseTheSameWindow() throws Exception {
        EditorLogsController controller = controller(account("ada"), window());

        ResponseEntity<Map<String, Object>> tail = controller.tail();
        assertEquals(true, tail.getBody().get("found"));
        assertEquals("line", tail.getBody().get("text"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.download(response);
        assertEquals("attachment; filename=\"cse.log\"", response.getHeader("Content-Disposition"));
        assertEquals("line", response.getContentAsString());
    }

    private static EditorLogsController controller(AccountEntity account, LogTail.Snapshot snapshot) {
        AccountAccessor accessor = (request, response) -> account;
        LogTail tail = new LogTail() {
            @Override
            public String fileName() {
                return "cse.log";
            }

            @Override
            public String windowLabel() {
                return "256 KB";
            }

            @Override
            public Snapshot read() {
                return snapshot;
            }
        };
        return new EditorLogsController(accessor, tail);
    }

    private static AccountEntity account(String login) {
        AccountData data = new AccountData();
        data.setLogin(login);
        return new AccountEntity(data);
    }

    private static LogTail.Snapshot window() {
        return new LogTail.Snapshot("/logs/cse.log", "line", 4, false, List.of("/logs/cse.log"), null);
    }
}

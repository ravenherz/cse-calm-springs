package com.ravenherz.cse.admin;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.security.AccountAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ExtendedModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EditorSiteDataControllerTest {

    @Test
    void pageOpensTheDeskForASignedInAccount() throws Exception {
        ExtendedModelMap model = new ExtendedModelMap();
        EditorSiteDataController controller = controller(account(), "/admin/editor-site-data");

        String view = controller.page(model, new MockHttpServletRequest(), new MockHttpServletResponse());

        assertEquals("/admin/editor-site-data", view);
    }

    @Test
    void pageStopsWhenThereIsNoSession() throws Exception {
        EditorSiteDataController controller = controller(null, "/admin/editor-site-data");

        assertNull(controller.page(new ExtendedModelMap(), new MockHttpServletRequest(),
                new MockHttpServletResponse()));
    }

    private static EditorSiteDataController controller(AccountEntity account, String view) {
        AccountAccessor accessor = (request, response) -> account;
        SiteDataDesk desk = new SiteDataDesk() {
            @Override
            public String open(org.springframework.ui.Model model, jakarta.servlet.http.HttpServletRequest request,
                    jakarta.servlet.http.HttpServletResponse response, String error) {
                return view;
            }

            @Override
            public void export(jakarta.servlet.http.HttpServletRequest request,
                    jakarta.servlet.http.HttpServletResponse response) {
            }

            @Override
            public String importArchive(org.springframework.web.multipart.MultipartFile file, String confirm,
                    org.springframework.ui.Model model, jakarta.servlet.http.HttpServletRequest request,
                    jakarta.servlet.http.HttpServletResponse response) {
                return view;
            }
        };
        return new EditorSiteDataController(accessor, desk);
    }

    private static AccountEntity account() {
        AccountData data = new AccountData();
        data.setLogin("ada");
        return new AccountEntity(data);
    }
}

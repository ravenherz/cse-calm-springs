package com.ravenherz.cse.security;

import com.ravenherz.cse.controller.AuthSupport;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EditorGateMvcTest.ProbeController.class)
@Import(SecurityConfig.class)
class EditorGateMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthSupport authSupport;

    @Test
    void guestHtmlEditorRedirects401() throws Exception {
        when(authSupport.getAccessor(any(), any())).thenReturn(null);
        mockMvc.perform(get("/editor/gate-probe"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/?error=401"));
    }

    @Test
    void guestLogTailIsJson401() throws Exception {
        when(authSupport.getAccessor(any(), any())).thenReturn(null);
        mockMvc.perform(get("/editor/logs/tail")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void memberCannotOpenEditor() throws Exception {
        when(authSupport.getAccessor(any(), any())).thenReturn(account(SecurityLevel.ACTIVE_USER));
        mockMvc.perform(get("/editor/gate-probe"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/?error=403"));
    }

    @Test
    void moderatorCannotOpenEditor() throws Exception {
        when(authSupport.getAccessor(any(), any())).thenReturn(account(SecurityLevel.MODERATOR));
        mockMvc.perform(get("/editor/gate-probe"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/?error=403"));
    }

    @Test
    void adminCanOpenEditor() throws Exception {
        when(authSupport.getAccessor(any(), any())).thenReturn(account(SecurityLevel.ADMIN));
        mockMvc.perform(get("/editor/gate-probe"))
                .andExpect(status().isOk());
    }

    @Test
    void ownerCanOpenEditor() throws Exception {
        when(authSupport.getAccessor(any(), any())).thenReturn(account(SecurityLevel.OWNER));
        mockMvc.perform(get("/editor/gate-probe"))
                .andExpect(status().isOk());
    }

    private static AccountEntity account(SecurityLevel level) {
        AccountEntity entity = new AccountEntity(new AccountData("user", "hash", "u@example.com", level));
        entity.setId(new ObjectId());
        return entity;
    }

    @Controller
    static class ProbeController {

        @GetMapping("/editor/gate-probe")
        @ResponseBody
        String probe() {
            return "ok";
        }
    }
}

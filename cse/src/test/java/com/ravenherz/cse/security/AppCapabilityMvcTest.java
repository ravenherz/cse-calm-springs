package com.ravenherz.cse.security;

import com.ravenherz.cse.controller.AuthSupport;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AppCapabilityMvcTest.ProbeController.class)
@Import(SecurityConfig.class)
class AppCapabilityMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthSupport authSupport;

    @MockitoBean
    private CapabilityService capabilityService;

    @BeforeEach
    void stubCapabilities() {
        when(capabilityService.allows(any(), any())).thenReturn(true);
        when(capabilityService.allowsApp(any(), any())).thenReturn(true);
        when(capabilityService.canOpenEditor(any())).thenReturn(true);
    }

    @Test
    void guestDeniedAppUsesSiteErrorPageNotLogin() throws Exception {
        when(authSupport.getAccessor(any(), any())).thenReturn(null);
        when(capabilityService.allowsApp(any(), eq("projects"))).thenReturn(false);
        mockMvc.perform(get("/apps/projects/"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/?error=403"));
    }

    @Test
    void signedInDeniedAppUsesSiteErrorPageNotTomcat() throws Exception {
        when(authSupport.getAccessor(any(), any())).thenReturn(account());
        when(capabilityService.allowsApp(any(), eq("projects"))).thenReturn(false);
        mockMvc.perform(get("/apps/projects/"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/?error=403"));
    }

    @Test
    void jsonDeniedAppIs403Body() throws Exception {
        when(authSupport.getAccessor(any(), any())).thenReturn(null);
        when(capabilityService.allowsApp(any(), eq("projects"))).thenReturn(false);
        mockMvc.perform(get("/apps/projects/issues")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Not allowed")));
    }

    @Test
    void allowedAppPasses() throws Exception {
        when(authSupport.getAccessor(any(), any())).thenReturn(null);
        mockMvc.perform(get("/apps/projects/"))
                .andExpect(status().isOk())
                .andExpect(content().string("projects"));
    }

    private static AccountEntity account() {
        AccountEntity entity = new AccountEntity(
                new AccountData("ada", "hash", "ada@example.com", SecurityLevel.ACTIVE_USER));
        entity.setId(new ObjectId());
        return entity;
    }

    @Controller
    static class ProbeController {

        @GetMapping("/apps/projects/")
        @ResponseBody
        String projects() {
            return "projects";
        }

        @GetMapping("/apps/projects/issues")
        @ResponseBody
        String issues() {
            return "[]";
        }
    }
}

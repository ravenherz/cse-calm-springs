package com.ravenherz.cse.engine.install;

import com.ravenherz.cse.install.InstallService;
import com.ravenherz.cse.install.SiteReady;

import com.ravenherz.cse.controller.AuthSupport;
import com.ravenherz.cse.util.AuthRateLimiter;
import com.ravenherz.cse.security.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InstallController.class)
@Import(SecurityConfig.class)
class InstallControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SiteReady siteReady;

    @MockitoBean
    private InstallService installService;

    @MockitoBean
    private AuthRateLimiter authRateLimiter;

    @MockitoBean
    private AuthSupport authSupport;

    @MockitoBean
    private com.ravenherz.cse.security.CapabilityService capabilityService;

    @BeforeEach
    void allowInstallPosts() {
        when(authRateLimiter.allow(any())).thenReturn(true);
        when(authSupport.clientIp(any())).thenReturn("127.0.0.1");
        when(capabilityService.allows(any(), any())).thenReturn(true);
        when(capabilityService.allowsApp(any(), any())).thenReturn(true);
        when(capabilityService.canOpenEditor(any())).thenReturn(true);
    }

    @Test
    void statusIs404WhenConfigured() throws Exception {
        when(siteReady.isConfigured()).thenReturn(true);
        mockMvc.perform(get("/install/status"))
                .andExpect(status().isNotFound());
    }

    @Test
    void statusOkWhenUnconfigured() throws Exception {
        when(siteReady.isConfigured()).thenReturn(false);
        when(installService.status()).thenReturn(Map.of(
                "mongoReady", false,
                "mongoFromEnv", false,
                "hasOwner", false,
                "siteTitle", ""));
        mockMvc.perform(get("/install/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mongoReady").value(false));
    }

    @Test
    void mongoPostWithoutCsrfIsForbidden() throws Exception {
        when(siteReady.isConfigured()).thenReturn(false);
        mockMvc.perform(post("/install/mongo")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void mongoPostWithCsrfWhenConfiguredIs404() throws Exception {
        when(siteReady.isConfigured()).thenReturn(true);
        mockMvc.perform(post("/install/mongo")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void statusWritesCsrfCookieWhenUnconfigured() throws Exception {
        when(siteReady.isConfigured()).thenReturn(false);
        when(installService.status()).thenReturn(Map.of(
                "mongoReady", false,
                "mongoFromEnv", false,
                "hasOwner", false,
                "siteTitle", ""));
        mockMvc.perform(get("/install/status"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false));
    }

    @Test
    void mongoPostIsRateLimited() throws Exception {
        when(siteReady.isConfigured()).thenReturn(false);
        when(authRateLimiter.allow(any())).thenReturn(false);
        mockMvc.perform(post("/install/mongo")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.ok").value(false));
    }
}

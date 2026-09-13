package com.ravenherz.cse.security;

import com.ravenherz.cse.controller.AuthSupport;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CsrfPhase3MvcTest.ProbeController.class)
@Import(SecurityConfig.class)
class CsrfPhase3MvcTest {

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
    void getWritesXsrfCookie() throws Exception {
        mockMvc.perform(get("/csrf-probe"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false))
                .andExpect(header().exists("X-XSRF-TOKEN"));
    }

    @Test
    void postWithoutTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/csrf-probe")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void formPostWithCookieAndParamSucceeds() throws Exception {
        String token = xsrfCookie();
        mockMvc.perform(post("/csrf-probe")
                        .cookie(new Cookie("XSRF-TOKEN", token))
                        .param("_csrf", token)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void postWithTokenSucceeds() throws Exception {
        mockMvc.perform(post("/csrf-probe")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void jsonPostWithCookieHeaderSucceedsTwice() throws Exception {
        String token = xsrfCookie();
        mockMvc.perform(post("/csrf-probe")
                        .cookie(new Cookie("XSRF-TOKEN", token))
                        .header("X-XSRF-TOKEN", token)
                        .header("X-Requested-With", "XMLHttpRequest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
        mockMvc.perform(post("/csrf-probe")
                        .cookie(new Cookie("XSRF-TOKEN", token))
                        .header("X-XSRF-TOKEN", token)
                        .header("X-Requested-With", "XMLHttpRequest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void jsonPostSucceedsWhenHeaderMatchesTheSecondDuplicateCookie() throws Exception {
        String token = xsrfCookie();
        mockMvc.perform(post("/csrf-probe")
                        .cookie(new Cookie("XSRF-TOKEN", "stale-root-path"), new Cookie("XSRF-TOKEN", token))
                        .header("X-XSRF-TOKEN", token)
                        .header("X-Requested-With", "XMLHttpRequest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    private String xsrfCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/csrf-probe"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        if (cookie == null || cookie.getValue() == null || cookie.getValue().isBlank()) {
            for (Cookie candidate : result.getResponse().getCookies()) {
                if ("XSRF-TOKEN".equals(candidate.getName())
                        && candidate.getValue() != null && !candidate.getValue().isBlank()) {
                    return candidate.getValue();
                }
            }
        }
        assertNotNull(cookie);
        assertNotNull(cookie.getValue());
        return cookie.getValue();
    }

    @Controller
    static class ProbeController {

        @GetMapping("/csrf-probe")
        @ResponseBody
        String get() {
            return "ok";
        }

        @PostMapping("/csrf-probe")
        @ResponseBody
        String post() {
            return "ok";
        }
    }
}

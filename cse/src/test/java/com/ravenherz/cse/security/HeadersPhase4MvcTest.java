package com.ravenherz.cse.security;

import com.ravenherz.cse.controller.AuthSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HeadersPhase4MvcTest.ProbeController.class)
@Import(SecurityConfig.class)
class HeadersPhase4MvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthSupport authSupport;

    @Test
    void getSendsFrameOptionsNosniffAndCsp() throws Exception {
        mockMvc.perform(get("/headers-probe"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Security-Policy",
                        CseContentSecurityPolicy.DIRECTIVES));
    }

    @Test
    void staticPagesSkipCsp() throws Exception {
        mockMvc.perform(get("/apps/demo/"))
                .andExpect(header().doesNotExist("Content-Security-Policy"));
    }

    @Test
    void hstsOnlyOnHttps() throws Exception {
        mockMvc.perform(get("/headers-probe"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Strict-Transport-Security"));
        mockMvc.perform(get("/headers-probe").secure(true))
                .andExpect(status().isOk())
                .andExpect(header().string("Strict-Transport-Security",
                        "max-age=31536000 ; includeSubDomains"));
    }

    @Controller
    static class ProbeController {

        @GetMapping("/headers-probe")
        @ResponseBody
        String get() {
            return "ok";
        }
    }
}

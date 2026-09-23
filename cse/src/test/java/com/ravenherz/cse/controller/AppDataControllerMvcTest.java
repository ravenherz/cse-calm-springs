package com.ravenherz.cse.controller;

import com.ravenherz.cse.dal.EntityId;

import com.ravenherz.cse.dal.dao.AppStoreService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.AppEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.dto.basic.AppData;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import com.ravenherz.cse.security.SecurityConfig;
import com.ravenherz.cse.store.AppStoreAccess;
import com.ravenherz.cse.store.AppStoreDocument;
import com.ravenherz.cse.store.AppStoreException;
import com.ravenherz.cse.store.AppStoreRateLimiter;
import com.ravenherz.cse.store.AppStoreTableSpec;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AppDataController.class)
@Import(SecurityConfig.class)
class AppDataControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppStoreService appStores;

    @MockitoBean
    private AuthSupport authSupport;

    @MockitoBean
    private com.ravenherz.cse.security.CapabilityService capabilityService;

    @MockitoBean
    private AppStoreRateLimiter rateLimiter;

    @BeforeEach
    void allowWrites() {
        when(rateLimiter.allow(any())).thenReturn(true);
        when(authSupport.clientIp(any())).thenReturn("127.0.0.1");
        when(capabilityService.allows(any(), any())).thenReturn(true);
        when(capabilityService.allowsApp(any(), any())).thenReturn(true);
        when(capabilityService.canOpenEditor(any())).thenReturn(true);
    }

    @Test
    void listUnknownAppIs404() throws Exception {
        when(appStores.requireTable("fretlab", "progress"))
                .thenThrow(new AppStoreException(404, "Unknown app"));
        mockMvc.perform(get("/app-data/fretlab/progress"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void ownerListAsGuestIs401() throws Exception {
        when(appStores.requireTable("fretlab", "progress"))
                .thenReturn(new AppStoreTableSpec("progress", AppStoreAccess.OWNER, null));
        when(authSupport.getAccessor(any(), any())).thenReturn(null);
        mockMvc.perform(get("/app-data/fretlab/progress"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        verify(appStores, never()).list(anyString(), anyString(), any(), anyBoolean(), any(), anyInt());
    }

    @Test
    void publicReadListIsOpen() throws Exception {
        when(appStores.requireTable("hello-snake", "scores"))
                .thenReturn(new AppStoreTableSpec("scores", AppStoreAccess.PUBLIC_READ, null));
        when(appStores.list(eq("hello-snake"), eq("scores"), isNull(), eq(false), isNull(), eq(50)))
                .thenReturn(new AppStoreService.AppStoreList(List.of(), null));
        mockMvc.perform(get("/app-data/hello-snake/scores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void postWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(post("/app-data/fretlab/progress")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tuning\":\"EADGBE\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void postSetsOwnerFromSessionNotBody() throws Exception {
        AccountEntity member = member();
        when(appStores.requireTableOrCreate("fretlab", "progress"))
                .thenReturn(new AppStoreTableSpec("progress", AppStoreAccess.OWNER, null));
        when(authSupport.getAccessor(any(), any())).thenReturn(member);
        AppStoreDocument created = new AppStoreDocument();
        created.setId("68b0000000000000000000aa");
        created.setOwnerId(member.getId().toHexString());
        created.setData(Map.of("tuning", "EADGBE"));
        when(appStores.insert(eq("fretlab"), eq("progress"), eq(member.getId().toHexString()), any()))
                .thenReturn(created);
        mockMvc.perform(post("/app-data/fretlab/progress")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tuning\":\"EADGBE\",\"ownerId\":\"68b000000000000000000099\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(member.getId().toHexString()));
        verify(appStores).insert(eq("fretlab"), eq("progress"), eq(member.getId().toHexString()), any());
    }

    @Test
    void cmsSlugCannotOpenStore() throws Exception {
        when(appStores.requireTable("cse", "accounts"))
                .thenThrow(new AppStoreException(404, "Unknown app"));
        mockMvc.perform(get("/app-data/cse/accounts"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void schemaRequiresEnabledStore() throws Exception {
        AppEntity app = new AppEntity();
        AppData data = new AppData();
        data.setSlug("fretlab");
        data.setStoreEnabled(false);
        app.setAppData(data);
        when(appStores.requireApp("fretlab")).thenReturn(app);
        mockMvc.perform(get("/app-data/fretlab/_schema"))
                .andExpect(status().isForbidden());
    }

    @Test
    void writesAreRateLimited() throws Exception {
        when(rateLimiter.allow(any())).thenReturn(false);
        mockMvc.perform(post("/app-data/fretlab/progress")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
    }

    private static AccountEntity member() {
        AccountEntity account = new AccountEntity(new AccountData("ada", "hash", "ada@example.com",
                SecurityLevel.ACTIVE_USER));
        account.setId(EntityId.of("68b000000000000000000001"));
        return account;
    }
}

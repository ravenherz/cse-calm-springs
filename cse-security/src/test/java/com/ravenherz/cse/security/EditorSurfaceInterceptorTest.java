package com.ravenherz.cse.security;

import com.ravenherz.cse.dal.EntityId;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import com.ravenherz.cse.dal.role.CapabilityIds;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EditorSurfaceInterceptorTest {

    @Test
    void deniedSurfaceRedirectsToSiteErrorNotTomcat() throws Exception {
        AccountAccessor auth = mock(AccountAccessor.class);
        CapabilityService capabilities = mock(CapabilityService.class);
        AccountEntity account = new AccountEntity(
                new AccountData("ada", "hash", "ada@example.com", SecurityLevel.ADMIN));
        account.setId(EntityId.generate());
        when(auth.getAccessor(any(), any())).thenReturn(account);
        when(capabilities.allows(any(), eq(CapabilityIds.EDITOR_ROLES))).thenReturn(false);
        EditorSurfaceInterceptor interceptor = new EditorSurfaceInterceptor(auth, capabilities);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/editor/roles");
        request.setContextPath("/rhz-we");
        request.setRequestURI("/rhz-we/editor/roles");
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals("/rhz-we/?error=403", response.getRedirectedUrl());
    }
}

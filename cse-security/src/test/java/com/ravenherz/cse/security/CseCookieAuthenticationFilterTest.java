package com.ravenherz.cse.security;

import com.ravenherz.cse.dal.EntityId;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CseCookieAuthenticationFilterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anonymousWhenNoAccessor() throws Exception {
        AccountAccessor authSupport = mock(AccountAccessor.class);
        when(authSupport.getAccessor(any(), any())).thenReturn(null);
        new CseCookieAuthenticationFilter(authSupport).doFilter(
                new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void copiesLoginableSessionIntoContext() throws Exception {
        AccountEntity account = new AccountEntity(
                new AccountData("raven", "hash", "r@example.com", SecurityLevel.ADMIN));
        account.setId(EntityId.generate());
        AccountAccessor authSupport = mock(AccountAccessor.class);
        when(authSupport.getAccessor(any(), any())).thenReturn(account);
        new CseCookieAuthenticationFilter(authSupport).doFilter(
                new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());
        CseAuthentication authentication = assertInstanceOf(CseAuthentication.class,
                SecurityContextHolder.getContext().getAuthentication());
        assertEquals("raven", authentication.getName());
        assertEquals(account, authentication.getAccount());
    }
}

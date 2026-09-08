package com.ravenherz.cse.security;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CseAuthoritiesTest {

    @Test
    void activeUserIsRoleUserOnly() {
        Set<String> roles = roles(account(SecurityLevel.ACTIVE_USER));
        assertEquals(Set.of(CseAuthorities.ROLE_USER), roles);
        assertFalse(CseAuthorities.isAdmin(account(SecurityLevel.ACTIVE_USER)));
        assertFalse(CseAuthorities.isAdmin(account(SecurityLevel.OPERATOR)));
        assertFalse(CseAuthorities.isAdmin(account(SecurityLevel.MODERATOR)));
    }

    @Test
    void adminAndOwnerGetRoleAdmin() {
        assertTrue(CseAuthorities.isAdmin(account(SecurityLevel.ADMIN)));
        assertTrue(CseAuthorities.isAdmin(account(SecurityLevel.OWNER)));
        assertTrue(roles(account(SecurityLevel.ADMIN)).contains(CseAuthorities.ROLE_ADMIN));
    }

    @Test
    void authenticationPrincipalIsLoginNotHash() {
        AccountEntity account = account(SecurityLevel.ADMIN);
        CseAuthentication authentication = CseAuthentication.authenticated(account);
        assertEquals("user", authentication.getName());
        assertEquals("user", authentication.getPrincipal());
        assertEquals("", authentication.getCredentials());
    }

    private static Set<String> roles(AccountEntity account) {
        return CseAuthorities.from(account).stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    private static AccountEntity account(SecurityLevel level) {
        AccountEntity entity = new AccountEntity(new AccountData("user", "hash", "u@example.com", level));
        entity.setId(new ObjectId());
        return entity;
    }
}

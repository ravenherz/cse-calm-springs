package com.ravenherz.cse.security;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class CseAuthorities {

    static final String ROLE_USER = "ROLE_USER";
    static final String ROLE_ADMIN = "ROLE_ADMIN";

    private CseAuthorities() {
    }

    static Collection<GrantedAuthority> from(AccountEntity account) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(ROLE_USER));
        return authorities;
    }

    static boolean legacyAdmin(AccountEntity account) {
        if (account == null || account.getAccountData() == null) {
            return false;
        }
        SecurityLevel level = account.getAccountData().getLevel();
        return level != null && level.getIntLevel() >= SecurityLevel.ADMIN.getIntLevel();
    }

    static boolean isAdmin(AccountEntity account) {
        return AccessRuntime.editorAccess(account);
    }
}

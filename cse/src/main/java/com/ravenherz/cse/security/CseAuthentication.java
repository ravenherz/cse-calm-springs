package com.ravenherz.cse.security;

import com.ravenherz.cse.dal.dto.AccountEntity;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public final class CseAuthentication extends AbstractAuthenticationToken {

    private final String login;
    private final AccountEntity account;

    private CseAuthentication(String login, AccountEntity account,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.login = login;
        this.account = account;
        setAuthenticated(true);
    }

    static CseAuthentication authenticated(AccountEntity account) {
        String login = account.getAccountData().getLogin();
        return new CseAuthentication(login, account, CseAuthorities.from(account));
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return login;
    }

    public AccountEntity getAccount() {
        return account;
    }

    @Override
    public String getName() {
        return login;
    }
}

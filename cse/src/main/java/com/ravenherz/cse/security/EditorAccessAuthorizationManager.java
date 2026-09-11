package com.ravenherz.cse.security;

import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.function.Supplier;

public final class EditorAccessAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final CapabilityService capabilities;
    private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

    public EditorAccessAuthorizationManager(CapabilityService capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication,
            RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        if (auth == null || !auth.isAuthenticated() || trustResolver.isAnonymous(auth)) {
            return new AuthorizationDecision(false);
        }
        if (!(auth instanceof CseAuthentication cse)) {
            return new AuthorizationDecision(false);
        }
        return new AuthorizationDecision(capabilities.canOpenEditor(cse.getAccount()));
    }
}

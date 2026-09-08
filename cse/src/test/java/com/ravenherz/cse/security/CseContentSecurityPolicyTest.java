package com.ravenherz.cse.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CseContentSecurityPolicyTest {

    @Test
    void locksDefaultOriginAndPlugins() {
        String policy = CseContentSecurityPolicy.DIRECTIVES;
        assertTrue(policy.contains("default-src 'self'"));
        assertTrue(policy.contains("object-src 'none'"));
        assertTrue(policy.contains("frame-ancestors 'none'"));
        assertTrue(policy.contains("base-uri 'self'"));
        assertTrue(policy.contains("form-action 'self'"));
        assertTrue(policy.contains("connect-src 'self'"));
    }

    @Test
    void allowsInlineHandlersQuillAndGoogleFonts() {
        String policy = CseContentSecurityPolicy.DIRECTIVES;
        assertTrue(policy.contains("script-src 'self' 'unsafe-inline' 'unsafe-eval'"));
        assertTrue(policy.contains("style-src 'self' 'unsafe-inline' https://fonts.googleapis.com"));
        assertTrue(policy.contains("font-src 'self' https://fonts.gstatic.com data:"));
        assertFalse(policy.contains("cdn.quilljs.com"));
    }
}

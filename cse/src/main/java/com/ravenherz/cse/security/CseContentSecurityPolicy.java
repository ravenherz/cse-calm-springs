package com.ravenherz.cse.security;

/**
 * CSP that matches this WAR: local scripts/styles, Google Fonts, inline handlers,
 * Quill, and {@code blob:} previews. {@code /apps/**} is excluded in
 * {@link SecurityConfig} so deployed {@code .cseapp} HTML is not constrained.
 */
final class CseContentSecurityPolicy {

    static final String DIRECTIVES = String.join("; ",
            "default-src 'self'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'",
            "object-src 'none'",
            "frame-src 'none'",
            "script-src 'self' 'unsafe-inline' 'unsafe-eval'",
            "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
            "font-src 'self' https://fonts.gstatic.com data:",
            "img-src 'self' data: blob: https:",
            "media-src 'self' blob:",
            "connect-src 'self'");

    private CseContentSecurityPolicy() {
    }
}

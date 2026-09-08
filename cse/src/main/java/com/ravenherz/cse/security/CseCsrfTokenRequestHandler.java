package com.ravenherz.cse.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

/**
 * Accepts the header or any {@code _csrf} parameter that equals the cookie token.
 * Duplicate query+body fields no longer fail just because {@code getParameter} picked the wrong one.
 */
final class CseCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestAttributeHandler attributes = new CsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            Supplier<CsrfToken> csrfToken) {
        this.attributes.handle(request, response, csrfToken);
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        String expected = csrfToken.getToken();
        String header = request.getHeader(csrfToken.getHeaderName());
        if (header == null || header.isBlank()) {
            header = request.getHeader(CseCookieCsrfTokenRepository.HEADER_NAME);
        }
        if (matches(expected, header)) {
            return expected;
        }
        String[] parameters = parameterValues(request, csrfToken.getParameterName());
        if (parameters != null) {
            for (String parameter : parameters) {
                if (matches(expected, parameter)) {
                    return expected;
                }
            }
        }
        if (header != null && !header.isBlank()) {
            return header;
        }
        return parameters != null && parameters.length > 0 ? parameters[0] : null;
    }

    private static String[] parameterValues(HttpServletRequest request, String parameterName) {
        String[] parameters = parameterName == null || parameterName.isBlank()
                ? null : request.getParameterValues(parameterName);
        if (parameters == null && !CseCookieCsrfTokenRepository.PARAMETER_NAME.equals(parameterName)) {
            parameters = request.getParameterValues(CseCookieCsrfTokenRepository.PARAMETER_NAME);
        }
        return parameters;
    }

    private static boolean matches(String expected, String actual) {
        if (expected == null || actual == null || actual.isBlank()) {
            return false;
        }
        if (expected.equals(actual)) {
            return true;
        }
        return expected.equals(java.net.URLDecoder.decode(actual, StandardCharsets.UTF_8));
    }
}

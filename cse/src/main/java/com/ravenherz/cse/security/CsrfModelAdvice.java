package com.ravenherz.cse.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Put the CSRF token on the Thymeleaf model. The EL compiler does not reliably
 * see request attribute {@code _csrf} as {@code ${_csrf.token}}.
 */
@ControllerAdvice
public class CsrfModelAdvice {

    @ModelAttribute
    public void csrfToken(HttpServletRequest request, Model model) {
        String contextPath = request.getContextPath();
        model.addAttribute("cseContextPath",
                contextPath == null || contextPath.isBlank() ? "/" : contextPath);
        Object token = request.getAttribute(CsrfToken.class.getName());
        if (!(token instanceof CsrfToken)) {
            token = request.getAttribute("_csrf");
        }
        if (token instanceof CsrfToken csrfToken) {
            model.addAttribute("_csrf", csrfToken);
        }
    }
}

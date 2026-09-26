package com.ravenherz.cse.thymeleaf;

import com.ravenherz.cse.admin.AccountDisplayDTO;
import com.ravenherz.cse.admin.RoleOptionDTO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorAccountsTemplateTest {

    @Test
    void directoryShowsCirclesSearchAndContextMenu() {
        AccountDisplayDTO self = new AccountDisplayDTO();
        self.setId("abc");
        self.setLogin("ravenherz");
        self.setInitial("R");
        self.setAvatar("data:image/jpeg;base64,qq");
        self.setSelf(true);
        self.setOwner(true);
        self.setCanEdit(false);
        self.setRoleName("Owner");
        self.setAssignableRoles(List.of());

        AccountDisplayDTO other = new AccountDisplayDTO();
        other.setId("def");
        other.setLogin("root");
        other.setInitial("R");
        other.setCanEdit(true);
        other.setOwner(false);
        other.setRoleId("role-1");
        other.setRoleName("Member");
        other.setAssignableRoles(List.of(new RoleOptionDTO("role-1", "Member", true)));

        SpringTemplateEngine engine = engine();
        MockServletContext servletContext = new MockServletContext();
        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(servletContext);
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setContextPath("/rhz-we");
        request.setRequestURI("/rhz-we/editor/accounts");
        MockHttpServletResponse response = new MockHttpServletResponse();
        WebContext context = new WebContext(application.buildExchange(request, response));
        context.setVariable("accounts", List.of(self, other));
        context.setVariable("q", "");
        context.setVariable("canTransfer", true);
        context.setVariable("transferTargets", List.of(other));
        context.setVariable("saved", false);
        context.setVariable("transferred", false);
        context.setVariable("error", null);
        context.setVariable("cseContextPath", "/rhz-we");
        context.setVariable("username", "ravenherz");
        context.setVariable("navAccounts", true);

        String html = engine.process("admin/editor-accounts-list", context);
        assertFalse(html.contains("${"), html);
        assertTrue(html.contains("account-grid"), html);
        assertTrue(html.contains("account-avatar"), html);
        assertTrue(html.contains("id=\"account-q\""), html);
        assertTrue(html.contains("name=\"q\""), html);
        assertTrue(html.contains("Search"), html);
        assertTrue(html.contains("data:image/jpeg;base64,qq"), html);
        assertTrue(html.contains("ravenherz"), html);
        assertTrue(html.contains("root"), html);
        assertTrue(html.contains("data-account-action=\"details\""), html);
        assertTrue(html.contains("Assign role"), html);
        assertTrue(html.contains("/editor/roles/save"), html);
        assertTrue(html.contains("Transfer ownership"), html);
        assertTrue(html.contains("/editor/roles/transfer"), html);
        assertTrue(html.contains("Delete"), html);
        assertTrue(html.contains("account-details"), html);
        assertTrue(html.contains("is-self"), html);
    }

    private static SpringTemplateEngine engine() {
        Path src = adminSrc();
        FileTemplateResolver pages = new FileTemplateResolver();
        pages.setPrefix(src.toString() + "/");
        pages.setSuffix(".html");
        pages.setTemplateMode(TemplateMode.HTML);
        pages.setCharacterEncoding("UTF-8");
        pages.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(pages);
        engine.setEnableSpringELCompiler(true);
        return engine;
    }

    private static Path adminSrc() {
        Path fromCse = Path.of("..", "app-admin", "src").toAbsolutePath().normalize();
        if (Files.isRegularFile(fromCse.resolve("admin").resolve("editor-accounts-list.html"))) {
            return fromCse;
        }
        Path fromRoot = Path.of("app-admin", "src").toAbsolutePath().normalize();
        if (Files.isRegularFile(fromRoot.resolve("admin").resolve("editor-accounts-list.html"))) {
            return fromRoot;
        }
        throw new IllegalStateException("app-admin/src/admin/editor-accounts-list.html not found");
    }
}

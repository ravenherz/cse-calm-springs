package com.ravenherz.cse.thymeleaf;

import com.ravenherz.cse.dal.EntityId;

import com.ravenherz.cse.dal.dto.RoleEntity;
import com.ravenherz.cse.dal.role.RoleSeeds;
import org.bson.types.ObjectId;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorRolesTemplateTest {

    @Test
    void accessMatrixSplitsGroupsIntoTabsWithHints() {
        RoleEntity member = role(RoleSeeds.MEMBER, "Member", false);
        Map<String, Object> account = group("account", "Account",
                row("account.auth", "Sign in", "Post credentials to sign in.", true, member));
        Map<String, Object> editor = group("editor", "Editor",
                row("editor.access", "Open Catalog", "Open Catalog.", false, member));

        SpringTemplateEngine engine = engine();
        MockServletContext servletContext = new MockServletContext();
        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(servletContext);
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setContextPath("/rhz-we");
        request.setRequestURI("/rhz-we/editor/roles");
        MockHttpServletResponse response = new MockHttpServletResponse();
        WebContext context = new WebContext(application.buildExchange(request, response));
        context.setVariable("canEditMatrix", true);
        context.setVariable("canCrudRoles", true);
        context.setVariable("saved", false);
        context.setVariable("matrixSaved", false);
        context.setVariable("error", null);
        context.setVariable("matrixRoles", List.of(member));
        context.setVariable("matrixGroups", List.of(account, editor));
        context.setVariable("customRoles", List.of(member));
        context.setVariable("cseContextPath", "/rhz-we");
        context.setVariable("username", "owner");
        context.setVariable("navRoles", true);

        String html = engine.process("admin/editor-roles", context);
        assertFalse(html.contains("${"), html);
        assertTrue(html.contains("roles-tabs"), html);
        assertTrue(html.contains("aria-label=\"Access groups\""), html);
        assertTrue(html.contains("id=\"tab-matrix-account\""), html);
        assertTrue(html.contains("id=\"tab-matrix-editor\""), html);
        assertTrue(html.contains("id=\"matrix-account\""), html);
        assertTrue(html.contains("id=\"matrix-editor\""), html);
        assertTrue(html.contains("roles-tab-panel is-active"), html);
        assertTrue(html.contains("roles-cap-hint"), html);
        assertTrue(html.contains("title=\"Post credentials to sign in.\""), html);
        assertTrue(html.contains("name=\"grant\""), html);
        assertTrue(html.contains("account.auth|" + member.idHex()), html);
        assertTrue(html.contains("editor.access|" + member.idHex()), html);
        assertTrue(html.contains("Member"), html);
        assertFalse(html.contains("roles-group-row"), html);
        assertTrue(html.contains("type=\"button\""), html);
        assertFalse(html.contains("/editor/roles/save"), html);
        assertFalse(html.contains("Transfer ownership"), html);
        assertFalse(html.contains("settings-context-title\">Accounts"), html);
    }

    private static Map<String, Object> group(String id, String label, Map<String, Object> row) {
        Map<String, Object> group = new LinkedHashMap<>();
        group.put("id", id);
        group.put("label", label);
        group.put("rows", List.of(row));
        return group;
    }

    private static Map<String, Object> row(String id, String label, String hint, boolean guestSafe, RoleEntity role) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("label", label);
        row.put("hint", hint);
        row.put("guestSafe", guestSafe);
        row.put("cells", Map.of(role.idHex(), true));
        return row;
    }

    private static RoleEntity role(String slug, String name, boolean owner) {
        RoleEntity entity = new RoleEntity();
        entity.setId(new ObjectId());
        entity.setSlug(slug);
        entity.setName(name);
        entity.setSystem(owner ? RoleSeeds.SYSTEM_OWNER : null);
        entity.setLoginable(true);
        return entity;
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
        if (Files.isRegularFile(fromCse.resolve("admin").resolve("editor-roles.html"))) {
            return fromCse;
        }
        Path fromRoot = Path.of("app-admin", "src").toAbsolutePath().normalize();
        if (Files.isRegularFile(fromRoot.resolve("admin").resolve("editor-roles.html"))) {
            return fromRoot;
        }
        throw new IllegalStateException("app-admin/src/admin/editor-roles.html not found");
    }
}

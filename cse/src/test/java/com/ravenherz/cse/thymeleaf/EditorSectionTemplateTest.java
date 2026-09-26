package com.ravenherz.cse.thymeleaf;

import com.ravenherz.cse.admin.AdminSectionPage;
import com.ravenherz.cse.core.admin.AdminField;
import com.ravenherz.cse.core.admin.AdminFieldError;
import com.ravenherz.cse.core.admin.AdminPresentation;
import com.ravenherz.cse.core.admin.AdminSection;
import com.ravenherz.cse.core.admin.CardPlace;
import com.ravenherz.cse.core.admin.FieldType;
import com.ravenherz.cse.core.admin.TreeGlyph;
import com.ravenherz.cse.redirect.RedirectAdmin;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorSectionTemplateTest {

    @Test
    void redirectDescriptorRendersFieldsInTheCatalog() {
        AdminSectionPage page = AdminSectionPage.form(
                new RedirectAdmin().section(),
                "create",
                null,
                Map.of("fromPath", "/old-post", "status", "301"),
                List.of(new AdminFieldError("fromPath", "A redirect from this path already exists")));

        String html = engine().process("admin/editor-section", context(page));

        assertFalse(html.contains("${"), html);
        assertTrue(html.contains(">Redirects</h2>"), html);
        assertTrue(html.contains("resource-tree"), html);
        assertFalse(html.contains("showNavSections"), html);
        assertTrue(html.contains("name=\"fromPath\""), html);
        assertTrue(html.contains("value=\"/old-post\""), html);
        assertTrue(html.contains("name=\"targetPath\""), html);
        assertTrue(html.contains("name=\"status\""), html);
        assertTrue(html.contains(">301<"), html);
        assertTrue(html.contains(">302<"), html);
        assertTrue(html.contains("A redirect from this path already exists"), html);

        AdminSectionPage list = AdminSectionPage.list(
                new RedirectAdmin().section(),
                List.of(Map.of(
                        "id", "abc",
                        "fromPath", "/rhz-we/*",
                        "targetPath", "/*",
                        "status", "301")));
        String cards = engine().process("admin/editor-section", context(list));
        assertTrue(cards.contains("301 /rhz-we/* to /*"), cards);
        assertTrue(cards.contains("thumb-tag-type"), cards);
        assertTrue(cards.contains(">/rhz-we/*<"), cards);
        assertTrue(cards.contains(">/*<"), cards);
        assertTrue(cards.contains(">301<"), cards);
    }

    @Test
    void sectionCardShowsAnImageField() {
        AdminSection section = new AdminSection("url-templates", "URL Templates", List.of(
                new AdminField("urlTemplateId", "Id", FieldType.TEXT, true, List.of(), CardPlace.SOURCE),
                new AdminField("urlImage", "Image", FieldType.TEXT, false, List.of(), CardPlace.IMAGE)),
                new AdminPresentation("{urlTemplateId}", TreeGlyph.FILE));
        AdminSectionPage list = AdminSectionPage.list(section, List.of(Map.of(
                "id", "abc",
                "urlTemplateId", "youtube",
                "urlImage", "data:image/png;base64,abc+def/ghi=")));
        String cards = engine().process("admin/editor-section", context(list));
        assertTrue(cards.contains("class=\"item-thumb\""), cards);
        assertTrue(cards.contains("src=\"data:image/png;base64,abc+def/ghi=\""), cards);
        assertFalse(cards.contains("/rhz-we/data:"), cards);
        assertFalse(cards.contains("app-logo-fallback"), cards);
    }

    private static WebContext context(AdminSectionPage page) {
        MockServletContext servletContext = new MockServletContext();
        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(servletContext);
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setContextPath("/rhz-we");
        request.setRequestURI("/rhz-we/editor/sections/redirects/create");
        MockHttpServletResponse response = new MockHttpServletResponse();
        WebContext context = new WebContext(application.buildExchange(request, response));
        context.setVariable("adminSection", page);
        context.setVariable("cseContextPath", "/rhz-we");
        context.setVariable("username", "owner");
        context.setVariable("navResources", false);
        context.setVariable("navAccounts", false);
        context.setVariable("navRoles", false);
        context.setVariable("navSettings", false);
        context.setVariable("navSiteData", false);
        context.setVariable("navApi", false);
        context.setVariable("navLogs", false);
        context.setVariable("navTranscode", false);
        context.setVariable("navInstance", false);
        context.setVariable("resourceGroupTree", java.util.List.of());
        context.setVariable("defaultGroupId", "");
        context.setVariable("selectedGroup", null);
        return context;
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
        if (Files.isRegularFile(fromCse.resolve("admin").resolve("editor-section.html"))) {
            return fromCse;
        }
        Path fromRoot = Path.of("app-admin", "src").toAbsolutePath().normalize();
        if (Files.isRegularFile(fromRoot.resolve("admin").resolve("editor-section.html"))) {
            return fromRoot;
        }
        throw new IllegalStateException("app-admin/src/admin/editor-section.html not found");
    }
}

package com.ravenherz.cse.thymeleaf;

import com.ravenherz.cse.controller.PublicApiDocs;
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

class EditorApiTemplateTest {

    @Test
    void apiTabRendersCatalogWithContextPrefix() {
        SpringTemplateEngine engine = engine();
        MockServletContext servletContext = new MockServletContext();
        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(servletContext);
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setContextPath("/rhz-we");
        request.setRequestURI("/rhz-we/editor/api");
        MockHttpServletResponse response = new MockHttpServletResponse();
        WebContext context = new WebContext(application.buildExchange(request, response));
        context.setVariable("apiSections", PublicApiDocs.sections());
        context.setVariable("cseContextPath", "/rhz-we");
        context.setVariable("resourceGroupTree", List.of());
        context.setVariable("selectedGroup", null);
        context.setVariable("defaultGroupId", "");
        context.setVariable("username", "owner");
        context.setVariable("navApi", true);
        context.setVariable("navResources", false);
        context.setVariable("navApps", false);
        context.setVariable("navThemes", false);
        context.setVariable("navCategoriesActive", false);
        context.setVariable("navPages", false);
        context.setVariable("navPlaylists", false);
        context.setVariable("navAccounts", false);
        context.setVariable("navRoles", false);
        context.setVariable("navSettings", false);
        context.setVariable("navSiteData", false);
        context.setVariable("navLogs", false);

        String html = engine.process("admin/editor-api", context);

        assertFalse(html.contains("${"), html);
        assertTrue(html.contains("/rhz-we/account/auth"), html);
        assertTrue(html.contains("/rhz-we/rest/site"), html);
        assertTrue(html.contains("X-XSRF-TOKEN"), html);
        assertTrue(html.contains("id=\"install\""), html);
        assertTrue(html.contains("role=\"tablist\""), html);
        assertTrue(html.contains(">Conventions</a>"), html);
        assertTrue(html.contains(">Account</a>"), html);
        assertTrue(html.contains(">REST helpers</a>"), html);
        assertTrue(html.contains(">Public site</a>"), html);
        assertTrue(html.contains(">First-boot install</a>"), html);
        assertTrue(html.contains("id=\"app-data\""), html);
        assertTrue(html.contains(">App data</a>"), html);
        assertTrue(html.contains("api-tab-panel"), html);
        assertTrue(html.contains("is-post"), html);
        assertTrue(html.contains("is-get"), html);
        assertTrue(html.contains("<details class=\"api-endpoint\""), html);
        assertTrue(html.contains("api-endpoint-summary"), html);
        assertTrue(html.contains("api-endpoint-body"), html);
        int authHead = html.indexOf("/rhz-we/account/auth");
        int authFlags = html.indexOf(">Public</span>", authHead);
        int authHint = html.indexOf("Sign in. Sets HttpOnly cookies", authHead);
        int authRequest = html.indexOf("api-sample-label", authHint);
        assertTrue(authHead > 0 && authFlags > authHead && authHint > authFlags && authRequest > authHint, html);
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
        if (Files.isRegularFile(fromCse.resolve("admin").resolve("editor-api.html"))) {
            return fromCse;
        }
        Path fromRoot = Path.of("app-admin", "src").toAbsolutePath().normalize();
        if (Files.isRegularFile(fromRoot.resolve("admin").resolve("editor-api.html"))) {
            return fromRoot;
        }
        throw new IllegalStateException("app-admin/src/admin/editor-api.html not found");
    }
}

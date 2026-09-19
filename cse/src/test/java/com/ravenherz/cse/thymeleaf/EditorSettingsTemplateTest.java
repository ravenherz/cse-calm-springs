package com.ravenherz.cse.thymeleaf;

import com.ravenherz.cse.util.themes.ThemeInfo;
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

class EditorSettingsTemplateTest {

    @Test
    void appearanceSelectsShareSchemaListsForModernPaperAndJs() {
        List<String> schemas = List.of("atelier", "chalk", "charcoal", "ivory", "linen", "midnight", "studio");
        ThemeInfo modern = new ThemeInfo("modern", "modern", null, null, "modern", "linen", schemas, true);
        ThemeInfo paper = new ThemeInfo("paper", "Paper", null, null, "modern", "ivory", schemas, true);
        ThemeInfo client = new ThemeInfo("client", "JS", null, null, "js", "linen", schemas, true);

        SpringTemplateEngine engine = engine();
        MockServletContext servletContext = new MockServletContext();
        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(servletContext);
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setContextPath("/rhz-we");
        request.setRequestURI("/rhz-we/editor/settings");
        MockHttpServletResponse response = new MockHttpServletResponse();
        WebContext context = new WebContext(application.buildExchange(request, response));
        Map<String, String> view = new LinkedHashMap<>();
        view.put("styles-theme", "modern");
        view.put("styles-schema", "linen");
        Map<String, String> personal = new LinkedHashMap<>();
        personal.put("company-title", "Calm Springs");
        Map<String, Map<String, String>> contexts = new LinkedHashMap<>();
        contexts.put("config-view", view);
        contexts.put("config-personal", personal);
        context.setVariable("settingContexts", contexts);
        context.setVariable("publicThemes", List.of(modern, paper, client));
        context.setVariable("activeThemeSchemas", schemas);
        context.setVariable("cseContextPath", "/rhz-we");
        context.setVariable("resourceGroupTree", List.of());
        context.setVariable("selectedGroup", null);
        context.setVariable("defaultGroupId", "");
        context.setVariable("username", "owner");
        context.setVariable("navApi", false);
        context.setVariable("navResources", false);
        context.setVariable("navApps", false);
        context.setVariable("navThemes", false);
        context.setVariable("navCategoriesActive", false);
        context.setVariable("navPages", false);
        context.setVariable("navPlaylists", false);
        context.setVariable("navAccounts", false);
        context.setVariable("navRoles", false);
        context.setVariable("navSettings", true);
        context.setVariable("navSiteData", false);
        context.setVariable("navLogs", false);

        String html = engine.process("admin/editor-settings", context);

        assertFalse(html.contains("${"), html);
        assertTrue(html.contains(">Settings</h1>"), html);
        assertTrue(html.contains("role=\"tablist\""), html);
        assertTrue(html.contains("settings-tab-config-view"), html);
        assertTrue(html.contains("settings-panel-config-personal"), html);
        assertTrue(html.contains(">view<"), html);
        assertTrue(html.contains(">personal<"), html);
        assertFalse(html.contains("form-panel"), html);
        assertFalse(html.contains("scope=\"col\""), html);
        assertFalse(html.contains("id=\"my-styles-theme\""), html);
        assertFalse(html.contains("id=\"my-styles-schema\""), html);
        assertFalse(html.contains("Save your theme"), html);
        assertTrue(html.contains("bindThemeToSchema"), html);
        assertTrue(html.contains("atelier, chalk, charcoal, ivory, linen, midnight, studio"), html);
        assertTrue(html.contains("data-default=\"linen\""), html);
        assertTrue(html.contains("data-default=\"ivory\""), html);
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
        if (Files.isRegularFile(fromCse.resolve("admin").resolve("editor-settings.html"))) {
            return fromCse;
        }
        Path fromRoot = Path.of("app-admin", "src").toAbsolutePath().normalize();
        if (Files.isRegularFile(fromRoot.resolve("admin").resolve("editor-settings.html"))) {
            return fromRoot;
        }
        throw new IllegalStateException("app-admin/src/admin/editor-settings.html not found");
    }
}

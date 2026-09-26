package com.ravenherz.cse.thymeleaf;

import com.ravenherz.cse.admin.ScriptRunRow;
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

class EditorScriptingTemplateTest {

    @Test
    void runsTabListsRecordedRuns() {
        String html = render(List.of(new ScriptRunRow("on-save", "Done", "2026-09-26 12:00:00", "ok")));

        assertFalse(html.contains("${"), html);
        assertTrue(html.contains(">Runs</a>"), html);
        assertTrue(html.contains("/rhz-we/editor/sections/scripts"), html);
        assertTrue(html.contains("resource-tree"), html);
        assertTrue(html.contains(">on-save<"), html);
        assertTrue(html.contains(">Done<"), html);
        assertTrue(html.contains("2026-09-26 12:00:00"), html);
        assertTrue(html.contains(">ok<"), html);
    }

    private static String render(List<ScriptRunRow> runs) {
        SpringTemplateEngine engine = engine();
        MockServletContext servletContext = new MockServletContext();
        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(servletContext);
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setContextPath("/rhz-we");
        request.setRequestURI("/rhz-we/editor/scripting");
        MockHttpServletResponse response = new MockHttpServletResponse();
        WebContext context = new WebContext(application.buildExchange(request, response));
        context.setVariable("scriptRuns", runs);
        context.setVariable("scriptError", "");
        context.setVariable("cseContextPath", "/rhz-we");
        context.setVariable("username", "owner");
        context.setVariable("navResources", true);
        context.setVariable("navAccounts", false);
        context.setVariable("navRoles", false);
        context.setVariable("navSettings", false);
        context.setVariable("navSiteData", false);
        context.setVariable("navApi", false);
        context.setVariable("navLogs", false);
        context.setVariable("navTranscode", false);
        context.setVariable("navInstance", false);
        context.setVariable("resourceGroupTree", List.of());
        context.setVariable("defaultGroupId", "");
        context.setVariable("selectedGroup", null);
        return engine.process("admin/editor-scripting", context);
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
        if (Files.isRegularFile(fromCse.resolve("admin").resolve("editor-scripting.html"))) {
            return fromCse;
        }
        Path fromRoot = Path.of("app-admin", "src").toAbsolutePath().normalize();
        if (Files.isRegularFile(fromRoot.resolve("admin").resolve("editor-scripting.html"))) {
            return fromRoot;
        }
        throw new IllegalStateException("app-admin/src/admin/editor-scripting.html not found");
    }
}

package com.ravenherz.cse.thymeleaf;

import com.ravenherz.cse.controller.EditorTranscodeController.Item;
import com.ravenherz.cse.controller.EditorTranscodeController.QueueSnapshot;
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

class EditorTranscodeTemplateTest {

    @Test
    void transcodeTabRendersQueueAndPoller() {
        QueueSnapshot queue = new QueueSnapshot(1, 1, 0, 0, 1, 1, List.of(
                new Item("aaaaaaaaaaaaaaaaaaaaaaaa", "processing", 42, null, null, "ada",
                        "483.84 MB", null, "live.mov"),
                new Item("bbbbbbbbbbbbbbbbbbbbbbbb", "queued", 0, null, null, "ada",
                        "8.0 MB", null, "next.mp4")));

        SpringTemplateEngine engine = engine();
        MockServletContext servletContext = new MockServletContext();
        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(servletContext);
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setContextPath("/rhz-we");
        request.setRequestURI("/rhz-we/editor/transcode");
        MockHttpServletResponse response = new MockHttpServletResponse();
        WebContext context = new WebContext(application.buildExchange(request, response));
        context.setVariable("queue", queue);
        context.setVariable("cseContextPath", "/rhz-we");
        context.setVariable("resourceGroupTree", List.of());
        context.setVariable("selectedGroup", null);
        context.setVariable("defaultGroupId", "");
        context.setVariable("username", "owner");
        context.setVariable("navTranscode", true);
        context.setVariable("navResources", false);
        context.setVariable("navAccounts", false);
        context.setVariable("navRoles", false);
        context.setVariable("navSettings", false);
        context.setVariable("navSiteData", false);
        context.setVariable("navApi", false);
        context.setVariable("navLogs", false);
        context.setVariable("navInstance", false);

        String html = engine.process("admin/editor-transcode", context);

        assertFalse(html.contains("${"), html);
        assertTrue(html.contains(">Transcode</h1>"), html);
        assertTrue(html.contains("/rhz-we/editor/transcode/queue"), html);
        assertTrue(html.contains("1 running · 1 queued"), html);
        assertTrue(html.contains(">live.mov<"), html);
        assertTrue(html.contains(">next.mp4<"), html);
        assertTrue(html.contains(">Author<"), html);
        assertTrue(html.contains(">Size in<"), html);
        assertTrue(html.contains(">Size out<"), html);
        assertTrue(html.contains(">ada<"), html);
        assertTrue(html.contains("483.84 MB"), html);
        assertTrue(html.contains("transcode-meter"), html);
        assertTrue(html.contains("42%"), html);
        assertTrue(html.contains("id=\"transcode-auto\""), html);
        assertTrue(html.contains("class=\"editor-nav\""), html);
        assertTrue(html.contains("/editor/transcode\""), html);
        assertTrue(html.contains("nav-sep"), html);
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
        if (Files.isRegularFile(fromCse.resolve("admin").resolve("editor-transcode.html"))) {
            return fromCse;
        }
        Path fromRoot = Path.of("app-admin", "src").toAbsolutePath().normalize();
        if (Files.isRegularFile(fromRoot.resolve("admin").resolve("editor-transcode.html"))) {
            return fromRoot;
        }
        throw new IllegalStateException("app-admin/src/admin/editor-transcode.html not found");
    }
}

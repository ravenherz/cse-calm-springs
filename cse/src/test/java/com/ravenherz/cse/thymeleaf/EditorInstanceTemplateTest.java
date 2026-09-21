package com.ravenherz.cse.thymeleaf;

import com.ravenherz.cse.engine.io.InstanceProbe.Cpu;
import com.ravenherz.cse.engine.io.InstanceProbe.Disk;
import com.ravenherz.cse.engine.io.InstanceProbe.Gpu;
import com.ravenherz.cse.engine.io.InstanceProbe.Host;
import com.ravenherz.cse.engine.io.InstanceProbe.Memory;
import com.ravenherz.cse.engine.io.InstanceProbe.Snapshot;
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

class EditorInstanceTemplateTest {

    @Test
    void instanceTabRendersHostCpuMemoryDiskAndGpu() {
        Snapshot snapshot = new Snapshot(
                1_700_000_000_000L,
                new Host("nl3", "Ubuntu 24.04.3 LTS (6.8.0-139-generic)", "amd64", "rhz-we",
                        "/var/cse/rhz-we", "21.0.4", "2d 4h"),
                new Cpu("Xeon", 8, 0.12, 0.04, 1.25, 12, 4),
                new Memory(16L * 1024 * 1024 * 1024, 8L * 1024 * 1024 * 1024, 8L * 1024 * 1024 * 1024,
                        "16 GB", "8 GB", "8 GB", 50,
                        512L * 1024 * 1024, 2L * 1024 * 1024 * 1024,
                        "512 MB", "2 GB", 25),
                List.of(new Disk("/dev/sda1 (/)", 200L * 1024 * 1024 * 1024, 80L * 1024 * 1024 * 1024,
                        120L * 1024 * 1024 * 1024, "200 GB", "80 GB", "120 GB", 40, true)),
                List.of(new Gpu("Tesla T4", 16L * 1024 * 1024 * 1024, 2L * 1024 * 1024 * 1024,
                        "16 GB", "2 GB", 12, 33, "550.54", true)));

        SpringTemplateEngine engine = engine();
        MockServletContext servletContext = new MockServletContext();
        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(servletContext);
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setContextPath("/rhz-we");
        request.setRequestURI("/rhz-we/editor/instance");
        MockHttpServletResponse response = new MockHttpServletResponse();
        WebContext context = new WebContext(application.buildExchange(request, response));
        context.setVariable("snapshot", snapshot);
        context.setVariable("cseContextPath", "/rhz-we");
        context.setVariable("resourceGroupTree", List.of());
        context.setVariable("selectedGroup", null);
        context.setVariable("defaultGroupId", "");
        context.setVariable("username", "owner");
        context.setVariable("navInstance", true);
        context.setVariable("navResources", false);
        context.setVariable("navAccounts", false);
        context.setVariable("navRoles", false);
        context.setVariable("navSettings", false);
        context.setVariable("navSiteData", false);
        context.setVariable("navApi", false);
        context.setVariable("navLogs", false);

        String html = engine.process("admin/editor-instance", context);

        assertFalse(html.contains("${"), html);
        assertTrue(html.contains(">Instance</h1>"), html);
        assertTrue(html.contains("/rhz-we/editor/instance/snapshot"), html);
        assertTrue(html.contains(">Context<"), html);
        assertTrue(html.contains(">nl3<"), html);
        assertTrue(html.contains("/var/cse/rhz-we"), html);
        assertTrue(html.contains("Xeon"), html);
        assertTrue(html.contains(">Physical<"), html);
        assertTrue(html.contains(">JVM heap<"), html);
        assertTrue(html.contains("id=\"instance-load-body\""), html);
        assertTrue(html.contains("/dev/sda1"), html);
        assertTrue(html.contains(">CSE<"), html);
        assertFalse(html.contains(">Disks<"), html);
        assertTrue(html.contains(">Tesla T4<"), html);
        assertTrue(html.contains("nvidia-smi"), html);
        assertTrue(html.contains("data-meter=\"cpu.system\""), html);
        assertTrue(html.contains("id=\"instance-auto\""), html);
        assertFalse(html.contains("instance-card"), html);
        assertFalse(html.contains("instance-props"), html);
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
        if (Files.isRegularFile(fromCse.resolve("admin").resolve("editor-instance.html"))) {
            return fromCse;
        }
        Path fromRoot = Path.of("app-admin", "src").toAbsolutePath().normalize();
        if (Files.isRegularFile(fromRoot.resolve("admin").resolve("editor-instance.html"))) {
            return fromRoot;
        }
        throw new IllegalStateException("app-admin/src/admin/editor-instance.html not found");
    }
}

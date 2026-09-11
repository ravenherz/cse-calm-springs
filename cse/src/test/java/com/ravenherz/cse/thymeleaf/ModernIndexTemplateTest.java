package com.ravenherz.cse.thymeleaf;

import com.ravenherz.cse.present.CategorySectionDTO;
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

class ModernIndexTemplateTest {

    @Test
    void portfolioHomeRendersNavAnchorsWithSpringElCompiler() {
        SpringTemplateEngine engine = engine();
        MockServletContext servletContext = new MockServletContext();
        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(servletContext);
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setContextPath("/rhz-we");
        request.setRequestURI("/rhz-we/");
        MockHttpServletResponse response = new MockHttpServletResponse();
        WebContext context = new WebContext(application.buildExchange(request, response));

        CategorySectionDTO music = CategorySectionDTO.untitled("Music Releases", List.of());
        context.setVariable("navCategories", List.of(music));
        context.setVariable("navCategoriesActive", false);
        context.setVariable("portfolioHome", true);
        context.setVariable("sections", List.of(music));
        context.setVariable("configured", true);
        context.setVariable("isNotError", true);
        context.setVariable("loginPanel", true);
        context.setVariable("authenticated", false);
        context.setVariable("categoryView", false);
        context.setVariable("tagView", false);
        context.setVariable("readingPage", false);
        context.setVariable("htmlTitle", "ravenherz");
        context.setVariable("siteName", "ravenherz");
        context.setVariable("stylesTheme", "modern");
        context.setVariable("stylesShell", "modern");
        context.setVariable("stylesSchema", "studio");
        context.setVariable("username", "");
        context.setVariable("introPage", null);
        context.setVariable("selectedTag", null);
        context.setVariable("TagWelcomeTitle", "");
        context.setVariable("TagWelcomeMessage", "");
        context.setVariable("TagWelcomeDescription", "");
        context.setVariable("TagCompanyName", "");
        context.setVariable("TagCompanyPhysicalAddress", "");
        context.setVariable("TagCompanyPhone", "");
        context.setVariable("TagCompanyEmailAddress", "");
        context.setVariable("TagCompanySocialLinks", "");
        context.setVariable("TagCopyrightHolder", "");
        context.setVariable("TagCopyrightComment", "");
        context.setVariable("TagBuilderLink", "");
        context.setVariable("TagSoftVersion", "");
        context.setVariable("versionProduct", "cse");
        context.setVariable("versionVersion", "0");
        context.setVariable("versionBranch", "");

        String html = engine.process("index", context);

        assertTrue(html.contains("#s-music-releases"), html);
        assertTrue(html.contains("id=\"s-music-releases\""), html);
        assertFalse(html.contains("#{"), "Spring EL must not treat #s- as an expression: " + html);
        assertTrue(html.contains("account-link"), html);
        assertTrue(html.contains("apps/login"), html);
        assertFalse(html.contains("id=\"loginpanel-username\""), html);
        assertFalse(html.contains("cse-lp.js"), html);
        assertTrue(html.contains("/content-public/themes/modern/js/jquery.min.js"), html);
        assertTrue(html.contains("cse-video-player.js"), html);
        assertTrue(html.contains("/content-public/cse-core/css/system.css"), html);
        assertFalse(html.contains("quill.min.js"), html);
        assertFalse(html.contains("quill.snow.css"), html);
        int css = html.indexOf("/content-public/cse-core/css/system.css");
        int jquery = html.indexOf("/content-public/themes/modern/js/jquery.min.js");
        assertTrue(css >= 0 && jquery > css, html);
        assertFalse(html.contains("/content-public/js/"), html);
        assertFalse(html.contains("fonts.googleapis.com"), html);
    }

    @Test
    void modernSkinAppliesToJsShellClass() throws Exception {
        String css = Files.readString(modernSrc().resolve("css").resolve("styles.css"));
        assertTrue(css.contains("html.theme-js"), css);
        assertTrue(css.contains("body.theme-js"), css);
        Path client = clientSrc().resolve("index.html");
        String html = Files.readString(client);
        assertTrue(html.contains("theme-js"), html);
        assertTrue(html.contains("theme-modern"), html);
        assertTrue(html.contains("stylesShell"), html);
        assertTrue(html.contains("TagCompanyName"), html);
        assertTrue(html.contains("TagCompanySocialLinks"), html);
        assertTrue(html.contains("apps/login"), html);
        assertFalse(html.contains("loginpanel-username"), html);
        assertFalse(html.contains("cse-lp.js"), html);
        assertFalse(html.contains("content-public/js"), html);
        assertFalse(html.contains("fonts.googleapis.com"), html);
        assertFalse(html.contains("quill.min.js"), html);
        int cssPos = html.indexOf("cse-core/css/system.css");
        int jqueryPos = html.indexOf("jquery.min.js");
        assertTrue(cssPos >= 0 && jqueryPos > cssPos, html);
    }

    @Test
    void twoThousandsThemeReplacesLoginPanelWithAppLink() throws Exception {
        Path src = theme2000sSrc().resolve("index.html");
        String html = Files.readString(src);
        assertTrue(html.contains("apps/login"), html);
        assertFalse(html.contains("loginpanel-username"), html);
        assertFalse(html.contains("cse-lp.js"), html);
        assertFalse(html.contains("loginPanelInit"), html);
        assertFalse(html.contains("quill.min.js"), html);
        int css = html.indexOf("cse-core/css/system.css");
        int jquery = html.indexOf("jquery.min.js");
        assertTrue(css >= 0 && jquery > css, html);
    }

    private static SpringTemplateEngine engine() {
        Path src = modernSrc();
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

    private static Path modernSrc() {
        Path fromCse = Path.of("..", "theme-thymeleaf-modern", "src").toAbsolutePath().normalize();
        if (Files.isRegularFile(fromCse.resolve("index.html"))) {
            return fromCse;
        }
        Path fromRoot = Path.of("theme-thymeleaf-modern", "src").toAbsolutePath().normalize();
        if (Files.isRegularFile(fromRoot.resolve("index.html"))) {
            return fromRoot;
        }
        throw new IllegalStateException("theme-thymeleaf-modern/src/index.html not found");
    }

    private static Path clientSrc() {
        Path fromCse = Path.of("..", "theme-js-client", "src").toAbsolutePath().normalize();
        if (Files.isRegularFile(fromCse.resolve("index.html"))) {
            return fromCse;
        }
        Path fromRoot = Path.of("theme-js-client", "src").toAbsolutePath().normalize();
        if (Files.isRegularFile(fromRoot.resolve("index.html"))) {
            return fromRoot;
        }
        throw new IllegalStateException("theme-js-client/src/index.html not found");
    }

    private static Path theme2000sSrc() {
        Path fromCse = Path.of("..", "theme-thymeleaf-2000s", "src").toAbsolutePath().normalize();
        if (Files.isRegularFile(fromCse.resolve("index.html"))) {
            return fromCse;
        }
        Path fromRoot = Path.of("theme-thymeleaf-2000s", "src").toAbsolutePath().normalize();
        if (Files.isRegularFile(fromRoot.resolve("index.html"))) {
            return fromRoot;
        }
        throw new IllegalStateException("theme-thymeleaf-2000s/src/index.html not found");
    }
}

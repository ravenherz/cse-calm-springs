package com.ravenherz.cse.util.themes;

import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.cache.NonCacheableCacheEntryValidity;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolution;
import org.thymeleaf.templateresource.FileTemplateResource;
import org.thymeleaf.templateresource.ITemplateResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Thymeleaf files come from exploded packs on disk, not from the WAR.
 * Theme shells: {@code {id}/index} under the bound themes directory.
 * Editor: {@code admin/*} and {@code fragments/*} under the exploded {@code admin.cseapp}.
 */
public class DiskThemeTemplateResolver implements ITemplateResolver {

    public static final int ORDER = 1;

    /** Installed admin app folder. Same value as {@code StaticAppDeployer.ADMIN_SLUG}. */
    static final String ADMIN_APP = "admin";

    @Override
    public String getName() {
        return "disk-packs";
    }

    @Override
    public Integer getOrder() {
        return ORDER;
    }

    @Override
    public TemplateResolution resolveTemplate(IEngineConfiguration configuration, String ownerTemplate,
            String template, Map<String, Object> templateResolutionAttributes) {
        Path file = fileFor(template);
        if (file == null) {
            return null;
        }
        ITemplateResource resource = new FileTemplateResource(file.toFile(), "UTF-8");
        return new TemplateResolution(resource, TemplateMode.HTML, NonCacheableCacheEntryValidity.INSTANCE);
    }

    Path fileFor(String template) {
        if (template == null || template.isBlank()) {
            return null;
        }
        String name = template.startsWith("/") ? template.substring(1) : template;
        if (name.isBlank() || name.contains("..")) {
            return null;
        }
        try {
            if (isThemeShell(name)) {
                return existing(ThemeRoots.themesDir(), name + ".html");
            }
            if (isAdminTemplate(name)) {
                Path adminRoot = ThemeRoots.appsDir().resolve(ADMIN_APP);
                return existing(adminRoot, name + ".html");
            }
        } catch (IOException ex) {
            return null;
        }
        return null;
    }

    private static boolean isThemeShell(String name) {
        int slash = name.indexOf('/');
        return slash > 0 && slash == name.lastIndexOf('/') && name.endsWith("/index");
    }

    private static boolean isAdminTemplate(String name) {
        return name.startsWith("admin/") || name.startsWith("fragments/");
    }

    private static Path existing(Path root, String relative) {
        Path base = root.toAbsolutePath().normalize();
        Path file = base.resolve(relative).normalize();
        if (!file.startsWith(base) || !Files.isRegularFile(file)) {
            return null;
        }
        return file;
    }
}

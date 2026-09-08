package com.ravenherz.cse;

import com.ravenherz.cse.util.io.CseDisk;
import com.ravenherz.cse.util.io.CseDiskBinder;
import com.ravenherz.cse.util.staticapps.StaticAppDeployer;
import com.ravenherz.cse.util.themes.ThemeDiskResources;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.io.IOException;

@Configuration
@EnableWebMvc
public class MvcConfig implements WebMvcConfigurer {

    public MvcConfig(CseDiskBinder cseDiskBinder) {
        // Bind instance disk before /content-cache/** is mapped.
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String cacheLocation = CseDisk.contentCacheDir().toURI().toString();
        if (!cacheLocation.endsWith("/")) {
            cacheLocation += "/";
        }
        registry
                .addResourceHandler("/content-cache/**")
                .addResourceLocations(cacheLocation);
        String themesLocation = null;
        try {
            themesLocation = CseDisk.themesDir().toURI().toString();
            if (!themesLocation.endsWith("/")) {
                themesLocation += "/";
            }
        } catch (IOException ignored) {
            // Exploded packs stay dark until instance disk is writable.
        }
        String adminLocation = null;
        try {
            File adminDir = new File(CseDisk.staticPagesDir(), StaticAppDeployer.ADMIN_SLUG);
            if (!adminDir.isDirectory() && !adminDir.mkdirs()) {
                throw new IOException("Cannot create " + adminDir.getAbsolutePath());
            }
            adminLocation = adminDir.toURI().toString();
            if (!adminLocation.endsWith("/")) {
                adminLocation += "/";
            }
        } catch (IOException ignored) {
            // Admin CSS stays dark until the app pack is exploded.
        }
        if (themesLocation != null) {
            registry
                    .addResourceHandler("/content-public/themes/**")
                    .addResourceLocations(themesLocation)
                    .resourceChain(true)
                    .addResolver(ThemeDiskResources.isolated());
        }
        if (adminLocation != null) {
            registry
                    .addResourceHandler("/content-public/admin/**")
                    .addResourceLocations(adminLocation, "classpath:/static/content-public/admin/");
        }
        registry
                .addResourceHandler("/content-public/**")
                .addResourceLocations("classpath:/static/content-public/");
    }
}

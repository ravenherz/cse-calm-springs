package com.ravenherz.cse;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class MvcConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/content-cache/**")
                .addResourceLocations("file:/var/cse/content-cache/");
        registry
                .addResourceHandler("/content-public/**")
                .addResourceLocations("classpath:/static/content-public/");
    }
}
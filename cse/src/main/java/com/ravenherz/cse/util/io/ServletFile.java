package com.ravenherz.cse.util.io;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Classpath reads that work from an exploded WAR and from a fat JAR.
 */
@Component
public class ServletFile {

    private static final PathMatchingResourcePatternResolver RESOLVER =
            new PathMatchingResourcePatternResolver();

    public static String classpathLocation(String relPath) {
        if (relPath == null) {
            return "";
        }
        String filtered = relPath.replace('\\', '/');
        while (filtered.startsWith("/")) {
            filtered = filtered.substring(1);
        }
        return filtered;
    }

    public static InputStream openStream(String relPath) throws IOException {
        ClassPathResource resource = new ClassPathResource(classpathLocation(relPath));
        if (!resource.exists()) {
            return null;
        }
        return resource.getInputStream();
    }

    public static byte[] readBytes(String relPath) throws IOException {
        try (InputStream in = openStream(relPath)) {
            if (in == null) {
                return null;
            }
            return in.readAllBytes();
        }
    }

    public static String readUtf8(String relPath) throws IOException {
        byte[] bytes = readBytes(relPath);
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    public static Resource[] list(String directory, String filePattern) throws IOException {
        String location = classpathLocation(directory);
        if (!location.isEmpty() && !location.endsWith("/")) {
            location = location + "/";
        }
        String pattern = filePattern == null || filePattern.isBlank() ? "*" : filePattern;
        return RESOLVER.getResources("classpath*:" + location + pattern);
    }
}

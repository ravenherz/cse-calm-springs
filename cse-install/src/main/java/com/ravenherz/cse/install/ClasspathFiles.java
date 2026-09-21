package com.ravenherz.cse.install;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

final class ClasspathFiles {

    private ClasspathFiles() {
    }

    static InputStream open(String path) throws IOException {
        String location = path == null ? "" : path.replace('\\', '/');
        while (location.startsWith("/")) {
            location = location.substring(1);
        }
        ClassPathResource resource = new ClassPathResource(location);
        if (!resource.exists()) {
            return null;
        }
        return resource.getInputStream();
    }

    static byte[] readBytes(String path) throws IOException {
        try (InputStream in = open(path)) {
            if (in == null) {
                return null;
            }
            return in.readAllBytes();
        }
    }
}

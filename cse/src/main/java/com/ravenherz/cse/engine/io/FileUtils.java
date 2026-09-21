package com.ravenherz.cse.engine.io;

import com.ravenherz.cse.util.io.ServletFile;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Scope(value = "singleton")
public class FileUtils {

    public String readFileAsString(String relativePath) throws IOException {
        String text = ServletFile.readUtf8(relativePath);
        if (text == null) {
            throw new IOException("Classpath resource not found: " + relativePath);
        }
        return text;
    }
}

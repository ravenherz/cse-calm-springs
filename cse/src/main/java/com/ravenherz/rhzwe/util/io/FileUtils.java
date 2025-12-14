package com.ravenherz.rhzwe.util.io;

import com.ravenherz.rhzwe.util.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

@Lazy
@Service
@Scope(value = "singleton")
@DependsOn(value = {"settings"})
public class FileUtils {

    private Settings settings;

    @Lazy @Autowired
    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    public String readFileAsString(String relativePath) throws IOException {
        Scanner scanner = new Scanner(ServletFile.classPathFile(relativePath));
        List<String> lines = new LinkedList<>();
        while (scanner.hasNextLine()) {
            lines.add(scanner.nextLine());
        }
        scanner.close();
        return String.join("\n", lines);
    }
}

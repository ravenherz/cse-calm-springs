package com.ravenherz.rhzwe.dal.dto.basic.enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum ResourceType {
    IMAGE("images", Arrays.asList("jpg", "png")),
    AUDIO("audio", Arrays.asList("mp3")),
    INVALID;

    private final List<String> extensions;
    private final String path;

    ResourceType() {
        this.path = "invalid";
        this.extensions = new ArrayList<>();
    }

    ResourceType(String path, List<String> extensions) {
        this.path = path;
        this.extensions = extensions;
    }

    public static ResourceType getByFileName(String relativePath) {

        String[] pathParts = relativePath.trim().split("/");
        if (pathParts.length == 0) {
            return INVALID;
        }
        String[] nameParts = pathParts[pathParts.length-1].split("\\.");
        String extension = nameParts[nameParts.length-1].trim();
        if (nameParts.length < 2 || extension.length() == 0) {
            return INVALID;
        }
        for (ResourceType type : values()) {
            if (type.extensions.contains(extension)) {
                return type;
            }
        }
        return INVALID;
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public String getPath() {
        return path;
    }
}

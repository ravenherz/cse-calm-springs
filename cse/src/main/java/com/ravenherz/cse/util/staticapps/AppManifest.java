package com.ravenherz.cse.util.staticapps;

import com.fasterxml.jackson.databind.JsonNode;
import com.ravenherz.cse.util.Json;

public final class AppManifest {

    public static final String FILE_NAME = "version.manifest";

    private final String name;
    private final String version;
    private final String slug;
    private final String author;
    private final String company;
    private final String description;

    public AppManifest(String name, String version, String slug) {
        this(name, version, slug, null, null, null);
    }

    public AppManifest(String name, String version, String slug,
            String author, String company, String description) {
        this.name = blankToNull(name);
        this.version = blankToNull(version);
        this.slug = blankToNull(slug);
        this.author = blankToNull(author);
        this.company = blankToNull(company);
        this.description = blankToNull(description);
    }

    public static AppManifest parse(String text) {
        if (text == null) {
            return null;
        }
        String json = text.strip();
        if (json.startsWith("\uFEFF")) {
            json = json.substring(1).strip();
        }
        if (json.isEmpty()) {
            return null;
        }
        try {
            JsonNode node = Json.MAPPER.readTree(json);
            if (node == null || !node.isObject()) {
                return null;
            }
            AppManifest manifest = new AppManifest(
                    asText(node.get("name")),
                    asText(node.get("version")),
                    asText(node.get("slug")),
                    asText(node.get("author")),
                    asText(node.get("company")),
                    asText(node.get("description")));
            if (manifest.name == null && manifest.version == null && manifest.slug == null) {
                return null;
            }
            return manifest;
        } catch (Exception ex) {
            return null;
        }
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getSlug() {
        return slug;
    }

    public String getAuthor() {
        return author;
    }

    public String getCompany() {
        return company;
    }

    public String getDescription() {
        return description;
    }

    public String hint() {
        StringBuilder out = new StringBuilder();
        appendPart(out, name);
        appendPart(out, version);
        appendPart(out, slug);
        return out.length() == 0 ? null : out.toString();
    }

    private static void appendPart(StringBuilder out, String part) {
        if (part == null) {
            return;
        }
        if (out.length() > 0) {
            out.append(' ');
        }
        out.append(part);
    }

    private static String asText(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.isTextual() ? node.asText() : node.toString();
        return blankToNull(text);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}

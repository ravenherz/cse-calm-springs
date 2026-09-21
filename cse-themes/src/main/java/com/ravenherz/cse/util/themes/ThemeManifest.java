package com.ravenherz.cse.util.themes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public final class ThemeManifest {

    public static final String FILE_NAME = "theme.json";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final String id;
    private final String title;
    private final String author;
    private final String description;
    private final String engine;
    private final String shell;
    private final String defaultSchema;
    private final List<String> schemas;
    private final int sortOrder;

    public ThemeManifest(String id, String title, String author, String description, String engine,
            String shell, String defaultSchema, List<String> schemas) {
        this(id, title, author, description, engine, shell, defaultSchema, schemas, 0);
    }

    public ThemeManifest(String id, String title, String author, String description, String engine,
            String shell, String defaultSchema, List<String> schemas, int sortOrder) {
        this.id = blankToNull(id);
        this.title = blankToNull(title);
        this.author = blankToNull(author);
        this.description = blankToNull(description);
        this.engine = blankToNull(engine);
        this.shell = blankToNull(shell);
        this.defaultSchema = blankToNull(defaultSchema);
        this.schemas = schemas == null ? List.of() : List.copyOf(schemas);
        this.sortOrder = sortOrder;
    }

    public static ThemeManifest parse(String text) {
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
            JsonNode node = JSON.readTree(json);
            if (node == null || !node.isObject()) {
                return null;
            }
            List<String> schemas = new ArrayList<>();
            JsonNode schemasNode = node.get("schemas");
            if (schemasNode != null && schemasNode.isArray()) {
                for (JsonNode item : schemasNode) {
                    String value = asText(item);
                    if (value != null) {
                        schemas.add(value);
                    }
                }
            }
            ThemeManifest manifest = new ThemeManifest(
                    asText(node.get("id")),
                    asText(first(node, "title", "name")),
                    asText(node.get("author")),
                    asText(node.get("description")),
                    asText(node.get("engine")),
                    asText(node.get("shell")),
                    asText(first(node, "defaultSchema", "default-schema")),
                    schemas,
                    asInt(first(node, "sortOrder", "sort-order")));
            if (manifest.id == null && manifest.title == null && manifest.shell == null) {
                return null;
            }
            return manifest;
        } catch (Exception ex) {
            return null;
        }
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getDescription() {
        return description;
    }

    public String getEngine() {
        return engine;
    }

    public String getShell() {
        return shell;
    }

    public String getDefaultSchema() {
        return defaultSchema;
    }

    public List<String> getSchemas() {
        return schemas;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    private static JsonNode first(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode child = node.get(name);
            if (child != null && !child.isNull()) {
                return child;
            }
        }
        return null;
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

    private static int asInt(JsonNode node) {
        if (node == null || node.isNull()) {
            return 0;
        }
        if (node.isNumber()) {
            return node.asInt();
        }
        String text = asText(node);
        if (text == null) {
            return 0;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}

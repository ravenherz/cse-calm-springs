package com.ravenherz.cse.util.staticapps;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.ravenherz.cse.store.AppStoreAccess;
import com.ravenherz.cse.store.AppStoreNames;
import com.ravenherz.cse.store.AppStoreTableSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AppManifest {

    public static final String FILE_NAME = "version.manifest";
    private static final ObjectMapper JSON = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
            .build();

    private final String name;
    private final String version;
    private final String slug;
    private final String author;
    private final String company;
    private final String description;
    private final List<AppStoreTableSpec> storeTables;

    public AppManifest(String name, String version, String slug) {
        this(name, version, slug, null, null, null, List.of());
    }

    public AppManifest(String name, String version, String slug,
            String author, String company, String description) {
        this(name, version, slug, author, company, description, List.of());
    }

    public AppManifest(String name, String version, String slug,
            String author, String company, String description, List<AppStoreTableSpec> storeTables) {
        this.name = blankToNull(name);
        this.version = blankToNull(version);
        this.slug = blankToNull(slug);
        this.author = blankToNull(author);
        this.company = blankToNull(company);
        this.description = blankToNull(description);
        this.storeTables = storeTables == null ? List.of() : List.copyOf(storeTables);
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
            JsonNode node = JSON.readTree(json);
            if (node == null || !node.isObject()) {
                return null;
            }
            AppManifest manifest = new AppManifest(
                    asText(node.get("name")),
                    asText(node.get("version")),
                    asText(node.get("slug")),
                    asText(node.get("author")),
                    asText(node.get("company")),
                    asText(node.get("description")),
                    parseStoreTables(node.get("store")));
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

    public List<AppStoreTableSpec> getStoreTables() {
        return storeTables;
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

    private static List<AppStoreTableSpec> parseStoreTables(JsonNode store) {
        List<AppStoreTableSpec> tables = new ArrayList<>();
        if (store == null || !store.isObject()) {
            return tables;
        }
        JsonNode rows = store.get("tables");
        if (rows == null || !rows.isArray()) {
            return tables;
        }
        for (JsonNode row : rows) {
            if (row == null || !row.isObject()) {
                continue;
            }
            String name = asText(row.get("name"));
            if (!AppStoreNames.isTable(name)) {
                continue;
            }
            AppStoreAccess access = AppStoreAccess.parseOrDefault(asText(row.get("access")));
            Map<String, Object> schema = null;
            JsonNode schemaNode = row.get("schema");
            if (schemaNode != null && schemaNode.isObject()) {
                schema = JSON.convertValue(schemaNode, new TypeReference<>() {});
            }
            tables.add(new AppStoreTableSpec(name, access, schema));
        }
        return tables;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}

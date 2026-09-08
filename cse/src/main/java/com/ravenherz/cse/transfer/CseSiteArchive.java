package com.ravenherz.cse.transfer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class CseSiteArchive {

    private static final ObjectMapper JSON = JsonMapper.builder().build();
    private static final TypeReference<List<Map<String, Object>>> DOC_ARRAY = new TypeReference<>() {};

    private final JsonNode manifest;
    private final Map<String, List<Map<String, Object>>> collections;

    private CseSiteArchive(JsonNode manifest, Map<String, List<Map<String, Object>>> collections) {
        this.manifest = manifest;
        this.collections = collections;
    }

    JsonNode manifest() {
        return manifest;
    }

    List<Map<String, Object>> collection(String name) {
        return collections.getOrDefault(name, List.of());
    }

    static CseSiteArchive read(InputStream in) throws IOException {
        Map<String, byte[]> entries = unzip(in);
        byte[] manifestBytes = entries.get(CseSiteFormat.MANIFEST_ENTRY);
        if (manifestBytes == null) {
            throw new CseSiteImportException("Not a .csesite archive (missing manifest.json).");
        }
        JsonNode manifest = JSON.readTree(manifestBytes);
        if (manifest == null || !CseSiteFormat.FORMAT.equals(text(manifest.get("format")))) {
            throw new CseSiteImportException("Not a .csesite archive (format is not cse-site).");
        }
        int version = manifest.path("version").asInt(-1);
        if (version != CseSiteFormat.VERSION) {
            throw new CseSiteImportException("Unsupported .csesite version " + version + ".");
        }
        Map<String, List<Map<String, Object>>> collections = new LinkedHashMap<>();
        for (String name : CseSiteFormat.COLLECTIONS) {
            byte[] bytes = entries.get(CseSiteFormat.collectionEntry(name));
            if (bytes == null) {
                throw new CseSiteImportException("Archive is missing collections/" + name + ".json.");
            }
            List<Map<String, Object>> docs;
            try {
                docs = JSON.readValue(bytes, DOC_ARRAY);
            } catch (IOException e) {
                throw new CseSiteImportException("collections/" + name + ".json is not a JSON array.", e);
            }
            if (docs == null) {
                docs = List.of();
            }
            if (CseSiteReaders.looksLikeBson(docs)) {
                throw new CseSiteImportException(
                        "Refuse mongodump/BSON JSON ($oid / $date / _class) in " + name + ".");
            }
            collections.put(name, docs);
        }
        return new CseSiteArchive(manifest, collections);
    }

    private static Map<String, byte[]> unzip(InputStream in) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(in, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = normalize(entry.getName());
                if (name.isEmpty() || name.endsWith("/") || entry.isDirectory()) {
                    continue;
                }
                if (name.contains("..")) {
                    throw new CseSiteImportException("Refusing zip entry outside the archive: " + name);
                }
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                zip.transferTo(bytes);
                entries.put(name, bytes.toByteArray());
            }
        }
        if (entries.isEmpty()) {
            throw new CseSiteImportException("Not a .csesite archive (empty or not a zip).");
        }
        return entries;
    }

    private static String normalize(String name) {
        if (name == null) {
            return "";
        }
        String value = name.replace('\\', '/');
        while (value.startsWith("./")) {
            value = value.substring(2);
        }
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        return value;
    }

    private static String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }
}

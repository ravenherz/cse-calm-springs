package com.ravenherz.cse.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class Json {

    public static final ObjectMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
            .build();

    private Json() {
    }

    public static <T> T read(InputStream in, Class<T> type) throws IOException {
        return MAPPER.readValue(in, type);
    }

    public static <T> T read(InputStream in, TypeReference<T> type) throws IOException {
        return MAPPER.readValue(in, type);
    }

    public static Map<String, String> stringMap(String json) throws IOException {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        return stringMap(MAPPER.readTree(json));
    }

    public static Map<String, String> stringMap(InputStream in) throws IOException {
        return stringMap(MAPPER.readTree(in));
    }

    private static Map<String, String> stringMap(JsonNode node) {
        Map<String, String> out = new HashMap<>();
        if (node == null || !node.isObject()) {
            return out;
        }
        node.properties().forEach(entry -> {
            JsonNode value = entry.getValue();
            if (value == null || value.isNull()) {
                out.put(entry.getKey(), "");
            } else if (value.isTextual()) {
                out.put(entry.getKey(), value.asText());
            } else {
                out.put(entry.getKey(), value.toString());
            }
        });
        return out;
    }
}

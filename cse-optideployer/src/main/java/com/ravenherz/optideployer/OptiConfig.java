package com.ravenherz.optideployer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public record OptiConfig(
        String secret,
        String warPath,
        String context,
        String managerUrl,
        String username,
        String password) {

    public static OptiConfig read(InputStream in) throws IOException {
        JsonNode node = new ObjectMapper().readTree(in);
        String context = text(node, "context");
        if (context.equalsIgnoreCase("ROOT") || context.equals("/")) {
            context = "/";
        } else if (!context.startsWith("/")) {
            context = "/" + context;
        }
        String managerUrl = text(node, "managerUrl");
        while (managerUrl.endsWith("/")) {
            managerUrl = managerUrl.substring(0, managerUrl.length() - 1);
        }
        return new OptiConfig(
                text(node, "secret"),
                text(node, "warPath"),
                context,
                managerUrl,
                text(node, "username"),
                text(node, "password"));
    }

    public boolean matches(String given) {
        byte[] expected = secret.getBytes(StandardCharsets.UTF_8);
        byte[] actual = (given == null ? "" : given).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.asText().isBlank()) {
            throw new IllegalStateException("optideploy.json missing " + field);
        }
        return value.asText();
    }
}

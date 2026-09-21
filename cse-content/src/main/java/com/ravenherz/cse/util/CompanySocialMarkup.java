package com.ravenherz.cse.util;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Turns the legacy {@code company-social} string ({@code templateId:handle} pairs)
 * into {@code <cse-urls>} / {@code <cse-url>} markup for the footer.
 */
public final class CompanySocialMarkup {

    private static final Map<String, String> ALIASES = Map.of("twitter", "x");

    private CompanySocialMarkup() {
    }

    public static String toCseUrls(String raw) {
        return toCseUrls(raw, id -> true);
    }

    public static String toCseUrls(String raw, Predicate<String> include) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        Predicate<String> allow = include == null ? id -> true : include;
        String collapsed = raw.replaceAll(":+", ":");
        StringBuilder inner = new StringBuilder();
        Set<String> seen = new LinkedHashSet<>();
        for (String token : collapsed.split(" ")) {
            if (token == null || token.isBlank()) {
                continue;
            }
            String[] parts = token.split(":");
            if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
                continue;
            }
            String plugin = UrlTemplateIds.normalize(parts[0]);
            String templateId = UrlTemplateIds.normalize(ALIASES.getOrDefault(plugin, plugin));
            String id = parts[1].trim();
            if (!UrlTemplateIds.isValid(templateId) || id.isEmpty()) {
                continue;
            }
            if (!allow.test(templateId)) {
                continue;
            }
            if (!seen.add(templateId + "\0" + id)) {
                continue;
            }
            inner.append("<cse-url templateId=\"").append(attr(templateId))
                    .append("\" id=\"").append(attr(id)).append("\"></cse-url>");
        }
        if (inner.isEmpty()) {
            return "";
        }
        return "<cse-urls sizeOverride=\"s\">" + inner + "</cse-urls>";
    }

    private static String attr(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}

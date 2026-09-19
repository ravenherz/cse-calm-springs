package com.ravenherz.cse.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expands {@code <cse-md>} so its body is rendered with {@link MarkdownRenderer}.
 */
public final class CseMdProcessor {

    private static final Pattern TAG = Pattern.compile(
            "(?is)<cse-md\\b([^>]*)(?:\\s*/>|>(.*?)</cse-md>)");
    private static final Pattern ATTR = Pattern.compile(
            "(?i)\\b([a-zA-Z][\\w-]*)\\s*=\\s*[\"']([^\"']*)[\"']");
    private static final int MAX_PADDING_PX = 4096;

    private CseMdProcessor() {
    }

    public static String expand(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return markdown == null ? "" : markdown;
        }
        Matcher matcher = TAG.matcher(markdown);
        StringBuffer out = new StringBuffer();
        boolean found = false;
        while (matcher.find()) {
            found = true;
            String attrs = matcher.group(1) == null ? "" : matcher.group(1);
            String inner = matcher.group(2) == null ? "" : matcher.group(2);
            matcher.appendReplacement(out, Matcher.quoteReplacement(render(attrs, inner)));
        }
        if (!found) {
            return markdown;
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String render(String attrs, String inner) {
        int padding = paddingLeftPx(attr(attrs, "paddingLeft"));
        String html = MarkdownRenderer.renderDocument(expand(normalizeInner(inner)));
        return "<div class=\"cse-md\" style=\"padding-left:" + padding + "px\">" + html + "</div>";
    }

    static int paddingLeftPx(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        Matcher matcher = Pattern.compile("^\\s*(\\d{1,4})\\s*(px)?\\s*$", Pattern.CASE_INSENSITIVE)
                .matcher(raw);
        if (!matcher.matches()) {
            return 0;
        }
        return Math.min(Integer.parseInt(matcher.group(1)), MAX_PADDING_PX);
    }

    static String normalizeInner(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String text = raw.replace("\r\n", "\n").replace('\r', '\n');
        int start = text.startsWith("\n") ? 1 : 0;
        int end = text.length();
        if (end > start && text.charAt(end - 1) == '\n') {
            end--;
        }
        return dedent(text.substring(start, end));
    }

    private static String dedent(String text) {
        String[] lines = text.split("\n", -1);
        int min = Integer.MAX_VALUE;
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            int indent = 0;
            while (indent < line.length() && line.charAt(indent) == ' ') {
                indent++;
            }
            min = Math.min(min, indent);
        }
        if (min == Integer.MAX_VALUE || min == 0) {
            return text;
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                out.append('\n');
            }
            String line = lines[i];
            out.append(line.length() >= min ? line.substring(min) : line);
        }
        return out.toString();
    }

    private static String attr(String attrs, String name) {
        if (attrs == null || attrs.isBlank() || name == null) {
            return "";
        }
        Matcher matcher = ATTR.matcher(attrs);
        while (matcher.find()) {
            if (name.equalsIgnoreCase(matcher.group(1))) {
                return matcher.group(2);
            }
        }
        return "";
    }
}

package com.ravenherz.cse.pdf;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the public reading article's flex / Bootstrap rows into tables so
 * flying-saucer keeps photo, logos, and text beside each other at site sizes.
 */
final class PagePdfLayout {

    private static final Pattern COL = Pattern.compile(
            "\\bcol-(xs|sm|md|lg)-(\\d{1,2})\\b");

    private PagePdfLayout() {
    }

    static void prepare(Element root) {
        prepare(root, null);
    }

    static void prepare(Element root, String baseUrl) {
        prepare(root, baseUrl, null);
    }

    static void prepare(Element root, String baseUrl, String pageHref) {
        if (root == null) {
            return;
        }
        root.select(".reading-tags, .item-comments, .kicker, .export-pdf").remove();
        for (Element wrap : root.select(".reading-head, .reading-titles")) {
            wrap.unwrap();
        }
        for (Element custom : root.select("cse-md")) {
            custom.unwrap();
        }
        convertBootstrapRows(root);
        for (Element card : root.select("div.cv-img-card")) {
            flattenRow(card, ".cv-img-card-media", ".cv-card-body", "5mm", null);
        }
        for (Element embed : root.select("a.cse-embed, div.cse-embed")) {
            if (embed.selectFirst(".cse-embed-media") == null) {
                continue;
            }
            boolean compact = embed.hasClass("cse-embed-m");
            flattenRow(embed, ".cse-embed-media", ".cse-embed-body",
                    compact ? "8px" : "5mm", compact ? 32 : 112);
        }
        constrainImages(root);
        linkArticleHeader(root, pageHref);
        resolveHrefs(root, baseUrl);
        appendLinkIndex(root);
        applyUnbreakable(root);
    }

    private static void convertBootstrapRows(Element root) {
        Elements rows = root.select("div.row");
        for (int i = rows.size() - 1; i >= 0; i--) {
            convertRow(rows.get(i));
        }
    }

    private static void convertRow(Element row) {
        List<Element> cols = new ArrayList<>();
        for (Element child : row.children()) {
            if (columnSpan(child.className()) > 0) {
                cols.add(child);
            }
        }
        if (cols.isEmpty()) {
            return;
        }
        Element table = new Element("table");
        table.attr("class", ("pdf-row " + row.className()).trim());
        table.attr("cellpadding", "0");
        table.attr("cellspacing", "0");
        table.attr("style", "width:100%;table-layout:fixed;");
        Element tr = table.appendElement("tr");
        for (int i = 0; i < cols.size(); i++) {
            Element col = cols.get(i);
            int span = columnSpan(col.className());
            Element td = tr.appendElement("td");
            td.attr("class", ("pdf-col " + col.className()).trim());
            StringBuilder style = new StringBuilder("vertical-align:top;width:");
            style.append(String.format(Locale.US, "%.2f%%", span * 100.0 / 12.0));
            if (i > 0) {
                style.append(";padding-left:5mm");
            }
            td.attr("style", style.append(';').toString());
            td.html(col.html());
        }
        row.replaceWith(table);
    }

    static int columnSpan(String className) {
        int bestOrder = -1;
        int span = 0;
        Matcher matcher = COL.matcher(className == null ? "" : className);
        while (matcher.find()) {
            int order = switch (matcher.group(1)) {
                case "sm" -> 3;
                case "md" -> 2;
                case "lg" -> 1;
                default -> 0;
            };
            if (order >= bestOrder) {
                bestOrder = order;
                span = Integer.parseInt(matcher.group(2));
            }
        }
        return span;
    }

    private static void flattenRow(Element row, String mediaQuery, String bodyQuery,
            String gap, Integer mediaPx) {
        Element media = row.selectFirst(mediaQuery);
        if (media == null) {
            return;
        }
        Element body = row.selectFirst(bodyQuery);
        String href = "a".equalsIgnoreCase(row.tagName()) ? row.attr("href") : "";
        boolean compactLink = href != null && !href.isBlank() && mediaPx != null && mediaPx <= 32;
        if (compactLink) {
            if (body != null) {
                body.select(".cse-embed-desc, .cse-embed-meta").remove();
            }
            Element a = new Element("a");
            a.attr("class", (row.className() + " pdf-link").trim());
            a.attr("href", href);
            a.html(media.html() + (body == null ? "" : body.html()));
            sizeImages(a, mediaPx);
            row.replaceWith(a);
            return;
        }
        Element table = new Element("table");
        table.attr("class", row.className());
        table.attr("cellpadding", "0");
        table.attr("cellspacing", "0");
        Element tr = table.appendElement("tr");
        Element tdMedia = tr.appendElement("td");
        tdMedia.attr("class", media.className());
        tdMedia.attr("style", mediaStyle(media.attr("style"), mediaPx));
        tdMedia.html(media.html());
        if (mediaPx != null) {
            sizeImages(tdMedia, mediaPx);
        }
        if (body != null) {
            Element tdBody = tr.appendElement("td");
            tdBody.attr("class", body.className());
            tdBody.attr("style", "vertical-align:top;padding-left:" + gap);
            tdBody.html(body.html());
        }
        if (href != null && !href.isBlank()) {
            Element a = new Element("a");
            a.attr("class", "pdf-link");
            a.attr("href", href);
            a.appendChild(table);
            row.replaceWith(a);
        } else {
            row.replaceWith(table);
        }
    }

    private static void linkArticleHeader(Element root, String pageHref) {
        if (pageHref == null || pageHref.isBlank()) {
            return;
        }
        Element heading = firstArticleHeader(root);
        if (heading == null || heading.closest("a") != null || heading.selectFirst("a") != null) {
            return;
        }
        Element a = new Element("a");
        a.attr("class", "pdf-self pdf-link");
        a.attr("href", pageHref);
        for (org.jsoup.nodes.Node node : new ArrayList<>(heading.childNodes())) {
            a.appendChild(node);
        }
        heading.appendChild(a);
    }

    private static Element firstArticleHeader(Element root) {
        for (Element heading : root.select("h1")) {
            if (!heading.text().isBlank()) {
                return heading;
            }
        }
        return null;
    }

    private static void resolveHrefs(Element root, String baseUrl) {
        for (Element a : root.select("a[href]")) {
            String resolved = resolveHref(a.attr("href"), baseUrl);
            if (resolved != null && !resolved.isBlank()) {
                a.attr("href", resolved);
            }
        }
    }

    private static void appendLinkIndex(Element root) {
        Map<String, Integer> numbers = new LinkedHashMap<>();
        for (Element a : root.select("a[href]")) {
            if (a.closest(".pdf-links") != null) {
                continue;
            }
            String href = a.attr("href");
            if (!indexableHref(href)) {
                continue;
            }
            Integer n = numbers.get(href);
            if (n == null) {
                n = numbers.size() + 1;
                numbers.put(href, n);
            }
            if (a.selectFirst(".pdf-link-n") == null) {
                markLink(a, n);
            }
        }
        if (numbers.isEmpty()) {
            return;
        }
        Element section = root.appendElement("div");
        section.attr("class", "pdf-links pdf-unbreakable");
        section.appendElement("h2").text("Links");
        int i = 1;
        for (String href : numbers.keySet()) {
            section.appendElement("p").text("[" + i + "] " + href);
            i++;
        }
    }

    private static void markLink(Element a, int n) {
        a.addClass("pdf-link");
        Element mark = a.appendElement("span");
        mark.attr("class", "pdf-link-n");
        mark.attr("style", "vertical-align:top;color:#8a8d93;font-size:10px;font-weight:normal;"
                + "line-height:1;white-space:nowrap;padding-left:3px;");
        mark.text("[" + n + "]");
    }

    static boolean indexableHref(String href) {
        if (href == null || href.isBlank()) {
            return false;
        }
        String value = href.trim();
        return !value.startsWith("javascript:") && !value.startsWith("data:") && !value.equals("#");
    }

    static String resolveHref(String href, String baseUrl) {
        if (href == null || href.isBlank()) {
            return href;
        }
        String value = href.trim();
        if (value.startsWith("data:") || value.startsWith("mailto:") || value.startsWith("tel:")
                || value.startsWith("javascript:")) {
            return value;
        }
        try {
            java.net.URI uri = java.net.URI.create(value);
            if (uri.isAbsolute()) {
                return uri.toString();
            }
            if (baseUrl == null || baseUrl.isBlank()) {
                return value;
            }
            String base = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
            return java.net.URI.create(base).resolve(uri).toString();
        } catch (IllegalArgumentException ex) {
            return value;
        }
    }

    private static void applyUnbreakable(Element root) {
        Elements blocks = root.select(".pdf-unbreakable");
        for (int i = blocks.size() - 1; i >= 0; i--) {
            keepTogether(blocks.get(i));
        }
    }

    private static void keepTogether(Element block) {
        appendStyle(block, "page-break-inside:avoid;");
        if ("table".equalsIgnoreCase(block.tagName()) || alreadyBoxed(block)) {
            return;
        }
        Element table = new Element("table");
        table.attr("cellpadding", "0");
        table.attr("cellspacing", "0");
        table.attr("style", "width:100%;border-collapse:collapse;page-break-inside:avoid;");
        Element tr = table.appendElement("tr");
        tr.attr("style", "page-break-inside:avoid;");
        Element td = tr.appendElement("td");
        td.attr("style", "vertical-align:top;page-break-inside:avoid;");
        for (org.jsoup.nodes.Node node : new ArrayList<>(block.childNodes())) {
            td.appendChild(node);
        }
        block.appendChild(table);
    }

    private static boolean alreadyBoxed(Element block) {
        if (block.childrenSize() != 1) {
            return false;
        }
        Element child = block.child(0);
        return "table".equalsIgnoreCase(child.tagName())
                && child.attr("style").contains("page-break-inside:avoid");
    }

    private static void appendStyle(Element el, String extra) {
        String style = el.attr("style");
        StringBuilder next = new StringBuilder();
        if (style != null && !style.isBlank()) {
            next.append(style.trim());
            if (next.charAt(next.length() - 1) != ';') {
                next.append(';');
            }
        }
        next.append(extra);
        if (!extra.endsWith(";")) {
            next.append(';');
        }
        el.attr("style", next.toString());
    }

    private static void constrainImages(Element root) {
        for (Element img : root.select("a.cse-url-s img, .cse-url-s img")) {
            sizeImg(img, 42);
        }
        for (Element img : root.select("table.pdf-row td.pdf-col .cse-image img")) {
            String style = img.attr("style");
            StringBuilder next = new StringBuilder();
            if (style != null && !style.isBlank()) {
                next.append(style.trim());
                if (next.charAt(next.length() - 1) != ';') {
                    next.append(';');
                }
            }
            next.append("width:100%;max-width:100%;height:auto;margin-bottom:5mm;");
            img.attr("style", next.toString());
        }
    }

    private static void sizeImages(Element media, int px) {
        for (Element img : media.select("img")) {
            sizeImg(img, px);
        }
    }

    private static void sizeImg(Element img, int px) {
        img.attr("width", String.valueOf(px));
        img.attr("height", String.valueOf(px));
        String style = img.attr("style");
        StringBuilder next = new StringBuilder();
        if (style != null && !style.isBlank()) {
            next.append(style.trim());
            if (next.charAt(next.length() - 1) != ';') {
                next.append(';');
            }
        }
        next.append("width:").append(px).append("px;");
        next.append("height:").append(px).append("px;");
        next.append("max-width:").append(px).append("px;");
        next.append("max-height:").append(px).append("px;");
        img.attr("style", next.toString());
    }

    private static String mediaStyle(String existing, Integer mediaPx) {
        StringBuilder style = new StringBuilder();
        if (existing != null && !existing.isBlank()) {
            style.append(existing.trim());
            if (style.charAt(style.length() - 1) != ';') {
                style.append(';');
            }
        }
        style.append("vertical-align:top;");
        if (mediaPx != null) {
            style.append("width:").append(mediaPx).append("px;");
            style.append("height:").append(mediaPx).append("px;");
        }
        return style.toString();
    }
}

package com.ravenherz.cse.pdf;

/**
 * Canonical public {@code <article class="reading">} markup for PDF export.
 * Header, subheader, optional cover, and expanded body — not kicker, tags, or comments.
 */
final class PagePdfArticle {

    private PagePdfArticle() {
    }

    public static String html(ArticleSource page) {
        StringBuilder html = new StringBuilder(512);
        html.append("<article class=\"reading\">");
        String header = page == null ? null : page.header();
        if (header != null && !header.isBlank()) {
            html.append("<h1><span>").append(escape(header)).append("</span></h1>");
            String sub = page.subHeader();
            if (sub != null && !sub.isBlank()) {
                html.append("<p class=\"reading-sub\">").append(escape(sub)).append("</p>");
            }
        }
        if (showFeatured(page)) {
            html.append("<figure class=\"reading-media\"><img src=\"")
                    .append(escapeAttr(page.imageLinkFull()))
                    .append("\" alt=\"\"/></figure>");
        }
        String body = page == null ? null : page.description();
        html.append("<div class=\"piece-body item-description-content\">");
        if (body != null && !body.isBlank()) {
            html.append(body);
        }
        html.append("</div></article>");
        return html.toString();
    }

    static boolean showFeatured(ArticleSource page) {
        if (page == null || page.album() || page.noTopDisplayImage()) {
            return false;
        }
        String src = page.imageLinkFull();
        return src != null && !src.isBlank() && !isPlaceholderImage(src);
    }

    static boolean isPlaceholderImage(String src) {
        if (src == null) {
            return true;
        }
        String path = src.replace('\\', '/').toLowerCase();
        return path.contains("cse-core/images/no-image") || path.endsWith("/no-image.jpg");
    }

    static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    static String escapeAttr(String value) {
        return escape(value);
    }
}

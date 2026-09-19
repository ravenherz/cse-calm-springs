package com.ravenherz.cse.util;

import com.ravenherz.cse.dal.EntityAccess;
import com.ravenherz.cse.dal.dao.UrlTemplateService;
import com.ravenherz.cse.dal.dto.UrlTemplateEntity;
import com.ravenherz.cse.dal.dto.basic.UrlTemplateData;
import com.ravenherz.cse.dal.dto.basic.enums.AccessType;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expands {@code <cse-url>} and {@code <cse-urls>} tags left in page Markdown/HTML.
 */
@Component
public class UrlEmbedProcessor {

    public interface Lookup {
        UrlTemplateEntity findByTemplateId(String templateId);
    }

    private static final Pattern URL_TAG = Pattern.compile(
            "(?is)<cse-url\\b([^>]*)(?:\\s*/>|>\\s*</cse-url>)");
    private static final Pattern URLS_TAG = Pattern.compile(
            "(?is)<cse-urls\\b([^>]*)(?:\\s*/>|>(.*?)</cse-urls>)");
    private static final Pattern ATTR = Pattern.compile(
            "(?i)\\b([a-zA-Z][\\w-]*)\\s*=\\s*[\"']([^\"']*)[\"']");
    private static final UrlTemplateSize DEFAULT_SIZE = UrlTemplateSize.M;

    private static volatile UrlEmbedProcessor instance;

    private final Lookup lookup;

    @Autowired
    public UrlEmbedProcessor(ObjectProvider<UrlTemplateService> urlTemplateService) {
        UrlTemplateService service = urlTemplateService == null ? null : urlTemplateService.getIfAvailable();
        this.lookup = service == null ? id -> null : service::getByUrlTemplateId;
    }

    public static UrlEmbedProcessor of(Lookup lookup) {
        return new UrlEmbedProcessor(lookup);
    }

    private UrlEmbedProcessor(Lookup lookup) {
        this.lookup = lookup == null ? id -> null : lookup;
    }

    @PostConstruct
    void register() {
        instance = this;
    }

    @PreDestroy
    void unregister() {
        if (instance == this) {
            instance = null;
        }
    }

    public static String expand(String html) {
        if (html == null || html.isEmpty()) {
            return html == null ? "" : html;
        }
        UrlEmbedProcessor processor = instance;
        if (processor == null) {
            return html;
        }
        return processor.expandHtml(html);
    }

    public String expandHtml(String html) {
        if (html == null || html.isEmpty()) {
            return html == null ? "" : html;
        }
        String withContainers = expandContainers(html);
        return expandUrls(withContainers, null);
    }

    private String expandContainers(String html) {
        Matcher matcher = URLS_TAG.matcher(html);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String attrs = matcher.group(1) == null ? "" : matcher.group(1);
            String inner = matcher.group(2) == null ? "" : matcher.group(2);
            UrlTemplateSize override = UrlTemplateSize.parse(attr(attrs, "sizeOverride"), null);
            String expanded = expandUrls(inner, override);
            String sizeClass = override == null ? "" : " cse-urls-" + override.css();
            String replacement = "<div class=\"cse-urls" + sizeClass + "\">" + expanded + "</div>";
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private String expandUrls(String html, UrlTemplateSize sizeOverride) {
        if (html == null || html.isEmpty()) {
            return html == null ? "" : html;
        }
        Matcher matcher = URL_TAG.matcher(html);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String attrs = matcher.group(1) == null ? "" : matcher.group(1);
            matcher.appendReplacement(out, Matcher.quoteReplacement(render(attrs, sizeOverride)));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private String render(String attrs, UrlTemplateSize sizeOverride) {
        String templateId = UrlTemplateIds.normalize(attr(attrs, "templateId"));
        String id = attr(attrs, "id");
        if (id == null) {
            id = "";
        } else {
            id = id.trim();
        }
        UrlTemplateSize size = sizeOverride != null
                ? sizeOverride
                : UrlTemplateSize.parse(attr(attrs, "size"), DEFAULT_SIZE);
        String textOverride = attr(attrs, "textOverride");
        String secondLine = attr(attrs, "secondLine");
        String overrideUrl = attr(attrs, "overrideUrl");
        if (!UrlTemplateIds.isValid(templateId)) {
            return missing(templateId);
        }
        UrlTemplateEntity entity = lookup.findByTemplateId(templateId);
        if (entity == null || !publiclyReadable(entity)) {
            return missing(templateId);
        }
        UrlTemplateData data = entity.getUrlTemplateData() == null
                ? new UrlTemplateData() : entity.getUrlTemplateData();
        String href = hrefOf(data.getUrlPattern(), id, overrideUrl);
        String text = displayText(data.getUrlDefaultText(), id, textOverride);
        String image = data.getUrlImage() == null ? "" : data.getUrlImage().trim();
        return switch (size) {
            case XS -> renderXs(href, text);
            case S -> renderS(href, text, image);
            case M -> renderBlock(href, text, image, secondLine, false);
            case L -> renderBlock(href, text, image, secondLine, true);
        };
    }

    private static String renderXs(String href, String text) {
        return "<a class=\"cse-url cse-url-xs\" href=\"" + escape(href) + "\""
                + " target=\"_blank\" rel=\"noopener noreferrer\">"
                + escape(text) + "</a>";
    }

    private static String renderS(String href, String text, String image) {
        StringBuilder html = new StringBuilder();
        html.append("<a class=\"cse-url cse-url-s\" href=\"").append(escape(href)).append("\"")
                .append(" target=\"_blank\" rel=\"noopener noreferrer\"")
                .append(" title=\"").append(escape(text)).append("\">");
        if (!image.isBlank()) {
            html.append("<img src=\"").append(escape(image)).append("\" alt=\"")
                    .append(escape(text)).append("\" width=\"18\" height=\"18\"/>");
        } else {
            html.append(escape(text));
        }
        html.append("</a>");
        return html.toString();
    }

    private static String renderBlock(String href, String text, String image, String secondLine,
            boolean wide) {
        String sizeClass = wide ? "cse-url-l" : "cse-url-m";
        StringBuilder html = new StringBuilder();
        html.append("<a class=\"cse-url ").append(sizeClass).append("\" href=\"")
                .append(escape(href)).append("\" target=\"_blank\" rel=\"noopener noreferrer\">");
        html.append("<span class=\"cse-url-media\">");
        if (!image.isBlank()) {
            html.append("<img src=\"").append(escape(image)).append("\" alt=\"\"/>");
        } else {
            html.append("<span class=\"cse-url-placeholder\" aria-hidden=\"true\"></span>");
        }
        html.append("</span><span class=\"cse-url-body\">");
        html.append("<span class=\"cse-url-title\">").append(escape(text)).append("</span>");
        if (secondLine != null && !secondLine.isBlank()) {
            html.append("<span class=\"cse-url-second\">").append(escape(secondLine.trim())).append("</span>");
        }
        html.append("</span></a>");
        return html.toString();
    }

    private static String displayText(String defaultText, String id, String textOverride) {
        if (textOverride != null && !textOverride.isBlank()) {
            return textOverride.trim();
        }
        if (defaultText != null && !defaultText.isBlank()) {
            return substitute(defaultText, id);
        }
        return id == null ? "" : id;
    }

    private static String hrefOf(String pattern, String id, String overrideUrl) {
        if (overrideUrl != null && !overrideUrl.isBlank()) {
            String href = overrideUrl.trim();
            return unsafeHref(href) ? "#" : href;
        }
        if (pattern == null || pattern.isBlank()) {
            return "#";
        }
        String href = substitute(pattern.trim(), hrefId(id));
        if (href.isBlank() || unsafeHref(href)) {
            return "#";
        }
        return href;
    }

    private static boolean unsafeHref(String href) {
        String lower = href.trim().toLowerCase(Locale.ROOT);
        return lower.startsWith("javascript:")
                || lower.startsWith("vbscript:")
                || lower.startsWith("data:");
    }

    private static String substitute(String pattern, String id) {
        String value = id == null ? "" : id;
        return pattern.replace("%s", value);
    }

    private static String hrefId(String id) {
        if (id == null || id.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < id.length(); i++) {
            char ch = id.charAt(i);
            if (ch == ' ') {
                out.append("%20");
            } else if (ch == '"' || ch == '<' || ch == '>' || ch == '&' || ch == '#' || ch == '?') {
                out.append('%');
                out.append(String.format("%02X", (int) ch));
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }

    private static String missing(String templateId) {
        String id = templateId == null ? "" : templateId;
        return "<div class=\"cse-url cse-url-missing\" data-template=\""
                + escape(id) + "\">URL template not found</div>";
    }

    private static boolean publiclyReadable(UrlTemplateEntity entity) {
        if (entity.getHistoryData() == null || entity.getHistoryData().getEvents() == null) {
            return true;
        }
        return EntityAccess.isAccessible(entity, AccessType.ACCESS_READ, null);
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

    private static String escape(String value) {
        return PlaylistEmbedProcessor.escape(value);
    }
}

package com.ravenherz.cse.pdf;

import com.ravenherz.cse.dal.dto.AccountEntity;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

/**
     * Turns an article into a PDF using flying-saucer + OpenPDF.
 */
public class PagePdfRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PagePdfRenderer.class);
    private static final String CSS = loadCss();

    public interface MediaLoader {
        byte[] load(String src, AccountEntity accessor);
    }

    private final MediaLoader media;

    public PagePdfRenderer(MediaLoader media) {
        this.media = media == null ? (src, accessor) -> null : media;
    }

    public byte[] render(ArticleSource page, AccountEntity accessor) throws IOException {
        return render(page, accessor, null);
    }

    public byte[] render(ArticleSource page, AccountEntity accessor, String baseUrl) throws IOException {
        String article = PagePdfArticle.html(page);
        String pageHref = page == null ? null : page.pageLink();
        String xhtml = toXhtml(inlineImages(article, accessor, baseUrl, pageHref));
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(xhtml, documentBase(baseUrl));
        renderer.layout();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            renderer.createPDF(out);
        } catch (Exception ex) {
            throw new IOException("Could not render page PDF", ex);
        }
        return out.toByteArray();
    }

    String inlineImages(String articleHtml, AccountEntity accessor) {
        return inlineImages(articleHtml, accessor, null);
    }

    String inlineImages(String articleHtml, AccountEntity accessor, String baseUrl) {
        return inlineImages(articleHtml, accessor, baseUrl, null);
    }

    String inlineImages(String articleHtml, AccountEntity accessor, String baseUrl, String pageHref) {
        Document doc = Jsoup.parseBodyFragment(articleHtml == null ? "" : articleHtml);
        doc.select("script, iframe, object, embed, audio, video").remove();
        PagePdfLayout.prepare(doc.body(), baseUrl, pageHref);
        for (Element img : doc.select("img[src]")) {
            String src = img.attr("src");
            if (src.startsWith("data:")) {
                continue;
            }
            byte[] bytes = media.load(src, accessor);
            if (bytes == null || bytes.length == 0) {
                img.remove();
                continue;
            }
            img.attr("src", "data:" + mime(src) + ";base64," + Base64.getEncoder().encodeToString(bytes));
        }
        doc.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.xhtml)
                .prettyPrint(false);
        return doc.body().html();
    }

    static String toXhtml(String articleInner) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head>"
                + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>"
                + "<style type=\"text/css\">" + CSS + "</style></head><body>"
                + (articleInner == null ? "" : articleInner)
                + "</body></html>";
    }

    static String documentBase(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://cse.local/";
        }
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    public static String publicPathFromSrc(String src) {
        if (src == null || src.isBlank()) {
            return "";
        }
        String path = src.trim().replace('\\', '/');
        int cut = path.indexOf('?');
        if (cut >= 0) {
            path = path.substring(0, cut);
        }
        cut = path.indexOf('#');
        if (cut >= 0) {
            path = path.substring(0, cut);
        }
        String rest;
        if (path.startsWith("./content-protected")) {
            rest = path.substring("./content-protected".length());
        } else {
            int marker = path.indexOf("/content-protected");
            if (marker < 0) {
                return "";
            }
            rest = path.substring(marker + "/content-protected".length());
        }
        if (rest.isEmpty()) {
            return "";
        }
        return rest.startsWith("/") ? rest : "/" + rest;
    }

    private static String mime(String src) {
        String path = src == null ? "" : src.toLowerCase(Locale.ROOT);
        int q = path.indexOf('?');
        if (q >= 0) {
            path = path.substring(0, q);
        }
        if (path.endsWith(".png")) {
            return "image/png";
        }
        if (path.endsWith(".gif")) {
            return "image/gif";
        }
        if (path.endsWith(".webp")) {
            return "image/webp";
        }
        if (path.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "image/jpeg";
    }

    private static String loadCss() {
        try (InputStream in = PagePdfRenderer.class.getResourceAsStream("/pdf-article.css")) {
            if (in == null) {
                return "";
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            LOGGER.warn("Could not load pdf-article.css: {}", ex.getMessage());
            return "";
        }
    }
}

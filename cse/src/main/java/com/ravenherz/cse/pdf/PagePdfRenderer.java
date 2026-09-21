package com.ravenherz.cse.pdf;

import com.ravenherz.cse.dal.EntityAccess;
import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.dao.ResourceService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.basic.ResourceData;
import com.ravenherz.cse.dal.dto.basic.enums.AccessType;
import com.ravenherz.cse.dal.dto.events.PageEvent;
import com.ravenherz.cse.util.io.CseDisk;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Locale;

/**
 * Turns a {@link PageEvent} article into a PDF using flying-saucer + OpenPDF.
 */
@Component
public class PagePdfRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PagePdfRenderer.class);
    private static final String CSS = loadCss();

    interface MediaLoader {
        byte[] load(String src, AccountEntity accessor);
    }

    private final MediaLoader media;

    @Autowired
    public PagePdfRenderer(ServiceProvider serviceProvider) {
        this((src, accessor) -> loadProtected(serviceProvider, src, accessor));
    }

    PagePdfRenderer(MediaLoader media) {
        this.media = media == null ? (src, accessor) -> null : media;
    }

    public byte[] render(PageEvent page, AccountEntity accessor) throws IOException {
        return render(page, accessor, null);
    }

    public byte[] render(PageEvent page, AccountEntity accessor, String baseUrl) throws IOException {
        String article = PagePdfArticle.html(page);
        String pageHref = page == null ? null : page.getPageLink();
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

    static String publicBaseUrl(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String proto = headerFirst(request, "X-Forwarded-Proto");
        if (proto == null || proto.isBlank()) {
            proto = request.isSecure() ? "https" : request.getScheme();
        }
        String host = headerFirst(request, "X-Forwarded-Host");
        if (host == null || host.isBlank()) {
            host = headerFirst(request, "Host");
        }
        if (host == null || host.isBlank()) {
            host = request.getServerName();
            int port = request.getServerPort();
            boolean standard = ("http".equalsIgnoreCase(proto) && port == 80)
                    || ("https".equalsIgnoreCase(proto) && port == 443)
                    || port <= 0;
            if (!standard) {
                host = host + ":" + port;
            }
        }
        String context = request.getContextPath();
        if (context == null || "/".equals(context)) {
            context = "";
        } else if (context.endsWith("/")) {
            context = context.substring(0, context.length() - 1);
        }
        return proto + "://" + host + context + "/";
    }

    static String documentBase(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://cse.local/";
        }
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    private static String headerFirst(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            return null;
        }
        int comma = value.indexOf(',');
        return comma < 0 ? value.trim() : value.substring(0, comma).trim();
    }

    static String publicPathFromSrc(String src) {
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

    private static byte[] loadProtected(ServiceProvider services, String src, AccountEntity accessor) {
        if (services == null) {
            return null;
        }
        String publicPath = publicPathFromSrc(src);
        if (publicPath.isEmpty()) {
            return null;
        }
        ResourceService resources = services.getResourceService();
        if (resources == null) {
            return null;
        }
        ResourceEntity entity = resources.getByPublicPath(publicPath);
        if (entity == null || !EntityAccess.isAccessible(entity, AccessType.ACCESS_READ, accessor)) {
            return null;
        }
        ResourceData data = entity.dataForPublicPath(publicPath);
        if (data == null) {
            return null;
        }
        byte[] cached = cachedBytes(data.getPathProtected());
        if (cached != null) {
            return cached;
        }
        byte[] inline = ResourceEntity.bytesOf(data);
        if (inline != null) {
            return inline;
        }
        if (data.isLargeFile() && data.getDataChunkIds() != null && !data.getDataChunkIds().isEmpty()) {
            return resources.getRawBytesFromChunks(data.getDataChunkIds());
        }
        return null;
    }

    private static byte[] cachedBytes(String pathProtected) {
        if (pathProtected == null || pathProtected.isBlank()) {
            return null;
        }
        try {
            var file = CseDisk.cachedMedia(pathProtected);
            if (file != null && file.isFile()) {
                return Files.readAllBytes(file.toPath());
            }
        } catch (Exception ex) {
            LOGGER.debug("PDF media cache miss for {}: {}", pathProtected, ex.getMessage());
        }
        return null;
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

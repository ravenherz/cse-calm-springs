package com.ravenherz.cse.engine.pdf;

import com.ravenherz.cse.pdf.ArticleSource;
import com.ravenherz.cse.pdf.PagePdfRenderer;

import com.ravenherz.cse.controller.AuthSupport;
import com.ravenherz.cse.dal.EntityAccess;
import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.basic.enums.AccessType;
import com.ravenherz.cse.dal.dto.events.PageEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PagePdfExport {

    private static final Logger LOGGER = LoggerFactory.getLogger(PagePdfExport.class);

    private final ServiceProvider serviceProvider;
    private final AuthSupport authSupport;
    private final PagePdfRenderer renderer;

    @Autowired
    public PagePdfExport(ServiceProvider serviceProvider, AuthSupport authSupport, PagePdfMediaLoader media) {
        this(serviceProvider, authSupport, new PagePdfRenderer(media));
    }

    PagePdfExport(ServiceProvider serviceProvider, AuthSupport authSupport, PagePdfRenderer renderer) {
        this.serviceProvider = serviceProvider;
        this.authSupport = authSupport;
        this.renderer = renderer;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, String page)
            throws IOException {
        String name = page == null ? "" : page.trim();
        if (name.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        ItemEntity item = serviceProvider.getItemService().getByName(name);
        if (item == null || item.isAlbum() || item.getPageData() == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (!EntityAccess.isAccessible(item, AccessType.ACCESS_READ, accessor)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (!item.getPageData().isExportPdf()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        byte[] pdf;
        try {
            PageEvent event = PageEvent.PageEventConverter.toEvent(item);
            pdf = renderer.render(new ArticleSource(
                    event.getHeader(),
                    event.getSubHeader(),
                    event.getDescription(),
                    event.getImageLinkFull(),
                    event.isAlbum(),
                    event.isNoTopDisplayImage(),
                    event.getPageLink()), accessor, publicBaseUrl(request));
        } catch (Exception ex) {
            LOGGER.error("PDF export failed for {}", name, ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        if (pdf == null || pdf.length == 0) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename(name) + "\"");
        response.setContentLength(pdf.length);
        response.getOutputStream().write(pdf);
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

    private static String headerFirst(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            return null;
        }
        int comma = value.indexOf(',');
        return comma < 0 ? value.trim() : value.substring(0, comma).trim();
    }

    static String filename(String uri) {
        String base = uri == null ? "" : uri.replaceAll("[^A-Za-z0-9._-]", "_");
        if (base.isBlank()) {
            base = "page";
        }
        return base + ".pdf";
    }
}

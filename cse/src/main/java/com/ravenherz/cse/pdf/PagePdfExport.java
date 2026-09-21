package com.ravenherz.cse.pdf;

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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PagePdfExport {

    private static final Logger LOGGER = LoggerFactory.getLogger(PagePdfExport.class);

    private final ServiceProvider serviceProvider;
    private final AuthSupport authSupport;
    private final PagePdfRenderer renderer;

    public PagePdfExport(ServiceProvider serviceProvider, AuthSupport authSupport, PagePdfRenderer renderer) {
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
            pdf = renderer.render(PageEvent.PageEventConverter.toEvent(item), accessor,
                    PagePdfRenderer.publicBaseUrl(request));
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

    static String filename(String uri) {
        String base = uri == null ? "" : uri.replaceAll("[^A-Za-z0-9._-]", "_");
        if (base.isBlank()) {
            base = "page";
        }
        return base + ".pdf";
    }
}

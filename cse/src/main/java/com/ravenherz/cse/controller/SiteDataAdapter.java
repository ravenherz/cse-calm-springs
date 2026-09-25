package com.ravenherz.cse.controller;

import com.ravenherz.cse.admin.EditorChrome;
import com.ravenherz.cse.admin.SiteDataDesk;
import com.ravenherz.cse.dal.DataProvider;
import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.engine.util.Settings;
import com.ravenherz.cse.install.SetupAppBootstrap;
import com.ravenherz.cse.install.SiteReady;
import com.ravenherz.cse.transfer.CseSiteExporter;
import com.ravenherz.cse.transfer.CseSiteFormat;
import com.ravenherz.cse.transfer.CseSiteImportException;
import com.ravenherz.cse.transfer.CseSiteImporter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;

@Component
public class SiteDataAdapter implements SiteDataDesk {

    private final AuthSupport authSupport;
    private final EditorChrome editorChrome;
    private final DataProvider dataProvider;
    private final CseSiteExporter exporter;
    private final CseSiteImporter importer;
    private final ServiceProvider serviceProvider;
    private final Settings settings;
    private final SiteReady siteReadyGate;
    private final SetupAppBootstrap setupAppBootstrap;

    public SiteDataAdapter(AuthSupport authSupport, EditorChrome editorChrome, DataProvider dataProvider,
            CseSiteExporter exporter, CseSiteImporter importer, ServiceProvider serviceProvider, Settings settings,
            SiteReady siteReady, SetupAppBootstrap setupAppBootstrap) {
        this.authSupport = authSupport;
        this.editorChrome = editorChrome;
        this.dataProvider = dataProvider;
        this.exporter = exporter;
        this.importer = importer;
        this.serviceProvider = serviceProvider;
        this.settings = settings;
        this.siteReadyGate = siteReady;
        this.setupAppBootstrap = setupAppBootstrap;
    }

    @Override
    public void export(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!dataProvider.ping()) {
            response.sendRedirect(request.getContextPath() + "/editor/site-data?error=db");
            return;
        }
        String filename = "cse-site-" + LocalDate.now(ZoneOffset.UTC) + "." + CseSiteFormat.EXTENSION;
        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        exporter.write(response.getOutputStream(), dataProvider, serviceProvider, "mongodb",
                settings.listPersistedContexts());
        response.flushBuffer();
    }

    @Override
    public String importArchive(MultipartFile file, String confirm, Model model, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (!dataProvider.ping()) {
            return open(model, request, response, "Database is not reachable.");
        }
        if (!"replace".equals(confirm)) {
            return open(model, request, response, "Tick the box to replace CMS documents with this archive.");
        }
        if (file == null || file.isEmpty()) {
            return open(model, request, response, ".csesite file is required.");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null
                || !originalFilename.toLowerCase(Locale.ROOT).endsWith("." + CseSiteFormat.EXTENSION)) {
            return open(model, request, response, "Only .csesite files are accepted.");
        }
        try (InputStream in = file.getInputStream()) {
            importer.apply(in, dataProvider.getMongoTemplate(), settings);
        } catch (CseSiteImportException e) {
            return open(model, request, response, e.getMessage());
        }
        siteReadyGate.refreshAfterDataChange();
        authSupport.deleteAuthCookies(request, response);
        if (!siteReadyGate.isConfigured()) {
            setupAppBootstrap.explodeInstaller();
            response.sendRedirect(request.getContextPath() + "/apps/setup/");
            return null;
        }
        response.sendRedirect(request.getContextPath() + "/");
        return null;
    }

    @Override
    public String open(Model model, HttpServletRequest request, HttpServletResponse response, String error)
            throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        editorChrome.apply(model, accessor);
        boolean ready = dataProvider.ping();
        model.addAttribute("dbReady", ready);
        if (error != null) {
            model.addAttribute("error", error);
        } else if (!ready || "db".equals(request.getParameter("error"))) {
            model.addAttribute("error", "Database is not reachable.");
        }
        return "/admin/editor-site-data";
    }
}

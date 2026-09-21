package com.ravenherz.cse.controller;

import com.ravenherz.cse.dal.DataProvider;
import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.install.SetupAppBootstrap;
import com.ravenherz.cse.install.SiteReady;
import com.ravenherz.cse.transfer.CseSiteExporter;
import com.ravenherz.cse.transfer.CseSiteFormat;
import com.ravenherz.cse.transfer.CseSiteImportException;
import com.ravenherz.cse.transfer.CseSiteImporter;
import com.ravenherz.cse.engine.util.Settings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;

@Controller
@RequestMapping("/editor/site-data")
public class EditorSiteDataController extends AbstractController {

    private final DataProvider dataProvider;
    private final CseSiteExporter exporter;
    private final CseSiteImporter importer;
    private final SiteReady siteReadyGate;
    private final SetupAppBootstrap setupAppBootstrap;

    public EditorSiteDataController(DataProvider dataProvider, CseSiteExporter exporter,
            CseSiteImporter importer, ServiceProvider serviceProvider, Settings settings,
            SiteReady siteReady, SetupAppBootstrap setupAppBootstrap) {
        this.dataProvider = dataProvider;
        this.exporter = exporter;
        this.importer = importer;
        this.serviceProvider = serviceProvider;
        this.settings = settings;
        this.siteReadyGate = siteReady;
        this.setupAppBootstrap = setupAppBootstrap;
    }

    @GetMapping
    public String page(Model model, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        return render(model, request, response, null);
    }

    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return;
        }
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

    @PostMapping("/import")
    public String importSite(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "confirm", required = false) String confirm,
            Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        if (!dataProvider.ping()) {
            return render(model, request, response, "Database is not reachable.");
        }
        if (!"replace".equals(confirm)) {
            return render(model, request, response,
                    "Tick the box to replace CMS documents with this archive.");
        }
        if (file == null || file.isEmpty()) {
            return render(model, request, response, ".csesite file is required.");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null
                || !originalFilename.toLowerCase(Locale.ROOT).endsWith("." + CseSiteFormat.EXTENSION)) {
            return render(model, request, response, "Only .csesite files are accepted.");
        }
        try (InputStream in = file.getInputStream()) {
            importer.apply(in, dataProvider.getMongoTemplate(), settings);
        } catch (CseSiteImportException e) {
            return render(model, request, response, e.getMessage());
        }
        siteReadyGate.refreshAfterDataChange();
        deleteAuthCookies(request, response);
        if (!siteReadyGate.isConfigured()) {
            setupAppBootstrap.explodeInstaller();
            response.sendRedirect(request.getContextPath() + "/apps/setup/");
            return null;
        }
        response.sendRedirect(request.getContextPath() + "/");
        return null;
    }

    private String render(Model model, HttpServletRequest request, HttpServletResponse response, String error)
            throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        addEditorChrome(model, accessor);
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

package com.ravenherz.cse.admin;

import com.ravenherz.cse.security.AccountAccessor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
@RequestMapping("/editor/site-data")
public class EditorSiteDataController {

    private final AccountAccessor authSupport;
    private final SiteDataDesk siteData;

    public EditorSiteDataController(AccountAccessor authSupport, SiteDataDesk siteData) {
        this.authSupport = authSupport;
        this.siteData = siteData;
    }

    @GetMapping
    public String page(Model model, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (authSupport.getAccessor(request, response) == null) {
            return null;
        }
        return siteData.open(model, request, response, null);
    }

    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (authSupport.getAccessor(request, response) == null) {
            return;
        }
        siteData.export(request, response);
    }

    @PostMapping("/import")
    public String importSite(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "confirm", required = false) String confirm,
            Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (authSupport.getAccessor(request, response) == null) {
            return null;
        }
        return siteData.importArchive(file, confirm, model, request, response);
    }
}

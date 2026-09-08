package com.ravenherz.cse.controller.publicsite;

import com.ravenherz.cse.controller.AuthSupport;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.install.SiteReady;
import com.ravenherz.cse.util.Settings;
import com.ravenherz.cse.util.helpers.HttpErrorHelper;
import com.ravenherz.cse.util.html.CustomHtmlTag;
import com.ravenherz.cse.util.themes.ThemeCatalog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class PublicSiteApi {

    private final PublicIndexModel publicIndexModel;
    private final Settings settings;
    private final SiteReady siteReady;
    private final ThemeCatalog themeCatalog;
    private final HttpErrorHelper httpErrorHelper;
    private final AuthSupport authSupport;
    private final List<CustomHtmlTag> tags;

    public PublicSiteApi(PublicIndexModel publicIndexModel, Settings settings, SiteReady siteReady,
            ThemeCatalog themeCatalog, HttpErrorHelper httpErrorHelper, AuthSupport authSupport,
            @Autowired(required = false) @Lazy List<CustomHtmlTag> tags) {
        this.publicIndexModel = publicIndexModel;
        this.settings = settings;
        this.siteReady = siteReady;
        this.themeCatalog = themeCatalog;
        this.httpErrorHelper = httpErrorHelper;
        this.authSupport = authSupport;
        this.tags = tags == null ? List.of() : tags;
    }

    public ResponseEntity<Map<String, Object>> get(HttpServletRequest request, HttpServletResponse response,
            String page, String album, String tag, String category, String error) throws IOException {
        AccountEntity accessor = siteReady.isConfigured() ? authSupport.getAccessor(request, response) : null;
        Model model = new ExtendedModelMap();
        PublicSiteTags.addTo(model, tags);
        model.addAttribute("navCategories", Collections.emptyList());
        model.addAttribute("sections", Collections.emptyList());
        int[] loadError = {0};
        if (siteReady.isConfigured() && (error == null || error.isBlank())) {
            publicIndexModel.addPageContentToModel(model, request, response, page, album, tag, category,
                    accessor, (code, req, res) -> loadError[0] = code);
        }
        Map<String, Object> body = PublicSiteJson.from(model, request, settings, siteReady, themeCatalog,
                accessor, error, httpErrorHelper, loadError[0]);
        int status = loadError[0] == 0 ? 200 : loadError[0];
        return ResponseEntity.status(status).body(body);
    }
}

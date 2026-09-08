package com.ravenherz.cse.controller;

import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.controller.publicsite.PublicIndexModel;
import com.ravenherz.cse.controller.publicsite.PublicSiteTags;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.util.html.impl.TagNavigation;
import com.ravenherz.cse.util.staticapps.StaticAppDeployer;
import com.ravenherz.cse.util.themes.ThemeCatalog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.Collections;

@Controller
public class IndexController extends AbstractController {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexController.class);

    private final PublicIndexModel publicIndexModel;

    public IndexController(PublicIndexModel publicIndexModel) {
        this.publicIndexModel = publicIndexModel;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String getPage(
            Model model,
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = "page", required = false) String page,
            @RequestParam(value = "album", required = false) String album,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "error", required = false) String error
    ) throws IOException {
        if (!siteReady.isConfigured() && (error == null || error.isBlank())) {
            response.sendRedirect(request.getContextPath() + "/static-pages/"
                    + StaticAppDeployer.INSTALLER_SLUG + "/");
            return null;
        }
        AccountEntity accessor = null;
        try {
            PublicSiteTags.addTo(model, tags);

            accessor = siteReady.isConfigured() ? getAccessor(request, response) : null;
            addTheme(model, accessor);

            if (jsShell(accessor)) {
                fillJsShellModel(model, request, accessor);
                return String.format("/%s/index", resolvePublicShell(accessor));
            }

            if (siteReady.isConfigured()) {

                if (accessor != null) {
                    model.addAttribute("username", accessor.getAccountData().getLogin());
                }
                model.addAttribute("authenticated", accessor != null);

                TagNavigation tagNavigation = new TagNavigation(true);
                String horizontalNavigation = tagNavigation.getHtmlElement(accessor).getCode();
                if (horizontalNavigation.length() > 0) {
                    model.addAttribute(tagNavigation.getClass().getSimpleName() + "Horizontal",
                            horizontalNavigation);
                    tagNavigation = new TagNavigation(false);
                    model.addAttribute(tagNavigation.getClass().getSimpleName() + "Vertical",
                            tagNavigation.getHtmlElement(accessor).getCode());
                    model.addAttribute("hasNavigation", true);
                } else {
                    model.addAttribute("hasNavigation", false);
                }

                publicIndexModel.addPageContentToModel(model, request, response, page, album, tag,
                        category, accessor, this::error);

            }
            if (!model.containsAttribute("htmlTitle")) {
                model.addAttribute("htmlTitle",
                        settings.getValue(SettingKeys.CONTEXT_DATASOURCE_PERSONAL,
                                SettingKeys.KEY_TAG_COMPANY_TITLE));
            }
        } catch (Exception ex) {
            LOGGER.error("Exception", ex);
            error(500, request, response);
        }

        model.addAttribute("siteName",
                settings.getValue(SettingKeys.CONTEXT_DATASOURCE_PERSONAL, SettingKeys.KEY_TAG_COMPANY_TITLE));
        if (!model.containsAttribute("sections")) {
            model.addAttribute("sections", Collections.emptyList());
        }
        if (!model.containsAttribute("navCategories")) {
            model.addAttribute("navCategories", Collections.emptyList());
        }
        if (!model.containsAttribute("portfolioHome")) {
            model.addAttribute("portfolioHome", false);
        }
        if (!model.containsAttribute("readingPage")) {
            model.addAttribute("readingPage", false);
        }
        if (!model.containsAttribute("readingAlbum")) {
            model.addAttribute("readingAlbum", false);
        }
        if (!model.containsAttribute("tagView")) {
            model.addAttribute("tagView", false);
        }
        if (!model.containsAttribute("categoryView")) {
            model.addAttribute("categoryView", false);
        }
        if (!model.containsAttribute("introPage")) {
            model.addAttribute("introPage", null);
        }
        model.addAttribute("defaultPage",
                settings.getValue(SettingKeys.CONTEXT_DATASOURCE_VIEW, SettingKeys.KEY_DEFAULT_PAGE));
        model.addAttribute("configured", siteReady.isConfigured());
        model.addAttribute("loginPanel", siteReady.isConfigured());
        model.addAttribute("isNotError", request.getParameter("error") == null);
        model.addAttribute("selectedTag", request.getParameter("tag"));
        model.addAttribute("request", request);

        return String.format("/%s/index", resolvePublicShell(accessor));
    }

    private boolean jsShell(AccountEntity accessor) {
        if (themeCatalog == null) {
            return false;
        }
        var info = themeCatalog.find(themeCatalog.resolve(accessor).getCssId());
        return info != null && ThemeCatalog.isJsShell(info.getShell());
    }

    private void fillJsShellModel(Model model, HttpServletRequest request, AccountEntity accessor) {
        if (accessor != null) {
            model.addAttribute("username", accessor.getAccountData().getLogin());
        }
        model.addAttribute("authenticated", accessor != null);
        model.addAttribute("siteName",
                settings.getValue(SettingKeys.CONTEXT_DATASOURCE_PERSONAL, SettingKeys.KEY_TAG_COMPANY_TITLE));
        model.addAttribute("htmlTitle", model.getAttribute("siteName"));
        model.addAttribute("configured", siteReady.isConfigured());
        model.addAttribute("loginPanel", siteReady.isConfigured());
        model.addAttribute("isNotError", request.getParameter("error") == null);
    }
}

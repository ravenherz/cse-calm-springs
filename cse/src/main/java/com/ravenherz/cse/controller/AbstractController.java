package com.ravenherz.cse.controller;

import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.install.SiteReady;
import com.ravenherz.cse.util.Settings;
import com.ravenherz.cse.util.html.ControllerAccessibleTag;
import com.ravenherz.cse.util.html.CustomHtmlTag;
import com.ravenherz.cse.util.io.FileUtils;
import com.ravenherz.cse.util.themes.ThemeCatalog;
import com.ravenherz.cse.util.themes.ThemeSelection;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.ui.Model;

import java.io.IOException;
import java.util.List;

public abstract class AbstractController {

    @Autowired
    @Lazy
    protected List<? extends CustomHtmlTag> tags;

    @Autowired
    @Lazy
    protected List<ControllerAccessibleTag> controllerAccessibleTags;

    protected Settings settings;
    protected FileUtils fileUtils;
    protected ServiceProvider serviceProvider;
    protected SiteReady siteReady;
    protected ThemeCatalog themeCatalog;

    private AuthSupport authSupport;

    @Autowired
    public void setSettings(Settings settingsImpl) {
        settings = settingsImpl;
    }

    @Autowired
    public void setSiteReady(SiteReady siteReadyImpl) {
        siteReady = siteReadyImpl;
    }

    @Autowired
    public void setThemeCatalog(ThemeCatalog themeCatalogImpl) {
        themeCatalog = themeCatalogImpl;
    }

    @Autowired
    public void setFileUtils(FileUtils fileUtilsImpl) {
        fileUtils = fileUtilsImpl;
    }

    @Autowired
    public void setServiceProvider(ServiceProvider serviceProviderImpl) {
        serviceProvider = serviceProviderImpl;
    }

    @Autowired
    public void setAuthSupport(AuthSupport authSupport) {
        this.authSupport = authSupport;
    }

    protected AccountEntity getAccessor(HttpServletRequest request, HttpServletResponse response) {
        return authSupport.getAccessor(request, response);
    }

    protected boolean issueSession(AccountEntity entity, HttpServletRequest request,
            HttpServletResponse response) {
        return authSupport.issueSession(entity, request, response);
    }

    protected String getCookieByKey(HttpServletRequest request, String key) {
        return authSupport.getCookieByKey(request, key);
    }

    protected void deleteAuthCookies(HttpServletRequest request, HttpServletResponse response) {
        authSupport.deleteAuthCookies(request, response);
    }

    protected void addAuthCookies(HttpServletRequest request, HttpServletResponse response,
            String login, String sessionToken) {
        authSupport.addAuthCookies(request, response, login, sessionToken);
    }

    protected String clientIp(HttpServletRequest request) {
        return authSupport.clientIp(request);
    }

    protected void error(int code, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        authSupport.redirectError(code, request, response);
    }

    protected void addEditorChrome(Model model, AccountEntity accessor) {
        model.addAttribute("username", accessor.getAccountData().getLogin());
        addTheme(model);
    }

    protected void addTheme(Model model) {
        addTheme(model, null);
    }

    protected void addTheme(Model model, AccountEntity accessor) {
        ThemeSelection selection = resolveAppearance(accessor);
        model.addAttribute("stylesTheme", selection.getCssId());
        model.addAttribute("stylesSchema", selection.getSchemaId());
        model.addAttribute("stylesShell", selection.getShellId());
    }

    protected String resolvePublicTheme() {
        return resolveAppearance(null).getCssId();
    }

    protected String resolvePublicShell() {
        return resolveAppearance(null).getShellId();
    }

    protected String resolvePublicShell(AccountEntity accessor) {
        return resolveAppearance(accessor).getShellId();
    }

    protected String resolvePublicSchema(String theme) {
        ThemeSelection selection = resolveAppearance(null);
        if (theme != null && theme.equals(selection.getCssId())) {
            return selection.getSchemaId();
        }
        if (themeCatalog != null) {
            ThemeSelection forTheme = themeCatalog.resolve();
            if (theme != null && theme.equals(forTheme.getCssId())) {
                return forTheme.getSchemaId();
            }
        }
        return "modern".equals(theme) ? "linen" : "snowy";
    }

    private ThemeSelection resolveAppearance(AccountEntity accessor) {
        if (themeCatalog != null) {
            return themeCatalog.resolve(accessor);
        }
        return ThemeSelection.fallback();
    }
}

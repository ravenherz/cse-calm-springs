package com.ravenherz.cse.controller;

import com.ravenherz.cse.dal.EntityAccess;
import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.basic.enums.AccessType;
import com.ravenherz.cse.dal.role.CapabilityIds;
import com.ravenherz.cse.install.SiteReady;
import com.ravenherz.cse.security.AccessForms;
import com.ravenherz.cse.security.CapabilityService;
import com.ravenherz.cse.engine.util.Settings;
import com.ravenherz.cse.util.html.ControllerAccessibleTag;
import com.ravenherz.cse.util.html.CustomHtmlTag;
import com.ravenherz.cse.engine.io.FileUtils;
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
    private CapabilityService capabilityService;

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

    @Autowired(required = false)
    public void setCapabilityService(CapabilityService capabilityService) {
        this.capabilityService = capabilityService;
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
        boolean editor = capabilityService == null || capabilityService.canOpenEditor(accessor);
        model.addAttribute("showNavCatalog", editor);
        model.addAttribute("showNavAccounts", allows(accessor, CapabilityIds.EDITOR_ACCOUNTS));
        model.addAttribute("showNavRoles", allows(accessor, CapabilityIds.EDITOR_ROLES));
        model.addAttribute("showNavSettings", allows(accessor, CapabilityIds.EDITOR_SETTINGS));
        model.addAttribute("showNavSiteData", allows(accessor, CapabilityIds.EDITOR_SITE_DATA));
        model.addAttribute("showNavApi", editor);
        model.addAttribute("showNavLogs", allows(accessor, CapabilityIds.EDITOR_LOGS));
        model.addAttribute("showNavTranscode", editor);
        model.addAttribute("showNavInstance", allows(accessor, CapabilityIds.EDITOR_INSTANCE));
        model.addAttribute("canEditMatrix", com.ravenherz.cse.security.AccountRoles.isOwner(accessor,
                serviceProvider == null ? null : serviceProvider.getRoleService()));
    }

    private boolean allows(AccountEntity accessor, String capabilityId) {
        return capabilityService == null || capabilityService.allows(accessor, capabilityId);
    }

    protected void addAccessLookups(Model model) {
        if (serviceProvider == null || serviceProvider.getRoleService() == null) {
            return;
        }
        List<AccountEntity> accounts = serviceProvider.getAccountService() == null
                ? List.of() : serviceProvider.getAccountService().getAllAccounts();
        AccessForms.addLookups(model, serviceProvider.getRoleService(), accounts);
    }

    protected void addAccessPanel(Model model, BasicEntity entity, AccountEntity accessor) {
        if (serviceProvider == null || serviceProvider.getRoleService() == null) {
            return;
        }
        boolean canEdit = entity == null
                || EntityAccess.isAccessible(entity, AccessType.ACCESS_EDIT, accessor);
        AccessForms.addToModel(model, entity, serviceProvider.getRoleService(),
                serviceProvider.getAccountService().getAllAccounts(), canEdit);
    }

    protected void applyAccess(HttpServletRequest request, BasicEntity entity) {
        if (serviceProvider == null || serviceProvider.getRoleService() == null) {
            return;
        }
        AccessForms.apply(request, entity, serviceProvider.getRoleService());
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

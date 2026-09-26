package com.ravenherz.cse.controller;

import com.ravenherz.cse.admin.EditorChrome;
import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.role.CapabilityIds;
import com.ravenherz.cse.security.AccountRoles;
import com.ravenherz.cse.security.CapabilityService;
import com.ravenherz.cse.util.themes.ThemeCatalog;
import com.ravenherz.cse.util.themes.ThemeSelection;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
public class EditorChromeAdapter implements EditorChrome {

    private final CapabilityService capabilityService;
    private final ServiceProvider serviceProvider;
    private final ThemeCatalog themeCatalog;

    public EditorChromeAdapter(CapabilityService capabilityService, ServiceProvider serviceProvider,
            ThemeCatalog themeCatalog) {
        this.capabilityService = capabilityService;
        this.serviceProvider = serviceProvider;
        this.themeCatalog = themeCatalog;
    }

    @Override
    public void apply(Model model, AccountEntity accessor) {
        model.addAttribute("username", accessor.getAccountData().getLogin());
        ThemeSelection selection = themeCatalog == null
                ? ThemeSelection.fallback() : themeCatalog.resolve(null);
        model.addAttribute("stylesTheme", selection.getCssId());
        model.addAttribute("stylesSchema", selection.getSchemaId());
        model.addAttribute("stylesShell", selection.getShellId());
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
        model.addAttribute("canEditMatrix", AccountRoles.isOwner(accessor,
                serviceProvider == null ? null : serviceProvider.getRoleService()));
    }

    private boolean allows(AccountEntity accessor, String capabilityId) {
        return capabilityService == null || capabilityService.allows(accessor, capabilityId);
    }
}

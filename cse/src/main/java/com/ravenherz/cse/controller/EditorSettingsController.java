package com.ravenherz.cse.controller;

import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.util.themes.ThemeInfo;
import com.ravenherz.cse.util.themes.ThemeSelection;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.Map;

@Controller
@RequestMapping("/editor")
public class EditorSettingsController extends AbstractController {

    @GetMapping("/settings")
    public String settingsPage(Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        addEditorChrome(model, accessor);
        fillAppearance(model, accessor);
        model.addAttribute("settingContexts", settings.getStorageSnapshot());
        return "/admin/editor-settings";
    }

    @PostMapping("/settings/save")
    public String saveSettings(@RequestParam Map<String, String> params,
                               Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }

        String appearanceError = appearanceError(params);
        String personalError = savePersonalTheme(accessor, params);

        java.util.Set<String> changedContexts = new java.util.LinkedHashSet<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String name = entry.getKey();
            if (name == null || !name.contains("::")) {
                continue;
            }
            int split = name.indexOf("::");
            String context = name.substring(0, split);
            String key = name.substring(split + 2);
            if (context.isBlank() || key.isBlank() || SettingKeys.isSecretContext(context)
                    || !settings.hasContext(context)) {
                continue;
            }
            if (appearanceError != null && SettingKeys.CONTEXT_DATASOURCE_VIEW.equals(context)
                    && (SettingKeys.KEY_STYLES_THEME.equals(key) || SettingKeys.KEY_STYLES_SCHEMA.equals(key))) {
                continue;
            }
            settings.putValue(context, key, entry.getValue() == null ? "" : entry.getValue());
            changedContexts.add(context);
        }

        boolean allOk = true;
        for (String context : changedContexts) {
            if (!settings.persistContext(context)) {
                allOk = false;
            }
        }

        if (appearanceError != null) {
            model.addAttribute("error", appearanceError);
        } else if (personalError != null) {
            model.addAttribute("error", personalError);
        } else if (!allOk) {
            model.addAttribute("error", "Saved in memory, but MongoDB could not persist at least one group.");
        } else {
            model.addAttribute("saved", true);
        }
        addEditorChrome(model, accessor);
        fillAppearance(model, accessor);
        model.addAttribute("settingContexts", settings.getStorageSnapshot());
        return "/admin/editor-settings";
    }

    private void fillAppearance(Model model, AccountEntity accessor) {
        if (themeCatalog == null) {
            return;
        }
        java.util.List<ThemeInfo> themes = themeCatalog.list();
        model.addAttribute("publicThemes", themes);
        ThemeSelection selection = themeCatalog.resolve();
        ThemeInfo current = themeCatalog.find(selection.getCssId());
        model.addAttribute("activeThemeSchemas",
                current == null ? java.util.List.of() : current.getSchemas());
        String personalTheme = accessor.getAccountData() == null ? "" : nullToEmpty(accessor.getAccountData().getStylesTheme());
        model.addAttribute("personalTheme", personalTheme);
        model.addAttribute("personalSchema", accessor.getAccountData() == null
                ? "" : nullToEmpty(accessor.getAccountData().getStylesSchema()));
        ThemeInfo personal = themeCatalog.find(personalTheme);
        model.addAttribute("personalThemeSchemas",
                personal == null ? java.util.List.of() : personal.getSchemas());
    }

    private String savePersonalTheme(AccountEntity accessor, Map<String, String> params) {
        if (!params.containsKey("my-styles-theme") && !params.containsKey("my-styles-schema")) {
            return null;
        }
        String theme = params.get("my-styles-theme");
        String schema = params.get("my-styles-schema");
        if (theme == null) {
            theme = "";
        }
        theme = theme.trim();
        if (schema == null) {
            schema = "";
        }
        schema = schema.trim();
        if (!theme.isEmpty() && themeCatalog != null && !themeCatalog.isKnownTheme(theme)) {
            return "Unknown personal theme: " + theme;
        }
        if (!theme.isEmpty() && !schema.isEmpty() && themeCatalog != null
                && !themeCatalog.isKnownSchema(theme, schema)) {
            return "Color schema '" + schema + "' is not part of theme '" + theme + "'";
        }
        accessor.getAccountData().setStylesTheme(theme.isEmpty() ? null : theme);
        accessor.getAccountData().setStylesSchema(schema.isEmpty() ? null : schema);
        serviceProvider.getAccountService().replace(accessor);
        return null;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String appearanceError(Map<String, String> params) {
        if (themeCatalog == null) {
            return null;
        }
        String themeName = SettingKeys.CONTEXT_DATASOURCE_VIEW + "::" + SettingKeys.KEY_STYLES_THEME;
        String schemaName = SettingKeys.CONTEXT_DATASOURCE_VIEW + "::" + SettingKeys.KEY_STYLES_SCHEMA;
        String theme = params.get(themeName);
        String schema = params.get(schemaName);
        if (theme == null && schema == null) {
            return null;
        }
        if (theme == null) {
            theme = settings.getValue(SettingKeys.CONTEXT_DATASOURCE_VIEW, SettingKeys.KEY_STYLES_THEME);
        }
        if (schema == null) {
            schema = settings.getValue(SettingKeys.CONTEXT_DATASOURCE_VIEW, SettingKeys.KEY_STYLES_SCHEMA);
        }
        if (theme != null && !theme.isBlank() && !themeCatalog.isKnownTheme(theme)) {
            return "Unknown public theme: " + theme;
        }
        if (schema != null && !schema.isBlank() && !themeCatalog.isKnownSchema(theme, schema)) {
            return "Color schema '" + schema + "' is not part of theme '" + theme + "'";
        }
        return null;
    }
}

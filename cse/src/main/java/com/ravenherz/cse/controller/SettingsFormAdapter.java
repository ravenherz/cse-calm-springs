package com.ravenherz.cse.controller;

import com.ravenherz.cse.admin.SettingsForm;
import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.engine.util.Settings;
import com.ravenherz.cse.util.themes.ThemeCatalog;
import com.ravenherz.cse.util.themes.ThemeInfo;
import com.ravenherz.cse.util.themes.ThemeSelection;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class SettingsFormAdapter implements SettingsForm {

    private final Settings settings;
    private final ThemeCatalog themeCatalog;

    public SettingsFormAdapter(Settings settings, ThemeCatalog themeCatalog) {
        this.settings = settings;
        this.themeCatalog = themeCatalog;
    }

    @Override
    public void fill(Model model) {
        fillAppearance(model);
        model.addAttribute("settingContexts", settings.getStorageSnapshot());
    }

    @Override
    public void save(Map<String, String> params, Model model) {
        String appearanceError = appearanceError(params);
        Set<String> changedContexts = new LinkedHashSet<>();
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
        } else if (!allOk) {
            model.addAttribute("error", "Saved in memory, but MongoDB could not persist at least one group.");
        } else {
            model.addAttribute("saved", true);
        }
    }

    private void fillAppearance(Model model) {
        if (themeCatalog == null) {
            return;
        }
        List<ThemeInfo> themes = themeCatalog.list();
        model.addAttribute("publicThemes", themes);
        ThemeSelection selection = themeCatalog.resolve();
        ThemeInfo current = themeCatalog.find(selection.getCssId());
        model.addAttribute("activeThemeSchemas",
                current == null ? List.of() : current.getSchemas());
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

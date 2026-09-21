package com.ravenherz.cse.util.html;

import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.engine.util.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;

@DependsOn(value = "settings")
public abstract class AccessToSettingsTag {

    protected static Settings settings;
    protected static final String context = SettingKeys.CONTEXT_DATASOURCE_PERSONAL;

    @Autowired
    public void setSettings(Settings settingsImpl) {
        settings = settingsImpl;
    }

    protected String defaultValueIfNull(String value, String key) {
        return value == null ? settings.getValue(context, key) : value;
    }
}

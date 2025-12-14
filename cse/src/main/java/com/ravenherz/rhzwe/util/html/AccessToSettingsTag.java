package com.ravenherz.rhzwe.util.html;

import com.ravenherz.rhzwe.constants.Strings;
import com.ravenherz.rhzwe.util.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;

@DependsOn(value = "settings")
public abstract class AccessToSettingsTag {

    protected static Settings settings;
    protected static final String context = Strings.CONTEXT_DATASOURCE_PERSONAL;

    @Autowired
    public void setSettings(Settings settingsImpl) {
        settings = settingsImpl;
    }

    protected String defaultValueIfNull(String value, String key) {
        return value == null ? settings.getValue(context, key) : value;
    }
}

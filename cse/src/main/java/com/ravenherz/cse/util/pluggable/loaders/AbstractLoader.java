package com.ravenherz.cse.util.pluggable.loaders;

import com.ravenherz.cse.util.Settings;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractLoader {

    private Settings settings;

    @Autowired
    public void setSettings(Settings settingsImpl) {
        settings = settingsImpl;
    }

    public Settings getSettings() {
        return settings;
    }
}

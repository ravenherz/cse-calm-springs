package com.ravenherz.rhzwe.util.pluggable.loaders;

import com.ravenherz.rhzwe.util.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

public abstract class AbstractLoader {

    private static Settings settings;

    @Lazy
    @Autowired
    public void setSettings (Settings settingsImpl) {
        settings = settingsImpl;
    }

    public Settings getSettings () {
        return settings;
    }
}

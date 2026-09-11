package com.ravenherz.cse.dal;

import com.ravenherz.cse.constants.SettingKeys;

public final class EntityVersions {

    public static final String FALLBACK = "0.1.0";

    private static volatile ConfigSource config;

    private EntityVersions() {
    }

    public static void bind(ConfigSource source) {
        config = source;
    }

    public static String current() {
        ConfigSource source = config;
        if (source == null) {
            return FALLBACK;
        }
        try {
            String version = source.getValue(SettingKeys.CONTEXT_DATASOURCE_BUILD_INFO,
                    SettingKeys.KEY_TAG_SOFT_VERSION_VERSION);
            if (version == null || version.isBlank()) {
                return FALLBACK;
            }
            return version.trim();
        } catch (RuntimeException ex) {
            return FALLBACK;
        }
    }
}

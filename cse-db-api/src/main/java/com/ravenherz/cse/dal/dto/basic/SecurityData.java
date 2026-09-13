package com.ravenherz.cse.dal.dto.basic;

import com.ravenherz.cse.dal.dto.basic.enums.AccessType;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class SecurityData implements Serializable {

    private Map<AccessType, AccessRule> accessSettings;

    public SecurityData() {
        this.accessSettings = defaultSettings();
    }

    public SecurityData(Map<AccessType, AccessRule> accessSettings) {
        if (accessSettings != null) {
            this.accessSettings = new EnumMap<>(AccessType.class);
            this.accessSettings.putAll(accessSettings);
        } else {
            this.accessSettings = defaultSettings();
        }
    }

    public static Map<AccessType, AccessRule> defaultSettings() {
        Map<AccessType, AccessRule> settings = new EnumMap<>(AccessType.class);
        settings.put(AccessType.ACCESS_READ, AccessRule.inheritAll());
        settings.put(AccessType.ACCESS_EDIT, AccessRule.inheritAll());
        settings.put(AccessType.ACCESS_DELETE, AccessRule.inheritAll());
        return settings;
    }

    public AccessRule rule(AccessType accessType) {
        if (accessType == null || accessSettings == null) {
            return AccessRule.inheritAll();
        }
        AccessRule rule = accessSettings.get(accessType);
        return rule == null ? AccessRule.inheritAll() : rule;
    }

    public AccessRule getRead() {
        return rule(AccessType.ACCESS_READ);
    }

    public AccessRule getEdit() {
        return rule(AccessType.ACCESS_EDIT);
    }

    public AccessRule getDelete() {
        return rule(AccessType.ACCESS_DELETE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SecurityData that = (SecurityData) o;
        return Objects.equals(accessSettings, that.accessSettings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessSettings);
    }

    public Map<AccessType, AccessRule> getAccessSettings() {
        return accessSettings;
    }

    public void setAccessSettings(Map<AccessType, AccessRule> accessSettings) {
        this.accessSettings = accessSettings;
    }
}

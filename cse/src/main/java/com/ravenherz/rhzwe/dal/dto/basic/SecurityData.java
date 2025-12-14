package com.ravenherz.rhzwe.dal.dto.basic;

import com.ravenherz.rhzwe.dal.dto.basic.enums.AccessType;
import com.ravenherz.rhzwe.dal.dto.basic.enums.SecurityLevel;
import dev.morphia.annotations.Entity;

import java.beans.Transient;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

@Entity
public final class SecurityData implements Serializable {

    private Map<AccessType, SecurityLevel> accessSettings;

    public SecurityData() {
        this.accessSettings = getDefaultAccessSettings();
    }

    public SecurityData(Map<AccessType, SecurityLevel> accessSettings) {
        if (accessSettings != null) {
            this.accessSettings = accessSettings;
        } else {
            new SecurityData();
        }
    }

    @Transient
    private Map<AccessType, SecurityLevel> getDefaultAccessSettings() {
        return EntityAccessConstants.DEFAULT;
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

    public Map<AccessType, SecurityLevel> getAccessSettings() {
        return accessSettings;
    }

    public void setAccessSettings(Map<AccessType, SecurityLevel> accessSettings) {
        this.accessSettings = accessSettings;
    }
}

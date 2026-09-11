package com.ravenherz.cse.dal.dto.basic;

import java.util.Objects;

public final class RoleGrant {

    private String capabilityId;
    private String roleId;

    public RoleGrant() {
    }

    public RoleGrant(String capabilityId, String roleId) {
        this.capabilityId = capabilityId;
        this.roleId = roleId;
    }

    public String getCapabilityId() {
        return capabilityId;
    }

    public void setCapabilityId(String capabilityId) {
        this.capabilityId = capabilityId;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoleGrant that)) {
            return false;
        }
        return Objects.equals(capabilityId, that.capabilityId) && Objects.equals(roleId, that.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(capabilityId, roleId);
    }
}

package com.ravenherz.cse.dal.dto.basic;

import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import com.ravenherz.cse.dal.role.RoleSeeds;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AccessRule {

    private boolean inherit = true;
    private List<String> roleIds = new ArrayList<>();
    private List<String> accountIds = new ArrayList<>();
    private String legacyThreshold;

    public AccessRule() {
    }

    public AccessRule(boolean inherit, List<String> roleIds, List<String> accountIds) {
        this.inherit = inherit;
        this.roleIds = roleIds == null ? new ArrayList<>() : new ArrayList<>(roleIds);
        this.accountIds = accountIds == null ? new ArrayList<>() : new ArrayList<>(accountIds);
    }

    public static AccessRule inheritAll() {
        return new AccessRule(true, List.of(), List.of());
    }

    public static AccessRule ofRoles(List<String> roleIds) {
        return new AccessRule(false, roleIds, List.of());
    }

    public static AccessRule fromLegacy(SecurityLevel threshold) {
        AccessRule rule = new AccessRule();
        rule.inherit = false;
        rule.legacyThreshold = threshold == null ? null : threshold.name();
        rule.roleIds = new ArrayList<>(RoleSeeds.slugsAtOrAbove(threshold));
        rule.accountIds = new ArrayList<>();
        return rule;
    }

    public boolean isInherit() {
        return inherit;
    }

    public void setInherit(boolean inherit) {
        this.inherit = inherit;
    }

    public List<String> getRoleIds() {
        if (roleIds == null) {
            roleIds = new ArrayList<>();
        }
        return roleIds;
    }

    public void setRoleIds(List<String> roleIds) {
        this.roleIds = roleIds == null ? new ArrayList<>() : new ArrayList<>(roleIds);
    }

    public List<String> getAccountIds() {
        if (accountIds == null) {
            accountIds = new ArrayList<>();
        }
        return accountIds;
    }

    public void setAccountIds(List<String> accountIds) {
        this.accountIds = accountIds == null ? new ArrayList<>() : new ArrayList<>(accountIds);
    }

    public String getLegacyThreshold() {
        return legacyThreshold;
    }

    public void setLegacyThreshold(String legacyThreshold) {
        this.legacyThreshold = legacyThreshold;
    }

    public boolean hasAccount(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return false;
        }
        return getAccountIds().contains(accountId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AccessRule that)) {
            return false;
        }
        return inherit == that.inherit
                && Objects.equals(getRoleIds(), that.getRoleIds())
                && Objects.equals(getAccountIds(), that.getAccountIds())
                && Objects.equals(legacyThreshold, that.legacyThreshold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inherit, getRoleIds(), getAccountIds(), legacyThreshold);
    }
}

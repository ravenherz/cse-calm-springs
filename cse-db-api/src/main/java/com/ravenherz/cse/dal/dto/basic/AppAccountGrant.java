package com.ravenherz.cse.dal.dto.basic;

import java.util.Objects;

public final class AppAccountGrant {

    private String capabilityId;
    private String accountId;

    public AppAccountGrant() {
    }

    public AppAccountGrant(String capabilityId, String accountId) {
        this.capabilityId = capabilityId;
        this.accountId = accountId;
    }

    public String getCapabilityId() {
        return capabilityId;
    }

    public void setCapabilityId(String capabilityId) {
        this.capabilityId = capabilityId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AppAccountGrant that)) {
            return false;
        }
        return Objects.equals(capabilityId, that.capabilityId) && Objects.equals(accountId, that.accountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(capabilityId, accountId);
    }
}

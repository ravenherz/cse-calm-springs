package com.ravenherz.cse.dal.dao;

import com.ravenherz.cse.dal.dto.RoleMatrixDocument;
import com.ravenherz.cse.dal.dto.basic.AppAccountGrant;
import com.ravenherz.cse.dal.dto.basic.RoleGrant;

import java.util.List;

public interface RoleMatrixService {

    RoleMatrixDocument get();

    void replaceGrants(List<RoleGrant> grants);

    void replaceAppGrants(List<AppAccountGrant> appGrants);

    boolean allows(String roleId, String capabilityId);

    boolean allowsAppAccount(String accountId, String capabilityId);

    void ensureSeeded(RoleService roles);

    void invalidateCache();
}

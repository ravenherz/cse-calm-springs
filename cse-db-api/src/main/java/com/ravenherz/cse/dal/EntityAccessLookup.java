package com.ravenherz.cse.dal;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.basic.enums.AccessType;

import java.util.Set;

public interface EntityAccessLookup {

    boolean isSiteOwner(AccountEntity account);

    String roleId(AccountEntity account);

    String guestRoleId();

    Set<String> defaultRoleIds(AccessType accessType);

    BasicEntity parentOf(BasicEntity entity);

    Set<String> expandRoleIds(Iterable<String> storedIds);
}

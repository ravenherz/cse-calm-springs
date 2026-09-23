package com.ravenherz.cse.dal;

import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;

/**
 * Who is asking for access. Feature account types implement this.
 */
public interface AccessActor {

    EntityId id();

    default String roleId() {
        return null;
    }

    default SecurityLevel level() {
        return null;
    }
}

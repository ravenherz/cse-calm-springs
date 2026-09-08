package com.ravenherz.cse.dal.dto.basic;

import com.ravenherz.cse.dal.dto.basic.enums.AccessType;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;

import java.util.HashMap;

public class EntityAccessConstants {

    public static final HashMap<AccessType, SecurityLevel> DEFAULT = new HashMap<AccessType, SecurityLevel>() {
        {
            put(AccessType.ACCESS_READ, SecurityLevel.GUEST);
            put(AccessType.ACCESS_EDIT, SecurityLevel.OWNER);
            put(AccessType.ACCESS_DELETE, SecurityLevel.MODERATOR);
        }
    };
    public static final HashMap<AccessType, SecurityLevel> GUIDE = new HashMap<AccessType, SecurityLevel>() {
        {
            put(AccessType.ACCESS_READ, SecurityLevel.GUIDE);
            put(AccessType.ACCESS_EDIT, SecurityLevel.GUIDE);
            put(AccessType.ACCESS_DELETE, SecurityLevel.GUIDE);
        }
    };
}

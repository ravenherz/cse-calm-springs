package com.ravenherz.rhzwe.dal.dto.basic;

import com.ravenherz.rhzwe.dal.dto.basic.enums.AccessType;
import com.ravenherz.rhzwe.dal.dto.basic.enums.SecurityLevel;
import dev.morphia.annotations.Entity;

import java.util.HashMap;

@Entity
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

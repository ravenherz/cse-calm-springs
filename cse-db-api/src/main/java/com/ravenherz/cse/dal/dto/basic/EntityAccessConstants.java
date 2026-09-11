package com.ravenherz.cse.dal.dto.basic;

import com.ravenherz.cse.dal.dto.basic.enums.AccessType;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import com.ravenherz.cse.dal.role.RoleSeeds;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class EntityAccessConstants {

    public static final Map<AccessType, AccessRule> DEFAULT = SecurityData.defaultSettings();

    public static final Map<AccessType, AccessRule> GUIDE = guideSettings();

    private EntityAccessConstants() {
    }

    private static Map<AccessType, AccessRule> guideSettings() {
        Map<AccessType, AccessRule> settings = new EnumMap<>(AccessType.class);
        AccessRule guide = AccessRule.fromLegacy(SecurityLevel.GUIDE);
        settings.put(AccessType.ACCESS_READ, guide);
        settings.put(AccessType.ACCESS_EDIT, AccessRule.fromLegacy(SecurityLevel.GUIDE));
        settings.put(AccessType.ACCESS_DELETE, AccessRule.fromLegacy(SecurityLevel.GUIDE));
        return settings;
    }

    /** @deprecated kept for callers that still pass rank maps during dual-write tests */
    @Deprecated
    public static Map<AccessType, SecurityLevel> legacyDefaultRanks() {
        HashMap<AccessType, SecurityLevel> ranks = new HashMap<>();
        ranks.put(AccessType.ACCESS_READ, SecurityLevel.GUEST);
        ranks.put(AccessType.ACCESS_EDIT, SecurityLevel.OWNER);
        ranks.put(AccessType.ACCESS_DELETE, SecurityLevel.MODERATOR);
        return ranks;
    }

    public static AccessRule fromLegacyThreshold(SecurityLevel threshold) {
        if (threshold == null) {
            return AccessRule.inheritAll();
        }
        AccessRule rule = AccessRule.fromLegacy(threshold);
        if (rule.getRoleIds().isEmpty() && threshold != SecurityLevel.OWNER) {
            rule.setRoleIds(RoleSeeds.slugsAtOrAbove(threshold));
        }
        return rule;
    }
}

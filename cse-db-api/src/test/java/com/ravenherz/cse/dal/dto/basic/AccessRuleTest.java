package com.ravenherz.cse.dal.dto.basic;

import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import com.ravenherz.cse.dal.role.RoleSeeds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessRuleTest {

    @Test
    void guestThresholdListsEverySeedExceptOwner() {
        AccessRule rule = AccessRule.fromLegacy(SecurityLevel.GUEST);
        assertFalse(rule.isInherit());
        assertTrue(rule.getRoleIds().contains(RoleSeeds.GUEST));
        assertTrue(rule.getRoleIds().contains(RoleSeeds.ADMIN));
        assertFalse(rule.getRoleIds().contains(RoleSeeds.OWNER));
    }

    @Test
    void ownerThresholdIsImplicitOnly() {
        AccessRule rule = AccessRule.fromLegacy(SecurityLevel.OWNER);
        assertFalse(rule.isInherit());
        assertTrue(rule.getRoleIds().isEmpty());
    }

    @Test
    void guideThresholdStartsAtGuide() {
        AccessRule rule = AccessRule.fromLegacy(SecurityLevel.GUIDE);
        assertEquals(RoleSeeds.slugsAtOrAbove(SecurityLevel.GUIDE), rule.getRoleIds());
        assertFalse(rule.getRoleIds().contains(RoleSeeds.MEMBER));
        assertTrue(rule.getRoleIds().contains(RoleSeeds.GUIDE));
    }
}

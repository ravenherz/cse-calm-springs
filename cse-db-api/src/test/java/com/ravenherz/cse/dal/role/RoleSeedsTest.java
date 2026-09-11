package com.ravenherz.cse.dal.role;

import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleSeedsTest {

    @Test
    void slugMapsEveryLegacyLevel() {
        assertEquals(RoleSeeds.GUEST, RoleSeeds.slugFor(SecurityLevel.GUEST));
        assertEquals(RoleSeeds.INACTIVE, RoleSeeds.slugFor(SecurityLevel.INACTIVE_USER));
        assertEquals(RoleSeeds.MEMBER, RoleSeeds.slugFor(SecurityLevel.ACTIVE_USER));
        assertEquals(RoleSeeds.PRIVILEGED, RoleSeeds.slugFor(SecurityLevel.PRIVILEGIED_USER));
        assertEquals(RoleSeeds.GUIDE, RoleSeeds.slugFor(SecurityLevel.GUIDE));
        assertEquals(RoleSeeds.OPERATOR, RoleSeeds.slugFor(SecurityLevel.OPERATOR));
        assertEquals(RoleSeeds.MODERATOR, RoleSeeds.slugFor(SecurityLevel.MODERATOR));
        assertEquals(RoleSeeds.ADMIN, RoleSeeds.slugFor(SecurityLevel.ADMIN));
        assertEquals(RoleSeeds.OWNER, RoleSeeds.slugFor(SecurityLevel.OWNER));
    }

    @Test
    void slugsAtOrAboveOmitOwner() {
        List<String> guest = RoleSeeds.slugsAtOrAbove(SecurityLevel.GUEST);
        assertTrue(guest.contains(RoleSeeds.GUEST));
        assertTrue(guest.contains(RoleSeeds.ADMIN));
        assertFalse(guest.contains(RoleSeeds.OWNER));
        assertEquals(List.of(RoleSeeds.ADMIN), RoleSeeds.slugsAtOrAbove(SecurityLevel.ADMIN));
        assertEquals(List.of(), RoleSeeds.slugsAtOrAbove(SecurityLevel.OWNER));
        assertEquals(List.of(RoleSeeds.GUIDE, RoleSeeds.OPERATOR, RoleSeeds.MODERATOR, RoleSeeds.ADMIN),
                RoleSeeds.slugsAtOrAbove(SecurityLevel.GUIDE));
    }

    @Test
    void loginableMatchesSeedFlags() {
        assertFalse(RoleSeeds.loginableFor(SecurityLevel.GUEST));
        assertFalse(RoleSeeds.loginableFor(SecurityLevel.INACTIVE_USER));
        assertFalse(RoleSeeds.loginableFor(SecurityLevel.GUIDE));
        assertTrue(RoleSeeds.loginableFor(SecurityLevel.ACTIVE_USER));
        assertTrue(RoleSeeds.loginableFor(SecurityLevel.ADMIN));
        assertTrue(RoleSeeds.loginableFor(SecurityLevel.OWNER));
    }
}

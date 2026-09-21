package com.ravenherz.cse.security;

import com.ravenherz.cse.dal.dao.RoleService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.RoleEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import com.ravenherz.cse.dal.role.RoleSeeds;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AccountRolePolicyTest {

    @Test
    void guestIsNotAssignable() {
        AccountRolePolicy.Decision decision = AccountRolePolicy.evaluate(
                true, true, false, role(RoleSeeds.MEMBER, false, true),
                role(RoleSeeds.GUEST, true, false), false, 1);
        assertFalse(decision.allowed());
        assertEquals("guest-not-assignable", decision.error());
    }

    @Test
    void ownerRowCannotBeDemoted() {
        RoleEntity owner = role(RoleSeeds.OWNER, true, true);
        AccountRolePolicy.Decision decision = AccountRolePolicy.evaluate(
                true, true, true, owner, role(RoleSeeds.ADMIN, false, true), true, 1);
        assertFalse(decision.allowed());
        assertEquals("sole-owner", decision.error());
        assertFalse(AccountRolePolicy.canEdit(true, owner));
    }

    @Test
    void ownerTransfersByGrantingOwnerToSomeoneElse() {
        AccountRolePolicy.Decision decision = AccountRolePolicy.evaluate(
                true, true, false, role(RoleSeeds.ADMIN, false, true),
                role(RoleSeeds.OWNER, true, true), true, 1);
        assertTrue(decision.allowed());
        assertTrue(decision.transferOwnership());
        assertTrue(decision.loginable());
    }

    @Test
    void adminCannotGrantOwnerWhenOneExists() {
        AccountRolePolicy.Decision decision = AccountRolePolicy.evaluate(
                false, true, false, role(RoleSeeds.MEMBER, false, true),
                role(RoleSeeds.OWNER, true, true), true, 1);
        assertFalse(decision.allowed());
        assertEquals("owner-required", decision.error());
    }

    @Test
    void adminCanHealMissingOwner() {
        AccountRolePolicy.Decision decision = AccountRolePolicy.evaluate(
                false, true, false, role(RoleSeeds.ADMIN, false, true),
                role(RoleSeeds.OWNER, true, true), true, 0);
        assertTrue(decision.allowed());
        assertFalse(decision.transferOwnership());
    }

    @Test
    void cannotLockYourselfOutOfEditor() {
        AccountRolePolicy.Decision decision = AccountRolePolicy.evaluate(
                false, true, true, role(RoleSeeds.ADMIN, false, true),
                role(RoleSeeds.MODERATOR, false, true), false, 1);
        assertFalse(decision.allowed());
        assertEquals("self-lockout", decision.error());
    }

    @Test
    void adminCanPromoteMember() {
        AccountRolePolicy.Decision decision = AccountRolePolicy.evaluate(
                false, true, false, role(RoleSeeds.MEMBER, false, true),
                role(RoleSeeds.MODERATOR, false, true), false, 1);
        assertTrue(decision.allowed());
        assertFalse(decision.transferOwnership());
        assertTrue(decision.loginable());
    }

    @Test
    void ownerCountIgnoresNulls() {
        RoleService roles = mock(RoleService.class);
        AccountEntity owner = account(SecurityLevel.OWNER);
        AccountEntity member = account(SecurityLevel.ACTIVE_USER);
        assertEquals(1, AccountRolePolicy.ownerCount(List.of(owner, member, new AccountEntity()), roles));
        assertEquals(0, AccountRolePolicy.ownersExcept(List.of(owner, member), owner.getId(), roles).size());
        assertEquals(1, AccountRolePolicy.ownersExcept(List.of(owner, member), member.getId(), roles).size());
    }

    private static RoleEntity role(String slug, boolean system, boolean loginable) {
        RoleEntity entity = new RoleEntity();
        entity.setId(new ObjectId());
        entity.setSlug(slug);
        entity.setName(slug);
        entity.setSystem(system ? slug : null);
        entity.setLoginable(loginable);
        return entity;
    }

    private static AccountEntity account(SecurityLevel level) {
        AccountEntity entity = new AccountEntity(new AccountData("user", "hash", "u@example.com", level));
        entity.setId(new ObjectId());
        return entity;
    }
}

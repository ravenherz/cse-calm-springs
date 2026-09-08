package com.ravenherz.cse.security;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountRolePolicyTest {

    @Test
    void guestIsNotAssignable() {
        AccountRolePolicy.Decision decision = AccountRolePolicy.evaluate(
                SecurityLevel.OWNER, false, SecurityLevel.ACTIVE_USER, SecurityLevel.GUEST, 1);
        assertFalse(decision.allowed());
        assertEquals("guest-not-assignable", decision.error());
    }

    @Test
    void ownerRowCannotBeDemoted() {
        AccountRolePolicy.Decision decision = AccountRolePolicy.evaluate(
                SecurityLevel.OWNER, true, SecurityLevel.OWNER, SecurityLevel.ADMIN, 1);
        assertFalse(decision.allowed());
        assertEquals("sole-owner", decision.error());
        assertFalse(AccountRolePolicy.canEdit(SecurityLevel.OWNER, SecurityLevel.OWNER));
    }

    @Test
    void ownerTransfersByGrantingOwnerToSomeoneElse() {
        AccountRolePolicy.Decision decision = AccountRolePolicy.evaluate(
                SecurityLevel.OWNER, false, SecurityLevel.ADMIN, SecurityLevel.OWNER, 1);
        assertTrue(decision.allowed());
        assertTrue(decision.transferOwnership());
        assertTrue(decision.loginable());
    }

    @Test
    void adminCannotGrantOwnerWhenOneExists() {
        AccountRolePolicy.Decision decision = AccountRolePolicy.evaluate(
                SecurityLevel.ADMIN, false, SecurityLevel.ACTIVE_USER, SecurityLevel.OWNER, 1);
        assertFalse(decision.allowed());
        assertEquals("owner-required", decision.error());
    }

    @Test
    void adminCanHealMissingOwner() {
        AccountRolePolicy.Decision decision = AccountRolePolicy.evaluate(
                SecurityLevel.ADMIN, false, SecurityLevel.ADMIN, SecurityLevel.OWNER, 0);
        assertTrue(decision.allowed());
        assertFalse(decision.transferOwnership());
    }

    @Test
    void cannotLockYourselfOutOfEditor() {
        AccountRolePolicy.Decision decision = AccountRolePolicy.evaluate(
                SecurityLevel.ADMIN, true, SecurityLevel.ADMIN, SecurityLevel.MODERATOR, 1);
        assertFalse(decision.allowed());
        assertEquals("self-lockout", decision.error());
    }

    @Test
    void adminCanPromoteMember() {
        AccountRolePolicy.Decision decision = AccountRolePolicy.evaluate(
                SecurityLevel.ADMIN, false, SecurityLevel.ACTIVE_USER, SecurityLevel.MODERATOR, 1);
        assertTrue(decision.allowed());
        assertFalse(decision.transferOwnership());
        assertTrue(decision.loginable());
    }

    @Test
    void guideAndInactiveAreNotLoginable() {
        assertFalse(AccountRolePolicy.loginableFor(SecurityLevel.GUIDE));
        assertFalse(AccountRolePolicy.loginableFor(SecurityLevel.INACTIVE_USER));
        assertTrue(AccountRolePolicy.loginableFor(SecurityLevel.ADMIN));
    }

    @Test
    void ownerDropdownIsLocked() {
        List<SecurityLevel> levels = AccountRolePolicy.assignableLevels(
                SecurityLevel.OWNER, SecurityLevel.OWNER, true, 1);
        assertTrue(levels.isEmpty());
        assertFalse(AccountRolePolicy.canEdit(SecurityLevel.OWNER, SecurityLevel.OWNER));
        assertTrue(AccountRolePolicy.canTransfer(SecurityLevel.OWNER));
        assertFalse(AccountRolePolicy.canTransfer(SecurityLevel.ADMIN));
    }

    @Test
    void adminSelfCanOnlyStayAdmin() {
        List<SecurityLevel> levels = AccountRolePolicy.assignableLevels(
                SecurityLevel.ADMIN, SecurityLevel.ADMIN, true, 1);
        assertEquals(List.of(SecurityLevel.ADMIN), levels);
    }

    @Test
    void roleDropdownNeverIncludesOwner() {
        assertFalse(AccountRolePolicy.assignableLevels(
                SecurityLevel.OWNER, SecurityLevel.ACTIVE_USER, false, 1).contains(SecurityLevel.OWNER));
        assertFalse(AccountRolePolicy.assignableLevels(
                SecurityLevel.ADMIN, SecurityLevel.ACTIVE_USER, false, 1).contains(SecurityLevel.OWNER));
        assertTrue(AccountRolePolicy.canEdit(SecurityLevel.OWNER, SecurityLevel.ACTIVE_USER));
    }

    @Test
    void ownerCountIgnoresNulls() {
        AccountEntity owner = account(SecurityLevel.OWNER);
        AccountEntity member = account(SecurityLevel.ACTIVE_USER);
        assertEquals(1, AccountRolePolicy.ownerCount(List.of(owner, member, new AccountEntity())));
        assertEquals(0, AccountRolePolicy.ownersExcept(List.of(owner, member), owner.getId()).size());
        assertEquals(1, AccountRolePolicy.ownersExcept(List.of(owner, member), member.getId()).size());
    }

    private static AccountEntity account(SecurityLevel level) {
        AccountEntity entity = new AccountEntity(new AccountData("user", "hash", "u@example.com", level));
        entity.setId(new ObjectId());
        return entity;
    }
}

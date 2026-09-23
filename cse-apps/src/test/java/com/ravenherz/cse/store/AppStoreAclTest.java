package com.ravenherz.cse.store;

import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppStoreAclTest {

    @Test
    void ownerModeKeepsGuestsOutAndMembersOnTheirRows() {
        AccountEntity member = account(SecurityLevel.ACTIVE_USER, "68b000000000000000000001");
        AccountEntity other = account(SecurityLevel.ACTIVE_USER, "68b000000000000000000002");
        assertFalse(AppStoreAcl.canReadList(AppStoreAccess.OWNER, null));
        assertTrue(AppStoreAcl.canReadList(AppStoreAccess.OWNER, member));
        assertTrue(AppStoreAcl.canReadRow(AppStoreAccess.OWNER, member, "68b000000000000000000001"));
        assertFalse(AppStoreAcl.canReadRow(AppStoreAccess.OWNER, member, "68b000000000000000000002"));
        assertFalse(AppStoreAcl.canWriteRow(AppStoreAccess.OWNER, member, "68b000000000000000000002"));
        assertTrue(AppStoreAcl.canWriteRow(AppStoreAccess.OWNER, member, "68b000000000000000000001"));
        assertFalse(AppStoreAcl.canCreate(AppStoreAccess.OWNER, null));
        assertTrue(AppStoreAcl.canCreate(AppStoreAccess.OWNER, member));
        assertFalse(AppStoreAcl.canWriteRow(AppStoreAccess.OWNER, other, "68b000000000000000000001"));
    }

    @Test
    void publicReadLetsGuestsReadButNotWrite() {
        AccountEntity member = account(SecurityLevel.ACTIVE_USER, "68b000000000000000000001");
        assertTrue(AppStoreAcl.canReadList(AppStoreAccess.PUBLIC_READ, null));
        assertTrue(AppStoreAcl.canReadRow(AppStoreAccess.PUBLIC_READ, null, "68b000000000000000000001"));
        assertFalse(AppStoreAcl.canCreate(AppStoreAccess.PUBLIC_READ, null));
        assertTrue(AppStoreAcl.canCreate(AppStoreAccess.PUBLIC_READ, member));
        assertTrue(AppStoreAcl.canWriteRow(AppStoreAccess.PUBLIC_READ, member, "68b000000000000000000001"));
        assertFalse(AppStoreAcl.canWriteRow(AppStoreAccess.PUBLIC_READ, member, "68b000000000000000000002"));
    }

    @Test
    void memberWriteRequiresALoginableAccount() {
        AccountEntity member = account(SecurityLevel.ACTIVE_USER, "68b000000000000000000001");
        AccountEntity inactive = account(SecurityLevel.INACTIVE_USER, "68b000000000000000000003");
        assertFalse(AppStoreAcl.canReadList(AppStoreAccess.MEMBER_WRITE, null));
        assertTrue(AppStoreAcl.canReadList(AppStoreAccess.MEMBER_WRITE, member));
        assertFalse(AppStoreAcl.isMember(inactive));
        assertTrue(AppStoreAcl.canWriteRow(AppStoreAccess.MEMBER_WRITE, member, "68b000000000000000000099"));
        assertFalse(AppStoreAcl.canCreate(AppStoreAccess.MEMBER_WRITE, inactive));
    }

    @Test
    void adminModeIsSiteAdminOnlyIncludingOwner() {
        AccountEntity admin = account(SecurityLevel.ADMIN, "68b000000000000000000010");
        AccountEntity owner = account(SecurityLevel.OWNER, "68b000000000000000000011");
        AccountEntity member = account(SecurityLevel.ACTIVE_USER, "68b000000000000000000001");
        assertTrue(AppStoreAcl.isSiteAdmin(admin));
        assertTrue(AppStoreAcl.isSiteAdmin(owner));
        assertFalse(AppStoreAcl.canReadList(AppStoreAccess.ADMIN, member));
        assertTrue(AppStoreAcl.canReadList(AppStoreAccess.ADMIN, admin));
        assertTrue(AppStoreAcl.canCreate(AppStoreAccess.ADMIN, owner));
        assertFalse(AppStoreAcl.canCreate(AppStoreAccess.ADMIN, member));
        assertTrue(AppStoreAcl.canDefineSchema(admin, false));
        assertFalse(AppStoreAcl.canDefineSchema(member, false));
        assertTrue(AppStoreAcl.canDefineSchema(member, true));
    }

    private static AccountEntity account(SecurityLevel level, String id) {
        AccountEntity account = new AccountEntity(new AccountData("ada", "hash", "ada@example.com", level));
        account.setId(EntityId.of(id));
        return account;
    }
}

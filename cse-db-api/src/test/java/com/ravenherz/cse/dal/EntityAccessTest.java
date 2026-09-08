package com.ravenherz.cse.dal;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.dto.basic.EntityAccessConstants;
import com.ravenherz.cse.dal.dto.basic.HistoryData;
import com.ravenherz.cse.dal.dto.basic.PageData;
import com.ravenherz.cse.dal.dto.basic.SecurityData;
import com.ravenherz.cse.dal.dto.basic.enums.AccessType;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityAccessTest {

    @Test
    void nullsAreDenied() {
        ItemEntity page = ownedPage(account(SecurityLevel.ADMIN));
        assertFalse(EntityAccess.isAccessible(null, AccessType.ACCESS_READ, account(SecurityLevel.ADMIN)));
        assertFalse(EntityAccess.isAccessible(page, null, account(SecurityLevel.ADMIN)));
    }

    @Test
    void sameIdIsAlwaysAllowed() {
        AccountEntity self = account(SecurityLevel.INACTIVE_USER);
        assertTrue(EntityAccess.isAccessible(self, AccessType.ACCESS_DELETE, self));
    }

    @Test
    void missingOwnerAllowsReadOnly() {
        ItemEntity orphan = new ItemEntity("orphan", new PageData(), null);
        orphan.setId(new ObjectId());
        orphan.setHistoryData(new HistoryData());
        assertTrue(EntityAccess.isAccessible(orphan, AccessType.ACCESS_READ, null));
        assertFalse(EntityAccess.isAccessible(orphan, AccessType.ACCESS_EDIT, null));
        assertFalse(EntityAccess.isAccessible(orphan, AccessType.ACCESS_DELETE, null));
    }

    @Test
    void guestCanReadDefaultAndCannotEdit() {
        ItemEntity page = ownedPage(account(SecurityLevel.ADMIN));
        assertTrue(EntityAccess.isAccessible(page, AccessType.ACCESS_READ, null));
        assertFalse(EntityAccess.isAccessible(page, AccessType.ACCESS_EDIT, null));
        assertFalse(EntityAccess.isAccessible(page, AccessType.ACCESS_DELETE, null));
    }

    @Test
    void ownerCanEditDefaultPage() {
        AccountEntity owner = account(SecurityLevel.ACTIVE_USER);
        ItemEntity page = ownedPage(owner);
        assertTrue(EntityAccess.isAccessible(page, AccessType.ACCESS_EDIT, owner));
        assertTrue(EntityAccess.isAccessible(page, AccessType.ACCESS_DELETE, owner));
    }

    @Test
    void levelsRespectAccessSettings() {
        AccountEntity owner = account(SecurityLevel.ADMIN);
        ItemEntity page = ownedPage(owner);
        AccountEntity user = account(SecurityLevel.ACTIVE_USER);
        AccountEntity moderator = account(SecurityLevel.MODERATOR);
        AccountEntity operator = account(SecurityLevel.OPERATOR);

        assertFalse(EntityAccess.isAccessible(page, AccessType.ACCESS_EDIT, user));
        assertFalse(EntityAccess.isAccessible(page, AccessType.ACCESS_DELETE, operator));
        assertTrue(EntityAccess.isAccessible(page, AccessType.ACCESS_DELETE, moderator));
        assertTrue(EntityAccess.isAccessible(page, AccessType.ACCESS_DELETE, owner));
    }

    @Test
    void guideReadRequiresGuideWhenSettingsSaySo() {
        AccountEntity owner = account(SecurityLevel.ADMIN);
        ItemEntity page = ownedPage(owner);
        Map<AccessType, SecurityLevel> settings = new EnumMap<>(AccessType.class);
        settings.putAll(EntityAccessConstants.GUIDE);
        page.setSecurityData(new SecurityData(settings));

        assertFalse(EntityAccess.isAccessible(page, AccessType.ACCESS_READ, null));
        assertFalse(EntityAccess.isAccessible(page, AccessType.ACCESS_READ, account(SecurityLevel.ACTIVE_USER)));
        assertTrue(EntityAccess.isAccessible(page, AccessType.ACCESS_READ, account(SecurityLevel.GUIDE)));
    }

    private static AccountEntity account(SecurityLevel level) {
        AccountEntity entity = new AccountEntity(new AccountData("user", "hash", "u@example.com", level));
        entity.setId(new ObjectId());
        return entity;
    }

    private static ItemEntity ownedPage(AccountEntity owner) {
        ItemEntity page = new ItemEntity("page", new PageData(), owner);
        page.setId(new ObjectId());
        return page;
    }
}

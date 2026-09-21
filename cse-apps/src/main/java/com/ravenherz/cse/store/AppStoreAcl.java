package com.ravenherz.cse.store;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import com.ravenherz.cse.dal.role.RoleSeeds;

import java.util.Objects;

public final class AppStoreAcl {

    @FunctionalInterface
    public interface SiteAdmin {
        boolean test(AccountEntity account);
    }

    private static volatile SiteAdmin siteAdmin = AppStoreAcl::legacyLevel;

    private AppStoreAcl() {
    }

    /**
     * Production binds this to editor access. Tests keep the security-level fallback.
     */
    public static void bindSiteAdmin(SiteAdmin check) {
        siteAdmin = check == null ? AppStoreAcl::legacyLevel : check;
    }

    public static void resetSiteAdminForTests() {
        siteAdmin = AppStoreAcl::legacyLevel;
    }

    public static boolean isSiteAdmin(AccountEntity account) {
        return siteAdmin.test(account);
    }

    private static boolean legacyLevel(AccountEntity account) {
        if (account == null || account.getAccountData() == null) {
            return false;
        }
        SecurityLevel level = account.getAccountData().getLevel();
        return level != null && level.getIntLevel() >= SecurityLevel.ADMIN.getIntLevel();
    }

    public static boolean isMember(AccountEntity account) {
        if (account == null || account.getAccountData() == null) {
            return false;
        }
        if (account.getAccountData().isLoginable()) {
            return true;
        }
        return RoleSeeds.loginableFor(account.getAccountData().getLevel());
    }

    public static String ownerId(AccountEntity account) {
        if (account == null || account.getId() == null) {
            return null;
        }
        return account.getId().toHexString();
    }

    public static boolean canReadList(AppStoreAccess access, AccountEntity account) {
        return switch (access) {
            case OWNER, MEMBER_WRITE -> isMember(account);
            case PUBLIC_READ -> true;
            case ADMIN -> isSiteAdmin(account);
        };
    }

    public static boolean listSeesAll(AppStoreAccess access, AccountEntity account) {
        if (isSiteAdmin(account)) {
            return true;
        }
        return access == AppStoreAccess.PUBLIC_READ || access == AppStoreAccess.MEMBER_WRITE;
    }

    public static boolean canReadRow(AppStoreAccess access, AccountEntity account, String rowOwnerId) {
        if (isSiteAdmin(account)) {
            return true;
        }
        return switch (access) {
            case OWNER -> isMember(account) && Objects.equals(ownerId(account), rowOwnerId);
            case PUBLIC_READ -> true;
            case MEMBER_WRITE -> isMember(account);
            case ADMIN -> false;
        };
    }

    public static boolean canWriteRow(AppStoreAccess access, AccountEntity account, String rowOwnerId) {
        if (isSiteAdmin(account)) {
            return true;
        }
        if (!isMember(account)) {
            return false;
        }
        return switch (access) {
            case OWNER, PUBLIC_READ -> Objects.equals(ownerId(account), rowOwnerId);
            case MEMBER_WRITE -> true;
            case ADMIN -> false;
        };
    }

    public static boolean canCreate(AppStoreAccess access, AccountEntity account) {
        if (isSiteAdmin(account)) {
            return true;
        }
        if (!isMember(account)) {
            return false;
        }
        return access != AppStoreAccess.ADMIN;
    }

    public static boolean canDefineSchema(AccountEntity account, boolean storeOpen) {
        if (isSiteAdmin(account)) {
            return true;
        }
        return storeOpen && isMember(account);
    }

    public static boolean needsSignIn(AppStoreAccess access, AccountEntity account) {
        return !isMember(account) && access != AppStoreAccess.PUBLIC_READ;
    }
}

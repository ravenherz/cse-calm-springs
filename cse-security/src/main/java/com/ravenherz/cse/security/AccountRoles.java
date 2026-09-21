package com.ravenherz.cse.security;

import com.ravenherz.cse.dal.dao.RoleService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.RoleEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import com.ravenherz.cse.dal.role.RoleSeeds;

public final class AccountRoles {

    private AccountRoles() {
    }

    public static boolean isOwner(AccountEntity account, RoleService roles) {
        if (account == null || account.getAccountData() == null) {
            return false;
        }
        AccountData data = account.getAccountData();
        if (SecurityLevel.OWNER.equals(data.getLevel())) {
            return true;
        }
        String roleId = data.getRoleId();
        if (roleId == null || roleId.isBlank()) {
            return false;
        }
        if (RoleSeeds.OWNER.equals(roleId) || RoleSeeds.SYSTEM_OWNER.equals(roleId)) {
            return true;
        }
        if (roles == null) {
            return false;
        }
        RoleEntity role = roles.getById(roleId);
        if (role == null) {
            role = roles.getBySlug(roleId);
        }
        return role != null && role.isOwner();
    }

    public static String roleId(AccountEntity account, RoleService roles) {
        if (account == null || account.getAccountData() == null) {
            return null;
        }
        AccountData data = account.getAccountData();
        if (data.getRoleId() != null && !data.getRoleId().isBlank()) {
            if (roles != null && ObjectIdLooks.valid(data.getRoleId()) && roles.getById(data.getRoleId()) != null) {
                return data.getRoleId();
            }
            if (roles != null) {
                RoleEntity bySlug = roles.getBySlug(data.getRoleId());
                if (bySlug != null && bySlug.getId() != null) {
                    return bySlug.getId().toHexString();
                }
            }
            return data.getRoleId();
        }
        String slug = RoleSeeds.slugFor(data.getLevel());
        if (slug == null || roles == null) {
            return slug;
        }
        RoleEntity role = roles.getBySlug(slug);
        return role == null || role.getId() == null ? slug : role.getId().toHexString();
    }

    public static void assign(AccountData data, RoleEntity role, boolean editorAccess) {
        if (data == null || role == null) {
            return;
        }
        data.setRoleId(role.idHex());
        data.setLoginable(role.isLoginable());
        data.setLevel(RoleSeeds.dummyLevel(role.getSlug(), editorAccess));
    }

    public static void assignBySlug(AccountData data, RoleService roles, String slug, boolean editorAccess) {
        if (roles == null) {
            return;
        }
        RoleEntity role = roles.getBySlug(slug);
        if (role != null) {
            assign(data, role, editorAccess);
        }
    }

    private static final class ObjectIdLooks {
        static boolean valid(String hex) {
            return hex != null && hex.length() == 24 && hex.chars().allMatch(c ->
                    (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'));
        }
    }
}

package com.ravenherz.cse.security;

import com.ravenherz.cse.dal.EntityAccess;
import com.ravenherz.cse.dal.dao.RoleService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.RoleEntity;
import com.ravenherz.cse.dal.dto.basic.AccessRule;
import com.ravenherz.cse.dal.dto.basic.SecurityData;
import com.ravenherz.cse.dal.dto.basic.enums.AccessType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class AccessForms {

    private AccessForms() {
    }

    public static void apply(HttpServletRequest request, BasicEntity entity, RoleService roles) {
        if (request == null || entity == null || request.getParameter("accessPosted") == null) {
            return;
        }
        Map<AccessType, AccessRule> settings = new EnumMap<>(AccessType.class);
        settings.put(AccessType.ACCESS_READ, rule(request, "accessRead", roles, true));
        settings.put(AccessType.ACCESS_EDIT, rule(request, "accessEdit", roles, false));
        settings.put(AccessType.ACCESS_DELETE, rule(request, "accessDelete", roles, false));
        SecurityData data = entity.getSecurityData();
        if (data == null) {
            data = new SecurityData();
            entity.setSecurityData(data);
        }
        data.setAccessSettings(settings);
    }

    public static void addLookups(Model model, RoleService roles, List<AccountEntity> accounts) {
        List<RoleEntity> accessRoles = new ArrayList<>();
        if (roles != null) {
            for (RoleEntity role : roles.getAll()) {
                if (role != null && !role.isOwner() && !role.isArchived()) {
                    accessRoles.add(role);
                }
            }
        }
        model.addAttribute("accessRoles", accessRoles);
        List<AccountEntity> loginable = new ArrayList<>();
        if (accounts != null) {
            for (AccountEntity account : accounts) {
                if (account != null && account.getAccountData() != null && account.getAccountData().isLoginable()) {
                    loginable.add(account);
                }
            }
        }
        model.addAttribute("accessAccounts", loginable);
    }

    public static void addToModel(Model model, BasicEntity entity, RoleService roles,
            List<AccountEntity> accounts, boolean canEditAccess) {
        SecurityData data = entity == null || entity.getSecurityData() == null
                ? new SecurityData() : entity.getSecurityData();
        model.addAttribute("accessRead", data.rule(AccessType.ACCESS_READ));
        model.addAttribute("accessEdit", data.rule(AccessType.ACCESS_EDIT));
        model.addAttribute("accessDelete", data.rule(AccessType.ACCESS_DELETE));
        addLookups(model, roles, accounts);
        model.addAttribute("accessCanEdit", canEditAccess);
    }

    public static boolean canEdit(BasicEntity entity, AccountEntity accessor) {
        return entity != null && EntityAccess.isAccessible(entity, AccessType.ACCESS_EDIT, accessor);
    }

    public static boolean guestDenied(BasicEntity entity) {
        return entity != null && !EntityAccess.isAccessible(entity, AccessType.ACCESS_READ, null);
    }

    private static AccessRule rule(HttpServletRequest request, String prefix, RoleService roles,
            boolean allowGuest) {
        boolean inherit = "true".equals(request.getParameter(prefix + "Inherit"))
                || "on".equals(request.getParameter(prefix + "Inherit"));
        AccessRule rule = new AccessRule();
        rule.setInherit(inherit);
        List<String> roleIds = new ArrayList<>();
        String[] postedRoles = request.getParameterValues(prefix + "RoleIds");
        if (postedRoles != null && roles != null) {
            for (String raw : postedRoles) {
                RoleEntity role = roles.getById(raw);
                if (role == null) {
                    role = roles.getBySlug(raw);
                }
                if (role == null || role.isOwner() || role.isArchived()) {
                    continue;
                }
                if (role.isGuest() && !allowGuest) {
                    continue;
                }
                roleIds.add(role.idHex());
            }
        }
        rule.setRoleIds(roleIds);
        List<String> accountIds = new ArrayList<>();
        String[] postedAccounts = request.getParameterValues(prefix + "AccountIds");
        if (postedAccounts != null) {
            for (String raw : postedAccounts) {
                if (raw != null && !raw.isBlank()) {
                    accountIds.add(raw.trim());
                }
            }
        }
        rule.setAccountIds(accountIds);
        return rule;
    }
}

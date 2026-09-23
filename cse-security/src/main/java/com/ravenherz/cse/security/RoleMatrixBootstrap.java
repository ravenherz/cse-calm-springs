package com.ravenherz.cse.security;

import com.ravenherz.cse.dal.EntityAccess;
import com.ravenherz.cse.dal.EntityAccessLookup;
import com.ravenherz.cse.dal.dao.AccountService;
import com.ravenherz.cse.dal.dao.RoleMatrixService;
import com.ravenherz.cse.dal.dao.RoleService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.RoleEntity;
import com.ravenherz.cse.dal.role.RoleSeeds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RoleMatrixBootstrap implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoleMatrixBootstrap.class);

    private final RoleService roles;
    private final RoleMatrixService matrix;
    private final AccountService accounts;
    private final EntityAccessLookup accessLookup;
    private final CapabilityCatalog catalog;

    public RoleMatrixBootstrap(RoleService roles, RoleMatrixService matrix, AccountService accounts,
            EntityAccessLookup accessLookup, CapabilityCatalog catalog) {
        this.roles = roles;
        this.matrix = matrix;
        this.accounts = accounts;
        this.accessLookup = accessLookup;
        this.catalog = catalog;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            roles.ensureSeeded();
            matrix.ensureSeeded(roles);
            catalog.refresh();
            backfillAccounts();
            EntityAccess.setLookup(accessLookup);
        } catch (Exception ex) {
            LOGGER.warn("Role matrix seed skipped: {}", ex.getMessage());
        }
    }

    private void backfillAccounts() {
        for (AccountEntity account : accounts.getAllAccounts()) {
            if (account == null || account.getAccountData() == null) {
                continue;
            }
            String roleId = account.getAccountData().getRoleId();
            RoleEntity role = null;
            if (roleId != null && !roleId.isBlank()) {
                role = roles.getById(roleId);
                if (role == null) {
                    role = roles.getBySlug(roleId);
                }
            }
            if (role == null) {
                String slug = RoleSeeds.slugFor(account.getAccountData().getLevel());
                if (slug == null) {
                    slug = account.getAccountData().isLoginable() ? RoleSeeds.MEMBER : RoleSeeds.INACTIVE;
                }
                role = roles.getBySlug(slug);
            }
            if (role == null) {
                continue;
            }
            boolean editor = matrix.allows(role.idHex(), com.ravenherz.cse.dal.role.CapabilityIds.EDITOR_ACCESS)
                    || role.isOwner();
            AccountRoles.assign(account.getAccountData(), role, editor);
            accounts.replace(account);
        }
    }
}

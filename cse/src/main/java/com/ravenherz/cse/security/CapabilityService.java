package com.ravenherz.cse.security;

import com.ravenherz.cse.dal.dao.RoleMatrixService;
import com.ravenherz.cse.dal.dao.RoleService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.RoleEntity;
import com.ravenherz.cse.dal.role.CapabilityIds;
import com.ravenherz.cse.install.SiteReady;
import com.ravenherz.cse.util.staticapps.StaticAppDeployer;
import org.springframework.stereotype.Service;

@Service
public class CapabilityService {

    private final CapabilityCatalog catalog;
    private final RoleService roles;
    private final RoleMatrixService matrix;
    private final SiteReady siteReady;

    public CapabilityService(CapabilityCatalog catalog, RoleService roles, RoleMatrixService matrix,
            SiteReady siteReady) {
        this.catalog = catalog;
        this.roles = roles;
        this.matrix = matrix;
        this.siteReady = siteReady;
        AccessRuntime.bind(this);
    }

    public boolean allows(AccountEntity accountOrNull, String capabilityId) {
        if (capabilityId == null || capabilityId.isBlank()) {
            return false;
        }
        if (firstBootCapability(capabilityId)) {
            return true;
        }
        if (accountOrNull != null && AccountRoles.isOwner(accountOrNull, roles)) {
            return true;
        }
        CapabilityRecord record = catalog.find(capabilityId);
        if (accountOrNull == null) {
            if (record != null && !record.guestSafe()) {
                return false;
            }
            RoleEntity guest = roles.guest();
            return guest != null && matrix.allows(guest.idHex(), capabilityId);
        }
        if (record == null && !CapabilityIds.isApp(capabilityId)) {
            return false;
        }
        String roleId = AccountRoles.roleId(accountOrNull, roles);
        return roleId != null && matrix.allows(roleId, capabilityId);
    }

    public boolean allowsApp(AccountEntity account, String slug) {
        if (slug == null || slug.isBlank()) {
            return false;
        }
        String id = CapabilityIds.app(slug.trim().toLowerCase());
        if ("admin".equals(slug.trim().toLowerCase()) && allows(account, CapabilityIds.EDITOR_ACCESS)) {
            return true;
        }
        if (allows(account, id)) {
            return true;
        }
        if (account == null || account.getId() == null) {
            return false;
        }
        return matrix.allowsAppAccount(account.getId().toHexString(), id);
    }

    public boolean canOpenEditor(AccountEntity account) {
        return allows(account, CapabilityIds.EDITOR_ACCESS);
    }

    public CapabilityCatalog catalog() {
        return catalog;
    }

    /**
     * Fresh instances have no Mongo yet, so Guest and the role matrix do not exist.
     * Setup still has to load {@code /}, {@code /apps/setup/}, and install endpoints.
     */
    private boolean firstBootCapability(String capabilityId) {
        if (siteReady == null || siteReady.isConfigured()) {
            return false;
        }
        return CapabilityIds.SITE_READ.equals(capabilityId)
                || CapabilityIds.INSTALL.equals(capabilityId)
                || CapabilityIds.app(StaticAppDeployer.INSTALLER_SLUG).equals(capabilityId);
    }
}

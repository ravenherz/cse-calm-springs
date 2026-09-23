package com.ravenherz.cse.security;

import com.ravenherz.cse.dal.EntityId;

import com.ravenherz.cse.dal.dao.RoleMatrixService;
import com.ravenherz.cse.dal.dao.RoleService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.RoleEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import com.ravenherz.cse.dal.role.CapabilityIds;
import com.ravenherz.cse.dal.role.RoleSeeds;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CapabilityServiceTest {

    private CapabilityCatalog catalog;
    private RoleService roles;
    private RoleMatrixService matrix;
    private InstanceConfigured siteReady;
    private CapabilityService service;
    private RoleEntity guest;
    private RoleEntity member;

    @BeforeEach
    void setUp() {
        catalog = mock(CapabilityCatalog.class);
        roles = mock(RoleService.class);
        matrix = mock(RoleMatrixService.class);
        siteReady = mock(InstanceConfigured.class);
        when(siteReady.isConfigured()).thenReturn(true);
        guest = role(RoleSeeds.GUEST, true);
        member = role(RoleSeeds.MEMBER, false);
        when(roles.guest()).thenReturn(guest);
        when(roles.getBySlug(RoleSeeds.GUEST)).thenReturn(guest);
        when(roles.getBySlug(RoleSeeds.MEMBER)).thenReturn(member);
        when(roles.getById(guest.idHex())).thenReturn(guest);
        when(roles.getById(member.idHex())).thenReturn(member);
        when(catalog.find(CapabilityIds.SITE_READ))
                .thenReturn(CapabilityRecord.engine(CapabilityIds.SITE_READ, "site", "Public site", true));
        when(catalog.find(CapabilityIds.EDITOR_ACCESS))
                .thenReturn(CapabilityRecord.engine(CapabilityIds.EDITOR_ACCESS, "editor", "Open Catalog", false));
        service = new CapabilityService(catalog, roles, matrix, siteReady);
    }

    @AfterEach
    void unbind() {
        AccessRuntime.bind(null);
    }

    @Test
    void ownerIsAlwaysAllowed() {
        AccountEntity owner = account(SecurityLevel.OWNER);
        assertTrue(service.allows(owner, CapabilityIds.EDITOR_ACCESS));
        assertTrue(service.canOpenEditor(owner));
    }

    @Test
    void guestUsesGuestSafeMatrixRow() {
        when(matrix.allows(guest.idHex(), CapabilityIds.SITE_READ)).thenReturn(true);
        assertTrue(service.allows(null, CapabilityIds.SITE_READ));
        assertFalse(service.allows(null, CapabilityIds.EDITOR_ACCESS));
    }

    @Test
    void memberFollowsMatrix() {
        AccountEntity account = account(SecurityLevel.ACTIVE_USER);
        when(matrix.allows(member.idHex(), CapabilityIds.SITE_READ)).thenReturn(true);
        when(matrix.allows(member.idHex(), CapabilityIds.EDITOR_ACCESS)).thenReturn(false);
        assertTrue(service.allows(account, CapabilityIds.SITE_READ));
        assertFalse(service.canOpenEditor(account));
    }

    @Test
    void unconfiguredInstanceAllowsSetupWithoutGuestRole() {
        when(siteReady.isConfigured()).thenReturn(false);
        when(roles.guest()).thenReturn(null);
        assertTrue(service.allows(null, CapabilityIds.SITE_READ));
        assertTrue(service.allows(null, CapabilityIds.INSTALL));
        assertTrue(service.allowsApp(null, "setup"));
        assertFalse(service.allows(null, CapabilityIds.EDITOR_ACCESS));
        assertFalse(service.allowsApp(null, "projects"));
    }

    private static RoleEntity role(String slug, boolean system) {
        RoleEntity entity = new RoleEntity();
        entity.setId(new ObjectId());
        entity.setSlug(slug);
        entity.setSystem(system ? slug : null);
        entity.setLoginable(!RoleSeeds.GUEST.equals(slug));
        return entity;
    }

    private static AccountEntity account(SecurityLevel level) {
        AccountEntity entity = new AccountEntity(new AccountData("user", "hash", "u@example.com", level));
        entity.setId(EntityId.generate());
        return entity;
    }
}

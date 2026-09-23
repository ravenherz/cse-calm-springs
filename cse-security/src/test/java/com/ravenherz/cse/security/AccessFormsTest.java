package com.ravenherz.cse.security;

import com.ravenherz.cse.dal.EntityId;

import com.ravenherz.cse.dal.dao.RoleService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.RoleEntity;
import com.ravenherz.cse.dal.dto.basic.AccessRule;
import com.ravenherz.cse.dal.role.RoleSeeds;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccessFormsTest {

    @Test
    void applyMapsInheritAndDropsUnknownAndGuestEditRoles() {
        RoleEntity member = role(RoleSeeds.MEMBER, false);
        RoleEntity guest = role(RoleSeeds.GUEST, true);
        RoleService roles = mock(RoleService.class);
        when(roles.getById(member.idHex())).thenReturn(member);
        when(roles.getBySlug(member.getSlug())).thenReturn(member);
        when(roles.getById(guest.idHex())).thenReturn(guest);
        when(roles.getBySlug(guest.getSlug())).thenReturn(guest);
        when(roles.getById("deadbeef")).thenReturn(null);
        when(roles.getBySlug("deadbeef")).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("accessPosted", "1");
        request.addParameter("accessReadInherit", "true");
        request.addParameter("accessEditInherit", "false");
        request.addParameter("accessEditRoleIds", member.idHex(), "deadbeef", guest.idHex());
        request.addParameter("accessDeleteInherit", "true");

        AccountEntity file = new AccountEntity();
        AccessForms.apply(request, file, roles);

        AccessRule read = file.getSecurityData().getRead();
        AccessRule edit = file.getSecurityData().getEdit();
        AccessRule delete = file.getSecurityData().getDelete();
        assertTrue(read.isInherit());
        assertFalse(edit.isInherit());
        assertEquals(List.of(member.idHex()), edit.getRoleIds());
        assertTrue(delete.isInherit());
    }

    @Test
    void addLookupsOmitsOwnerAndDoesNotNeedAnEntity() {
        RoleEntity member = role(RoleSeeds.MEMBER, false);
        RoleEntity owner = role(RoleSeeds.OWNER, true);
        RoleEntity archived = role("old", false);
        archived.setArchived(true);
        RoleService roles = mock(RoleService.class);
        when(roles.getAll()).thenReturn(List.of(member, owner, archived));

        Model model = new ConcurrentModel();
        AccessForms.addLookups(model, roles, List.of());

        @SuppressWarnings("unchecked")
        List<RoleEntity> accessRoles = (List<RoleEntity>) model.getAttribute("accessRoles");
        assertEquals(List.of(member), accessRoles);
        assertFalse(model.containsAttribute("accessRead"));
        assertFalse(model.containsAttribute("accessCanEdit"));
    }

    private static RoleEntity role(String slug, boolean system) {
        RoleEntity entity = new RoleEntity();
        entity.setId(new ObjectId());
        entity.setSlug(slug);
        entity.setName(slug);
        entity.setSystem(system ? slug : null);
        return entity;
    }
}

package com.ravenherz.cse.present;

import com.ravenherz.cse.dal.EntityId;

import com.ravenherz.cse.dal.dto.RoleEntity;
import com.ravenherz.cse.dal.role.CapabilityIds;
import com.ravenherz.cse.dal.role.RoleSeeds;
import com.ravenherz.cse.security.CapabilityRecord;
import com.ravenherz.cse.security.RoleMatrixView;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleMatrixViewTest {

    @Test
    void groupsAreSeparateTabsInFixedOrderWithAlphaRows() {
        RoleEntity member = role(RoleSeeds.MEMBER, false);
        RoleEntity owner = role(RoleSeeds.OWNER, true);
        List<CapabilityRecord> caps = List.of(
                CapabilityRecord.engine(CapabilityIds.EDITOR_PAGES, "editor", "Pages", false, "Edit pages."),
                CapabilityRecord.engine(CapabilityIds.EDITOR_ACCOUNTS, "editor", "Accounts", false, "Open Accounts."),
                CapabilityRecord.engine(CapabilityIds.ACCOUNT_AUTH, "account", "Sign in", true, "Post credentials."),
                CapabilityRecord.app("zebra"),
                CapabilityRecord.app("admin"),
                CapabilityRecord.engine(CapabilityIds.CONTENT_READ, "content", "Default content read", true,
                        "Read when inherit applies."));
        List<Map<String, Object>> groups = RoleMatrixView.groups(caps, List.of(member, owner),
                (roleId, capId) -> member.idHex().equals(roleId) && CapabilityIds.ACCOUNT_AUTH.equals(capId));

        assertEquals(List.of("account", "apps", "content", "editor"),
                groups.stream().map(group -> group.get("id")).toList());
        assertEquals("Account", groups.get(0).get("label"));
        assertEquals("Apps", groups.get(1).get("label"));
        assertEquals("Content Defaults", groups.get(2).get("label"));
        assertEquals("Editor", groups.get(3).get("label"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> apps = (List<Map<String, Object>>) groups.get(1).get("rows");
        assertEquals(List.of("Admin app", "App: zebra"),
                apps.stream().map(row -> row.get("label")).toList());
        assertEquals("Open the admin app. Catalog access also grants this.", apps.get(0).get("hint"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> editor = (List<Map<String, Object>>) groups.get(3).get("rows");
        assertEquals(List.of("Accounts", "Pages"),
                editor.stream().map(row -> row.get("label")).toList());
        assertEquals("Open Accounts.", editor.get(0).get("hint"));

        @SuppressWarnings("unchecked")
        Map<String, Boolean> signInCells = (Map<String, Boolean>)
                ((List<Map<String, Object>>) groups.get(0).get("rows")).get(0).get("cells");
        assertTrue(signInCells.get(member.idHex()));
        assertTrue(signInCells.get(owner.idHex()));
    }

    @Test
    void groupLabelsMatchRequestedTabNames() {
        assertEquals("Account", RoleMatrixView.groupLabel("account"));
        assertEquals("Apps", RoleMatrixView.groupLabel("apps"));
        assertEquals("Content Defaults", RoleMatrixView.groupLabel("content"));
        assertEquals("Editor", RoleMatrixView.groupLabel("editor"));
        assertEquals("Install", RoleMatrixView.groupLabel("install"));
        assertEquals("Site", RoleMatrixView.groupLabel("site"));
    }

    private static RoleEntity role(String slug, boolean owner) {
        RoleEntity entity = new RoleEntity();
        entity.setId(new ObjectId());
        entity.setSlug(slug);
        entity.setName(slug);
        entity.setSystem(owner ? RoleSeeds.SYSTEM_OWNER : null);
        entity.setLoginable(true);
        return entity;
    }
}

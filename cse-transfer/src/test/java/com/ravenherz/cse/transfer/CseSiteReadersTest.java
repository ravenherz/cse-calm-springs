package com.ravenherz.cse.transfer;

import com.ravenherz.cse.dal.dto.CategoryEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.RoleEntity;
import com.ravenherz.cse.dal.dto.basic.AccessRule;
import com.ravenherz.cse.dal.dto.basic.enums.AccessType;
import com.ravenherz.cse.dal.role.RoleSeeds;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CseSiteReadersTest {

    @Test
    void securityReadsLegacyStringAndV2Object() {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", "68b000000000000000000002");
        Map<String, Object> access = new LinkedHashMap<>();
        access.put("ACCESS_READ", "GUEST");
        access.put("ACCESS_EDIT", Map.of(
                "inherit", false,
                "roleIds", List.of("admin"),
                "accountIds", List.of()));
        doc.put("securityData", Map.of("accessSettings", access));

        CategoryEntity category = CseSiteReaders.category(doc);
        AccessRule read = category.getSecurityData().rule(AccessType.ACCESS_READ);
        assertFalse(read.isInherit());
        assertTrue(read.getRoleIds().contains(RoleSeeds.GUEST));
        AccessRule edit = category.getSecurityData().rule(AccessType.ACCESS_EDIT);
        assertFalse(edit.isInherit());
        assertEquals(List.of("admin"), edit.getRoleIds());
    }

    @Test
    void roleImportDropsRetiredGuide() {
        Map<String, Object> guide = new LinkedHashMap<>();
        guide.put("id", "68b0000000000000000000aa");
        guide.put("slug", "guide");
        guide.put("name", "Guide");
        assertNull(CseSiteReaders.role(guide));

        Map<String, Object> member = new LinkedHashMap<>();
        member.put("id", "68b0000000000000000000bb");
        member.put("slug", "member");
        member.put("name", "Member");
        RoleEntity read = CseSiteReaders.role(member);
        assertNotNull(read);
        assertEquals(RoleSeeds.MEMBER, read.getSlug());
    }

    @Test
    void pageDataReadsNoTopDisplayImage() {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", "68b000000000000000000005");
        doc.put("itemType", "PAGE");
        doc.put("uniqueUriName", "hello");
        Map<String, Object> pageData = new LinkedHashMap<>();
        pageData.put("title", "Hello");
        pageData.put("noTopDisplayImage", true);
        doc.put("pageData", pageData);

        ItemEntity item = CseSiteReaders.item(doc);
        assertNotNull(item);
        assertTrue(item.getPageData().isNoTopDisplayImage());
    }

    @Test
    void pageDataDefaultsNoTopDisplayImage() {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", "68b000000000000000000006");
        doc.put("itemType", "PAGE");
        doc.put("uniqueUriName", "hello");
        doc.put("pageData", Map.of("title", "Hello"));

        ItemEntity item = CseSiteReaders.item(doc);
        assertNotNull(item);
        assertFalse(item.getPageData().isNoTopDisplayImage());
    }

    @Test
    void pageDataReadsExportPdf() {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", "68b000000000000000000007");
        doc.put("itemType", "PAGE");
        doc.put("uniqueUriName", "hello");
        Map<String, Object> pageData = new LinkedHashMap<>();
        pageData.put("title", "Hello");
        pageData.put("exportPdf", true);
        doc.put("pageData", pageData);

        ItemEntity item = CseSiteReaders.item(doc);
        assertNotNull(item);
        assertTrue(item.getPageData().isExportPdf());
    }

    @Test
    void pageDataDefaultsExportPdf() {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", "68b000000000000000000008");
        doc.put("itemType", "PAGE");
        doc.put("uniqueUriName", "hello");
        doc.put("pageData", Map.of("title", "Hello"));

        ItemEntity item = CseSiteReaders.item(doc);
        assertNotNull(item);
        assertFalse(item.getPageData().isExportPdf());
    }
}

package com.ravenherz.cse.admin;

import com.ravenherz.cse.core.admin.AdminField;
import com.ravenherz.cse.core.admin.AdminFieldError;
import com.ravenherz.cse.core.admin.AdminSection;
import com.ravenherz.cse.core.admin.AdminSectionRecords;
import com.ravenherz.cse.core.admin.AdminSectionSource;
import com.ravenherz.cse.core.admin.FieldType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminSectionCatalogTest {

    @Test
    void requiredEnumAndPathAreCheckedBeforeTheStore() {
        FakeRecords records = new FakeRecords();
        AdminSectionCatalog catalog = catalog(records);
        Map<String, String> posted = Map.of(
                "fromPath", "old",
                "status", "200",
                "enabled", "yes");

        AdminSectionPage page = catalog.create("sample", posted);

        assertEquals("fromPath", page.errors().get(0).field());
        assertTrue(records.created.isEmpty());
        assertEquals("/admin/editor-section", AdminSectionController.VIEW);
    }

    @Test
    void validCreateReachesTheStoreAndKeepsItsFieldError() {
        FakeRecords records = new FakeRecords();
        records.createError = new AdminFieldError("fromPath", "A redirect from this path already exists");
        AdminSectionCatalog catalog = catalog(records);

        AdminSectionPage page = catalog.create("sample", valid());

        assertEquals(1, records.created.size());
        assertEquals("fromPath", page.errors().get(0).field());
    }

    @Test
    void listEditAndDeleteUseTheStore() {
        FakeRecords records = new FakeRecords();
        records.rows.add(Map.of("id", "abc", "fromPath", "/old"));
        AdminSectionCatalog catalog = catalog(records);

        assertEquals("/old", catalog.list("sample").rows().get(0).get("fromPath"));
        assertEquals("/old", catalog.editForm("sample", "abc").values().get("fromPath"));
        assertTrue(catalog.delete("sample", "abc"));
        assertEquals("abc", records.deleted);
        assertTrue(catalog.list("missing").notFound());
    }

    @Test
    void sectionWithoutAStoreCannotBeSaved() {
        AdminSectionCatalog catalog = new AdminSectionCatalog(List.of(source()), List.of());

        AdminSectionPage page = catalog.create("sample", valid());

        assertFalse(page.errors().isEmpty());
        assertFalse(catalog.delete("sample", "abc"));
    }

    private static AdminSectionCatalog catalog(FakeRecords records) {
        return new AdminSectionCatalog(List.of(source()), List.of(records));
    }

    private static AdminSectionSource source() {
        return () -> new AdminSection("sample", "Sample", List.of(
                new AdminField("fromPath", "From", FieldType.PATH, true, List.of()),
                new AdminField("targetEntityId", "Document", FieldType.ENTITY_ID, false, List.of()),
                new AdminField("enabled", "Enabled", FieldType.BOOLEAN, true, List.of()),
                new AdminField("status", "Status", FieldType.ENUM, true, List.of("301", "302"))));
    }

    private static Map<String, String> valid() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("fromPath", "/old");
        fields.put("targetEntityId", "");
        fields.put("enabled", "true");
        fields.put("status", "301");
        fields.put("_csrf", "ignore-me");
        return fields;
    }

    private static final class FakeRecords implements AdminSectionRecords {
        private final List<Map<String, String>> rows = new ArrayList<>();
        private final List<Map<String, String>> created = new ArrayList<>();
        private AdminFieldError createError;
        private String deleted;

        @Override
        public String sectionId() {
            return "sample";
        }

        @Override
        public List<Map<String, String>> list() {
            return rows;
        }

        @Override
        public Map<String, String> find(String id) {
            for (Map<String, String> row : rows) {
                if (id.equals(row.get("id"))) {
                    return row;
                }
            }
            return null;
        }

        @Override
        public List<AdminFieldError> create(Map<String, String> fields) {
            created.add(fields);
            return createError == null ? List.of() : List.of(createError);
        }

        @Override
        public List<AdminFieldError> update(String id, Map<String, String> fields) {
            return List.of();
        }

        @Override
        public void delete(String id) {
            deleted = id;
        }
    }
}

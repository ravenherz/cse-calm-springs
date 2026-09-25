package com.ravenherz.cse.admin;

import com.ravenherz.cse.core.admin.AdminField;
import com.ravenherz.cse.core.admin.AdminFieldError;
import com.ravenherz.cse.core.admin.AdminSection;
import com.ravenherz.cse.core.admin.AdminSectionRecords;
import com.ravenherz.cse.core.admin.AdminSectionSource;
import com.ravenherz.cse.core.admin.FieldType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AdminSectionCatalog {

    private final List<AdminSectionSource> sources;
    private final List<AdminSectionRecords> records;

    public AdminSectionCatalog(List<AdminSectionSource> sources, List<AdminSectionRecords> records) {
        this.sources = sources == null ? List.of() : List.copyOf(sources);
        this.records = records == null ? List.of() : List.copyOf(records);
    }

    public AdminSectionPage list(String sectionId) {
        AdminSection section = section(sectionId);
        if (section == null) {
            return AdminSectionPage.missing();
        }
        AdminSectionRecords store = records(sectionId);
        List<Map<String, String>> rows = store == null ? List.of() : store.list();
        return AdminSectionPage.list(section, rows);
    }

    public AdminSectionPage createForm(String sectionId) {
        AdminSection section = section(sectionId);
        if (section == null) {
            return AdminSectionPage.missing();
        }
        return AdminSectionPage.form(section, "create", null, Map.of(), List.of());
    }

    public AdminSectionPage editForm(String sectionId, String id) {
        AdminSection section = section(sectionId);
        if (section == null) {
            return AdminSectionPage.missing();
        }
        AdminSectionRecords store = records(sectionId);
        Map<String, String> values = store == null ? null : store.find(id);
        if (values == null) {
            return AdminSectionPage.missing();
        }
        return AdminSectionPage.form(section, "edit", id, values, List.of());
    }

    public AdminSectionPage create(String sectionId, Map<String, String> posted) {
        return save(sectionId, null, posted, true);
    }

    public AdminSectionPage update(String sectionId, String id, Map<String, String> posted) {
        return save(sectionId, id, posted, false);
    }

    public boolean delete(String sectionId, String id) {
        AdminSectionRecords store = records(sectionId);
        if (section(sectionId) == null || store == null) {
            return false;
        }
        store.delete(id);
        return true;
    }

    private AdminSectionPage save(String sectionId, String id, Map<String, String> posted, boolean creating) {
        AdminSection section = section(sectionId);
        if (section == null) {
            return AdminSectionPage.missing();
        }
        Map<String, String> fields = declared(section, posted);
        List<AdminFieldError> errors = validate(section, fields);
        if (!errors.isEmpty()) {
            return AdminSectionPage.form(section, creating ? "create" : "edit", id, fields, errors);
        }
        AdminSectionRecords store = records(sectionId);
        if (store == null) {
            return AdminSectionPage.form(section, creating ? "create" : "edit", id, fields, List.of(
                    new AdminFieldError("", "This section cannot be saved yet")));
        }
        List<AdminFieldError> fromModule = creating ? store.create(fields) : store.update(id, fields);
        if (fromModule != null && !fromModule.isEmpty()) {
            return AdminSectionPage.form(section, creating ? "create" : "edit", id, fields, fromModule);
        }
        return AdminSectionPage.form(section, creating ? "create" : "edit", id, fields, List.of());
    }

    private AdminSection section(String sectionId) {
        for (AdminSectionSource source : sources) {
            AdminSection section = source.section();
            if (section != null && section.id().equals(sectionId)) {
                return section;
            }
        }
        return null;
    }

    private AdminSectionRecords records(String sectionId) {
        for (AdminSectionRecords store : records) {
            if (sectionId.equals(store.sectionId())) {
                return store;
            }
        }
        return null;
    }

    private static Map<String, String> declared(AdminSection section, Map<String, String> posted) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (AdminField field : section.fields()) {
            String value = posted == null ? null : posted.get(field.name());
            fields.put(field.name(), value == null ? "" : value.trim());
        }
        return fields;
    }

    private static List<AdminFieldError> validate(AdminSection section, Map<String, String> fields) {
        List<AdminFieldError> errors = new ArrayList<>();
        for (AdminField field : section.fields()) {
            String value = fields.get(field.name());
            if (value == null || value.isBlank()) {
                if (field.required()) {
                    errors.add(new AdminFieldError(field.name(), "Required"));
                }
                continue;
            }
            if (field.type() == FieldType.PATH && !value.startsWith("/")) {
                errors.add(new AdminFieldError(field.name(), "Use a path that starts with /"));
            } else if (field.type() == FieldType.BOOLEAN && !isBoolean(value)) {
                errors.add(new AdminFieldError(field.name(), "Use true or false"));
            } else if (field.type() == FieldType.ENUM && !field.options().contains(value)) {
                errors.add(new AdminFieldError(field.name(), "Choose one of the listed values"));
            } else if (field.type() == FieldType.ENTITY_ID && !isEntityId(value)) {
                errors.add(new AdminFieldError(field.name(), "Use a 24 character id"));
            }
        }
        return errors;
    }

    private static boolean isBoolean(String value) {
        String text = value.toLowerCase(Locale.ROOT);
        return "true".equals(text) || "false".equals(text);
    }

    private static boolean isEntityId(String value) {
        if (value.length() != 24) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!hex) {
                return false;
            }
        }
        return true;
    }
}

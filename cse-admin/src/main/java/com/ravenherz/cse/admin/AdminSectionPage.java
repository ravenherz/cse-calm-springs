package com.ravenherz.cse.admin;

import com.ravenherz.cse.core.admin.AdminFieldError;
import com.ravenherz.cse.core.admin.AdminSection;

import java.util.List;
import java.util.Map;

public final class AdminSectionPage {

    private final AdminSection section;
    private final String mode;
    private final String id;
    private final List<Map<String, String>> rows;
    private final Map<String, String> values;
    private final List<AdminFieldError> errors;
    private final boolean notFound;

    private AdminSectionPage(AdminSection section, String mode, String id,
            List<Map<String, String>> rows, Map<String, String> values,
            List<AdminFieldError> errors, boolean notFound) {
        this.section = section;
        this.mode = mode;
        this.id = id;
        this.rows = rows == null ? List.of() : List.copyOf(rows);
        this.values = values == null ? Map.of() : Map.copyOf(values);
        this.errors = errors == null ? List.of() : List.copyOf(errors);
        this.notFound = notFound;
    }

    static AdminSectionPage missing() {
        return new AdminSectionPage(null, "missing", null, List.of(), Map.of(), List.of(), true);
    }

    public static AdminSectionPage list(AdminSection section, List<Map<String, String>> rows) {
        return new AdminSectionPage(section, "list", null, rows, Map.of(), List.of(), false);
    }

    public static AdminSectionPage form(AdminSection section, String mode, String id,
            Map<String, String> values, List<AdminFieldError> errors) {
        return new AdminSectionPage(section, mode, id, List.of(), values, errors, false);
    }

    public AdminSection section() {
        return section;
    }

    public String mode() {
        return mode;
    }

    public String id() {
        return id;
    }

    public List<Map<String, String>> rows() {
        return rows;
    }

    public Map<String, String> values() {
        return values;
    }

    public List<AdminFieldError> errors() {
        return errors;
    }

    public boolean notFound() {
        return notFound;
    }
}

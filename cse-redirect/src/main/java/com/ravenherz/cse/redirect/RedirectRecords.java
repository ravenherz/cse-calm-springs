package com.ravenherz.cse.redirect;

import com.ravenherz.cse.core.admin.AdminFieldError;
import com.ravenherz.cse.core.admin.AdminSectionRecords;
import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.dto.BasicEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public final class RedirectRecords implements AdminSectionRecords {

    private final ResourceRedirectService store;
    private final List<RedirectReload> reloaders;

    public RedirectRecords(ResourceRedirectService store, List<RedirectReload> reloaders) {
        this.store = store;
        this.reloaders = reloaders == null ? List.of() : List.copyOf(reloaders);
    }

    @Override
    public String sectionId() {
        return RedirectAdmin.SECTION_ID;
    }

    @Override
    public List<Map<String, String>> list() {
        List<Map<String, String>> rows = new ArrayList<>();
        for (BasicEntity entity : store.getAll()) {
            if (entity instanceof ResourceRedirectEntity redirect) {
                rows.add(fields(redirect));
            }
        }
        return rows;
    }

    @Override
    public Map<String, String> find(String id) {
        ResourceRedirectEntity row = load(id);
        return row == null ? null : fields(row);
    }

    @Override
    public List<AdminFieldError> create(Map<String, String> fields) {
        ResourceRedirectEntity row = new ResourceRedirectEntity();
        apply(row, fields);
        List<AdminFieldError> errors = check(row);
        if (!errors.isEmpty()) {
            return errors;
        }
        store.insert(row);
        reload();
        return List.of();
    }

    @Override
    public List<AdminFieldError> update(String id, Map<String, String> fields) {
        ResourceRedirectEntity row = load(id);
        if (row == null) {
            return List.of(new AdminFieldError("", "That redirect was not found"));
        }
        apply(row, fields);
        List<AdminFieldError> errors = check(row);
        if (!errors.isEmpty()) {
            return errors;
        }
        store.replace(row);
        reload();
        return List.of();
    }

    @Override
    public void delete(String id) {
        ResourceRedirectEntity row = load(id);
        if (row == null) {
            return;
        }
        store.delete(row);
        reload();
    }

    private List<AdminFieldError> check(ResourceRedirectEntity row) {
        List<ResourceRedirectEntity> existing = new ArrayList<>();
        for (BasicEntity entity : store.getAll()) {
            if (entity instanceof ResourceRedirectEntity redirect) {
                existing.add(redirect);
            }
        }
        List<AdminFieldError> errors = new ArrayList<>();
        for (RedirectFieldError error : RedirectRules.check(row, existing)) {
            errors.add(new AdminFieldError(error.field(), error.message()));
        }
        return errors;
    }

    private ResourceRedirectEntity load(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        try {
            BasicEntity entity = store.getById(ResourceRedirectEntity.class, EntityId.of(id.trim()));
            return entity instanceof ResourceRedirectEntity redirect ? redirect : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void reload() {
        for (RedirectReload reloader : reloaders) {
            reloader.reloadRedirects();
        }
    }

    private static void apply(ResourceRedirectEntity row, Map<String, String> fields) {
        Map<String, String> values = fields == null ? Map.of() : fields;
        row.setFromPath(values.get("fromPath"));
        row.setTargetPath(values.get("targetPath"));
        String document = values.get("targetEntityId");
        if (document == null || document.isBlank()) {
            row.setTargetEntityId(null);
        } else {
            row.setTargetEntityId(EntityId.of(document.trim()));
        }
        row.setEnabled(!"false".equalsIgnoreCase(blank(values.get("enabled"))));
        row.setPreserveQuery("true".equalsIgnoreCase(blank(values.get("preserveQuery"))));
        row.setStatus(status(values.get("status")));
    }

    private static Map<String, String> fields(ResourceRedirectEntity row) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("id", row.getId() == null ? "" : row.getId().toString());
        values.put("fromPath", blank(row.getFromPath()));
        values.put("targetPath", blank(row.getTargetPath()));
        values.put("targetEntityId", row.getTargetEntityId() == null ? "" : row.getTargetEntityId().toString());
        values.put("enabled", Boolean.toString(row.isEnabled()));
        values.put("status", Integer.toString(row.getStatus()));
        values.put("preserveQuery", Boolean.toString(row.isPreserveQuery()));
        return values;
    }

    private static int status(String raw) {
        try {
            return Integer.parseInt(blank(raw));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static String blank(String value) {
        return value == null ? "" : value.trim();
    }
}

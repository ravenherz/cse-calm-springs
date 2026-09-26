package com.ravenherz.cse.scripting;

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
public final class ScriptRecords implements AdminSectionRecords {

    private final ScriptService store;
    private final List<ScriptChanged> listeners;

    public ScriptRecords(ScriptService store, List<ScriptChanged> listeners) {
        this.store = store;
        this.listeners = listeners == null ? List.of() : List.copyOf(listeners);
    }

    @Override
    public String sectionId() {
        return ScriptAdmin.SECTION_ID;
    }

    @Override
    public List<Map<String, String>> list() {
        List<Map<String, String>> rows = new ArrayList<>();
        for (ScriptEntity script : store.list()) {
            if (script != null) {
                rows.add(fields(script));
            }
        }
        return rows;
    }

    @Override
    public Map<String, String> find(String id) {
        ScriptEntity row = load(id);
        return row == null ? null : fields(row);
    }

    @Override
    public List<AdminFieldError> create(Map<String, String> fields) {
        List<AdminFieldError> errors = validate(null, fields);
        if (!errors.isEmpty()) {
            return errors;
        }
        ScriptEntity row = new ScriptEntity(ScriptRules.normalizeId(value(fields, "scriptId")),
                ScriptRules.normalizeFolder(value(fields, "folder")), value(fields, "source"), null);
        row.setId(EntityId.generate());
        store.insert(row);
        changed();
        return List.of();
    }

    @Override
    public List<AdminFieldError> update(String id, Map<String, String> fields) {
        ScriptEntity row = load(id);
        if (row == null) {
            return List.of(new AdminFieldError("", "That script was not found"));
        }
        List<AdminFieldError> errors = validate(row, fields);
        if (!errors.isEmpty()) {
            return errors;
        }
        row.setScriptId(ScriptRules.normalizeId(value(fields, "scriptId")));
        row.setFolder(ScriptRules.normalizeFolder(value(fields, "folder")));
        row.setSource(value(fields, "source"));
        store.replace(row);
        changed();
        return List.of();
    }

    @Override
    public void delete(String id) {
        ScriptEntity row = load(id);
        if (row == null) {
            return;
        }
        store.delete(row);
        changed();
    }

    private List<AdminFieldError> validate(ScriptEntity current, Map<String, String> fields) {
        List<AdminFieldError> errors = new ArrayList<>();
        String idError = ScriptRules.idError(value(fields, "scriptId"));
        if (idError != null) {
            errors.add(new AdminFieldError("scriptId", idError));
        }
        String folderError = ScriptRules.folderError(value(fields, "folder"));
        if (folderError != null) {
            errors.add(new AdminFieldError("folder", folderError));
        }
        String sourceError = ScriptRules.sourceError(value(fields, "source"));
        if (sourceError != null) {
            errors.add(new AdminFieldError("source", sourceError));
        }
        if (idError == null) {
            ScriptEntity duplicate = store.findByScriptId(ScriptRules.normalizeId(value(fields, "scriptId")));
            if (duplicate != null && (current == null || current.getId() == null
                    || !current.getId().equals(duplicate.getId()))) {
                errors.add(new AdminFieldError("scriptId", "A script with this id already exists"));
            }
        }
        return errors;
    }

    private ScriptEntity load(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        try {
            BasicEntity entity = store.getById(ScriptEntity.class, EntityId.of(id.trim()));
            return entity instanceof ScriptEntity script ? script : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void changed() {
        for (ScriptChanged listener : listeners) {
            listener.scriptsChanged();
        }
    }

    private static Map<String, String> fields(ScriptEntity row) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("id", row.getId() == null ? "" : row.getId().toString());
        values.put("scriptId", row.getScriptId() == null ? "" : row.getScriptId());
        values.put("folder", row.getFolder());
        values.put("source", row.getSource());
        return values;
    }

    private static String value(Map<String, String> fields, String name) {
        if (fields == null || fields.get(name) == null) {
            return "";
        }
        return fields.get(name);
    }
}

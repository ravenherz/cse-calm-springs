package com.ravenherz.cse.store;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AppStoreTableSpec {

    private String name;
    private AppStoreAccess access = AppStoreAccess.OWNER;
    private Map<String, Object> schema;

    public AppStoreTableSpec() {
    }

    public AppStoreTableSpec(String name, AppStoreAccess access, Map<String, Object> schema) {
        this.name = AppStoreNames.requireTable(name);
        this.access = access == null ? AppStoreAccess.OWNER : access;
        this.schema = schema == null ? null : new LinkedHashMap<>(schema);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AppStoreAccess getAccess() {
        return access == null ? AppStoreAccess.OWNER : access;
    }

    public void setAccess(AppStoreAccess access) {
        this.access = access;
    }

    public Map<String, Object> getSchema() {
        return schema;
    }

    public void setSchema(Map<String, Object> schema) {
        this.schema = schema;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("name", name);
        out.put("access", getAccess().wire());
        if (schema != null && !schema.isEmpty()) {
            out.put("schema", schema);
        }
        return out;
    }

    public static AppStoreTableSpec fromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        Object rawName = map.get("name");
        if (rawName == null || !AppStoreNames.isTable(rawName.toString())) {
            return null;
        }
        Object rawAccess = map.get("access");
        AppStoreAccess access = AppStoreAccess.parseOrDefault(
                rawAccess == null ? null : rawAccess.toString());
        Map<String, Object> schema = null;
        Object rawSchema = map.get("schema");
        if (rawSchema instanceof Map<?, ?> nested) {
            schema = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : nested.entrySet()) {
                if (entry.getKey() != null) {
                    schema.put(entry.getKey().toString(), entry.getValue());
                }
            }
        }
        return new AppStoreTableSpec(rawName.toString(), access, schema);
    }

    public static List<AppStoreTableSpec> listFrom(Object raw) {
        List<AppStoreTableSpec> out = new ArrayList<>();
        if (!(raw instanceof List<?> list)) {
            return out;
        }
        for (Object row : list) {
            if (!(row instanceof Map<?, ?> map)) {
                continue;
            }
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    copy.put(entry.getKey().toString(), entry.getValue());
                }
            }
            AppStoreTableSpec spec = fromMap(copy);
            if (spec != null) {
                out.add(spec);
            }
        }
        return out;
    }

    public static List<AppStoreTableSpec> merge(List<AppStoreTableSpec> existing,
            List<AppStoreTableSpec> incoming) {
        List<AppStoreTableSpec> out = new ArrayList<>();
        Set<String> names = new LinkedHashSet<>();
        if (existing != null) {
            for (AppStoreTableSpec spec : existing) {
                if (spec == null || spec.getName() == null || !names.add(spec.getName())) {
                    continue;
                }
                out.add(spec);
            }
        }
        if (incoming != null) {
            for (AppStoreTableSpec spec : incoming) {
                if (spec == null || spec.getName() == null || !names.add(spec.getName())) {
                    continue;
                }
                out.add(spec);
            }
        }
        return out;
    }
}

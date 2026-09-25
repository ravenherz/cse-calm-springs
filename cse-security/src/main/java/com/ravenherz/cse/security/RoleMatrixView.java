package com.ravenherz.cse.security;

import com.ravenherz.cse.dal.dto.RoleEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

public final class RoleMatrixView {

    private static final List<String> GROUP_ORDER = List.of(
            "account", "apps", "content", "editor", "install", "site");

    private RoleMatrixView() {
    }

    public static List<Map<String, Object>> groups(
            List<CapabilityRecord> capabilities,
            List<RoleEntity> allRoles,
            BiPredicate<String, String> roleAllowsCapability) {
        Map<String, List<CapabilityRecord>> grouped = new LinkedHashMap<>();
        for (String id : GROUP_ORDER) {
            grouped.put(id, new ArrayList<>());
        }
        if (capabilities != null) {
            for (CapabilityRecord record : capabilities) {
                if (record == null || record.group() == null) {
                    continue;
                }
                grouped.computeIfAbsent(record.group(), key -> new ArrayList<>()).add(record);
            }
        }
        List<RoleEntity> columns = allRoles == null ? List.of() : allRoles.stream()
                .filter(role -> role != null && !role.isArchived())
                .toList();
        List<Map<String, Object>> groups = new ArrayList<>();
        for (Map.Entry<String, List<CapabilityRecord>> entry : grouped.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            List<CapabilityRecord> records = new ArrayList<>(entry.getValue());
            records.sort(Comparator
                    .comparing(CapabilityRecord::label, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(CapabilityRecord::id, String.CASE_INSENSITIVE_ORDER));
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("id", entry.getKey());
            group.put("label", groupLabel(entry.getKey()));
            List<Map<String, Object>> rows = new ArrayList<>();
            for (CapabilityRecord record : records) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", record.id());
                row.put("label", record.label());
                row.put("hint", record.hint());
                row.put("guestSafe", record.guestSafe());
                Map<String, Boolean> cells = new LinkedHashMap<>();
                for (RoleEntity role : columns) {
                    boolean checked = role.isOwner()
                            || (roleAllowsCapability != null
                                    && roleAllowsCapability.test(role.idHex(), record.id()));
                    cells.put(role.idHex(), checked);
                }
                row.put("cells", cells);
                rows.add(row);
            }
            group.put("rows", rows);
            groups.add(group);
        }
        return groups;
    }

    public static String groupLabel(String group) {
        return switch (group) {
            case "account" -> "Account";
            case "site" -> "Site";
            case "apps" -> "Apps";
            case "editor" -> "Editor";
            case "content" -> "Content Defaults";
            case "install" -> "Install";
            default -> group == null ? "" : group;
        };
    }
}

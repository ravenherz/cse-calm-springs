package com.ravenherz.cse.scripting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

public final class ScriptTree {

    private ScriptTree() {
    }

    public static List<Branch> of(List<ScriptEntity> scripts, String selectedId) {
        Folder root = new Folder("");
        if (scripts != null) {
            for (ScriptEntity script : scripts) {
                if (script == null || script.getScriptId() == null || script.getScriptId().isBlank()) {
                    continue;
                }
                Folder cursor = root;
                for (String part : parts(script.getFolder())) {
                    cursor = cursor.child(part);
                }
                cursor.files.add(script);
            }
        }
        return root.branches(selectedId);
    }

    private static List<String> parts(String folder) {
        if (folder == null || folder.isBlank()) {
            return List.of();
        }
        List<String> parts = new ArrayList<>();
        for (String part : folder.split("/")) {
            if (!part.isBlank()) {
                parts.add(part);
            }
        }
        return parts;
    }

    public static final class Branch {
        private final String name;
        private final String entityId;
        private final boolean folder;
        private final boolean selected;
        private final List<Branch> children;

        private Branch(String name, String entityId, boolean folder, boolean selected, List<Branch> children) {
            this.name = name;
            this.entityId = entityId;
            this.folder = folder;
            this.selected = selected;
            this.children = children;
        }

        public String name() {
            return name;
        }

        public String entityId() {
            return entityId;
        }

        public boolean folder() {
            return folder;
        }

        public boolean selected() {
            return selected;
        }

        public List<Branch> children() {
            return children;
        }
    }

    private static final class Folder {
        private final String name;
        private final TreeMap<String, Folder> folders = new TreeMap<>();
        private final List<ScriptEntity> files = new ArrayList<>();

        private Folder(String name) {
            this.name = name;
        }

        private Folder child(String name) {
            return folders.computeIfAbsent(name, Folder::new);
        }

        private List<Branch> branches(String selectedId) {
            List<Branch> children = new ArrayList<>();
            for (Folder folder : folders.values()) {
                children.add(new Branch(folder.name, null, true, false, folder.branches(selectedId)));
            }
            files.sort(Comparator.comparing(ScriptEntity::getScriptId));
            for (ScriptEntity file : files) {
                String id = file.getId() == null ? "" : file.getId().toHexString();
                boolean selected = selectedId != null && !selectedId.isBlank() && selectedId.equals(id);
                children.add(new Branch(file.getScriptId(), id, false, selected, List.of()));
            }
            return children;
        }
    }
}

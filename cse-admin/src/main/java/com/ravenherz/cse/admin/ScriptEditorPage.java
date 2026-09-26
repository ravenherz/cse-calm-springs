package com.ravenherz.cse.admin;

import java.util.List;

/**
 * Scripts editor model. {@code form} is true when the right pane is editing or creating.
 */
public final class ScriptEditorPage {

    private final List<ScriptTreeNode> tree;
    private final String entityId;
    private final String scriptId;
    private final String folder;
    private final String source;
    private final String error;
    private final boolean form;

    public ScriptEditorPage(List<ScriptTreeNode> tree, String entityId, String scriptId, String folder,
            String source, String error, boolean form) {
        this.tree = tree == null ? List.of() : List.copyOf(tree);
        this.entityId = entityId == null ? "" : entityId;
        this.scriptId = scriptId == null ? "" : scriptId;
        this.folder = folder == null ? "" : folder;
        this.source = source == null ? "" : source;
        this.error = error == null ? "" : error;
        this.form = form;
    }

    public List<ScriptTreeNode> getTree() {
        return tree;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getScriptId() {
        return scriptId;
    }

    public String getFolder() {
        return folder;
    }

    public String getSource() {
        return source;
    }

    public String getError() {
        return error;
    }

    public boolean isForm() {
        return form;
    }
}

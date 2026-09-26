package com.ravenherz.cse.controller;

import com.ravenherz.cse.admin.ScriptDesk;
import com.ravenherz.cse.admin.ScriptEditorPage;
import com.ravenherz.cse.admin.ScriptRunRow;
import com.ravenherz.cse.admin.ScriptTreeNode;
import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.engine.scripting.OrphanSweep;
import com.ravenherz.cse.scripting.JsRuntime;
import com.ravenherz.cse.scripting.ScriptApi;
import com.ravenherz.cse.scripting.ScriptEntity;
import com.ravenherz.cse.scripting.ScriptRules;
import com.ravenherz.cse.scripting.ScriptRunEntity;
import com.ravenherz.cse.scripting.ScriptRunService;
import com.ravenherz.cse.scripting.ScriptRunStatus;
import com.ravenherz.cse.scripting.ScriptService;
import com.ravenherz.cse.scripting.ScriptTree;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ScriptDeskAdapter implements ScriptDesk {

    private static final DateTimeFormatter STARTED = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final ScriptService scripts;
    private final ScriptRunService runs;
    private final OrphanSweep sweep;

    public ScriptDeskAdapter(ScriptService scripts, ScriptRunService runs, OrphanSweep sweep) {
        this.scripts = scripts;
        this.runs = runs;
        this.sweep = sweep;
    }

    @Override
    public ScriptEditorPage open(String entityId, boolean creating) {
        ScriptEntity selected = creating ? null : load(entityId);
        boolean form = creating || selected != null;
        String id = selected == null || selected.getId() == null ? "" : selected.getId().toHexString();
        return page(id, form,
                selected == null ? "" : selected.getScriptId(),
                selected == null ? "" : selected.getFolder(),
                selected == null ? "" : selected.getSource(),
                "");
    }

    @Override
    public ScriptEditorPage save(String entityId, String scriptId, String folder, String source, EntityId actorId) {
        String idError = ScriptRules.idError(scriptId);
        if (idError != null) {
            return rejected(entityId, scriptId, folder, source, idError);
        }
        String folderError = ScriptRules.folderError(folder);
        if (folderError != null) {
            return rejected(entityId, scriptId, folder, source, folderError);
        }
        String sourceError = ScriptRules.sourceError(source);
        if (sourceError != null) {
            return rejected(entityId, scriptId, folder, source, sourceError);
        }
        String normalizedId = ScriptRules.normalizeId(scriptId);
        String normalizedFolder = ScriptRules.normalizeFolder(folder);
        ScriptEntity existing = load(entityId);
        if (entityId != null && !entityId.isBlank() && existing == null) {
            return rejected(entityId, scriptId, folder, source, "That script was not found");
        }
        ScriptEntity duplicate = scripts.findByScriptId(normalizedId);
        if (duplicate != null && (existing == null || existing.getId() == null
                || !existing.getId().equals(duplicate.getId()))) {
            return rejected(entityId, scriptId, folder, source, "A script with this id already exists");
        }
        ScriptEntity row = existing == null ? new ScriptEntity(normalizedId, normalizedFolder, source, actorId) : existing;
        if (existing == null) {
            row.setId(EntityId.generate());
        } else {
            row.setScriptId(normalizedId);
            row.setFolder(normalizedFolder);
            row.setSource(source == null ? "" : source);
        }
        if (existing == null) {
            scripts.insert(row);
        } else {
            scripts.replace(row);
        }
        String id = row.getId() == null ? "" : row.getId().toHexString();
        return page(id, true, row.getScriptId(), row.getFolder(), row.getSource(), "");
    }

    @Override
    public void delete(String entityId) {
        ScriptEntity row = load(entityId);
        if (row != null) {
            scripts.delete(row);
        }
    }

    @Override
    public String run(String entityId) {
        ScriptEntity script = load(entityId);
        if (script == null) {
            return "That script was not found";
        }
        ScriptRunEntity run = new ScriptRunEntity();
        run.setId(EntityId.generate());
        run.setScriptEntityId(script.getId());
        run.setScriptId(script.getScriptId());
        run.setStartedAt(System.currentTimeMillis());
        try {
            String report = JsRuntime.run(script.getSource(), new ScriptApi(Map.of(
                    "deleteOrphans", sweep::deleteOrphans)));
            run.setStatus(ScriptRunStatus.DONE);
            run.setMessage(report);
        } catch (RuntimeException ex) {
            run.setStatus(ScriptRunStatus.FAILED);
            run.setMessage(ex.getMessage() == null ? "Script failed" : ex.getMessage());
        }
        runs.insert(run);
        return null;
    }

    @Override
    public List<ScriptRunRow> runs() {
        List<ScriptRunRow> rows = new ArrayList<>();
        for (ScriptRunEntity run : runs.recent(50)) {
            if (run == null) {
                continue;
            }
            String started = run.getStartedAt() <= 0 ? "" : STARTED.format(Instant.ofEpochMilli(run.getStartedAt()));
            rows.add(new ScriptRunRow(run.getScriptId(), run.getStatus().label(), started, run.getMessage()));
        }
        return rows;
    }

    private ScriptEditorPage rejected(String entityId, String scriptId, String folder, String source, String error) {
        return page(entityId == null ? "" : entityId.trim(), true, scriptId, folder, source, error);
    }

    private ScriptEditorPage page(String selectedId, boolean form, String scriptId, String folder, String source,
            String error) {
        List<ScriptTreeNode> tree = new ArrayList<>();
        for (ScriptTree.Branch branch : ScriptTree.of(scripts.list(), selectedId)) {
            tree.add(node(branch));
        }
        return new ScriptEditorPage(tree, selectedId, scriptId, folder, source, error, form);
    }

    private static ScriptTreeNode node(ScriptTree.Branch branch) {
        List<ScriptTreeNode> children = new ArrayList<>();
        for (ScriptTree.Branch child : branch.children()) {
            children.add(node(child));
        }
        return new ScriptTreeNode(branch.name(), branch.entityId(), branch.folder(), branch.selected(), children);
    }

    private ScriptEntity load(String entityId) {
        if (entityId == null || entityId.isBlank()) {
            return null;
        }
        try {
            return scripts.find(EntityId.of(entityId.trim()));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

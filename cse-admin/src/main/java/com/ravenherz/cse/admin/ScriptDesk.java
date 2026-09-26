package com.ravenherz.cse.admin;

import com.ravenherz.cse.dal.EntityId;

import java.util.List;

/**
 * Script documents and run rows. The WAR stores them.
 */
public interface ScriptDesk {

    ScriptEditorPage open(String entityId, boolean creating);

    ScriptEditorPage save(String entityId, String scriptId, String folder, String source, EntityId actorId);

    void delete(String entityId);

    /**
     * @return an error when the script cannot be started, or null after the run is stored
     */
    String run(String entityId);

    List<ScriptRunRow> runs();
}

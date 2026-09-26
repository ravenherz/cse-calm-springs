package com.ravenherz.cse.scripting;

import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.dao.Store;

import java.util.List;

public interface ScriptService extends Store {

    List<ScriptEntity> list();

    ScriptEntity findByScriptId(String scriptId);

    ScriptEntity find(EntityId id);
}

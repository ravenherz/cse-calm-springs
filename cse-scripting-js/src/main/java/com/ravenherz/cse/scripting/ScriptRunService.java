package com.ravenherz.cse.scripting;

import com.ravenherz.cse.dal.dao.Store;

import java.util.List;

public interface ScriptRunService extends Store {

    List<ScriptRunEntity> recent(int limit);
}

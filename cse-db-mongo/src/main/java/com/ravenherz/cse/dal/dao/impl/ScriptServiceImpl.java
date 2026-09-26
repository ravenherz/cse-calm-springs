package com.ravenherz.cse.dal.dao.impl;

import com.ravenherz.cse.dal.DataProvider;
import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.dao.BasicService;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.scripting.ScriptEntity;
import com.ravenherz.cse.scripting.ScriptService;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository(value = "scriptService")
public class ScriptServiceImpl extends BasicService implements ScriptService {

    public ScriptServiceImpl(DataProvider dataProvider) {
        super(dataProvider);
    }

    @Override
    public List<BasicEntity> getAll() {
        return new ArrayList<>(list());
    }

    @Override
    public List<ScriptEntity> list() {
        return mongo().find(new Query().with(Sort.by("folder", "scriptId")), ScriptEntity.class);
    }

    @Override
    public ScriptEntity findByScriptId(String scriptId) {
        if (scriptId == null || scriptId.isBlank()) {
            return null;
        }
        return mongo().findOne(Query.query(Criteria.where("scriptId").is(scriptId)), ScriptEntity.class);
    }

    @Override
    public ScriptEntity find(EntityId id) {
        BasicEntity row = getById(ScriptEntity.class, id);
        return row instanceof ScriptEntity script ? script : null;
    }
}

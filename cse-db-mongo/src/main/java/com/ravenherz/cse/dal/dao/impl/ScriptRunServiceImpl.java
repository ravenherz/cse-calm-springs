package com.ravenherz.cse.dal.dao.impl;

import com.ravenherz.cse.dal.DataProvider;
import com.ravenherz.cse.dal.dao.BasicService;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.scripting.ScriptRunEntity;
import com.ravenherz.cse.scripting.ScriptRunService;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository(value = "scriptRunService")
public class ScriptRunServiceImpl extends BasicService implements ScriptRunService {

    public ScriptRunServiceImpl(DataProvider dataProvider) {
        super(dataProvider);
    }

    @Override
    public List<BasicEntity> getAll() {
        return new ArrayList<>(recent(200));
    }

    @Override
    public List<ScriptRunEntity> recent(int limit) {
        int size = limit < 1 ? 50 : Math.min(limit, 200);
        Query query = new Query()
                .with(Sort.by(Sort.Direction.DESC, "startedAt"))
                .limit(size);
        return mongo().find(query, ScriptRunEntity.class);
    }
}

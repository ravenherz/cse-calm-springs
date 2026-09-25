package com.ravenherz.cse.dal.dao.impl;

import com.ravenherz.cse.dal.DataProvider;
import com.ravenherz.cse.dal.dao.BasicService;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.redirect.ResourceRedirectEntity;
import com.ravenherz.cse.redirect.ResourceRedirectService;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository(value = "resourceRedirectService")
public class ResourceRedirectServiceImpl extends BasicService implements ResourceRedirectService {

    public ResourceRedirectServiceImpl(DataProvider dataProvider) {
        super(dataProvider);
    }

    @Override
    public List<BasicEntity> getAll() {
        return new ArrayList<>(mongo().findAll(ResourceRedirectEntity.class));
    }

    @Override
    public ResourceRedirectEntity getByFromPath(String fromPath) {
        if (fromPath == null || fromPath.isBlank()) {
            return null;
        }
        return mongo().findOne(Query.query(Criteria.where("fromPath").is(fromPath)),
                ResourceRedirectEntity.class);
    }
}

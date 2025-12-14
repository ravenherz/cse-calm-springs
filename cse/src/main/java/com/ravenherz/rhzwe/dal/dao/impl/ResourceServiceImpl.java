package com.ravenherz.rhzwe.dal.dao.impl;

import com.ravenherz.rhzwe.dal.dao.BasicService;
import com.ravenherz.rhzwe.dal.dao.ResourceService;
import com.ravenherz.rhzwe.dal.dto.ResourceEntity;
import dev.morphia.query.filters.Filters;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

@Lazy
@Repository(value = "resourceService")
@Scope(value = "singleton")
public class ResourceServiceImpl extends BasicService implements ResourceService {

    @Override
    public ResourceEntity getByPublicPath(String publicPath) {
        return dataProvider.getDatastore().find(ResourceEntity.class)
                .filter(Filters.eq("resourceData.pathPublic", publicPath))
                .first();
    }

}

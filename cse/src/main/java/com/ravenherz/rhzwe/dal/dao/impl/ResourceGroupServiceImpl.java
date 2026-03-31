package com.ravenherz.rhzwe.dal.dao.impl;

import com.ravenherz.rhzwe.dal.dao.BasicService;
import com.ravenherz.rhzwe.dal.dao.ResourceGroupService;
import com.ravenherz.rhzwe.dal.dto.BasicEntity;
import com.ravenherz.rhzwe.dal.dto.ResourceGroupEntity;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Lazy
@Repository(value = "resourceGroupService")
@Scope(value = "singleton")
public class ResourceGroupServiceImpl extends BasicService implements ResourceGroupService {

    @Override
    public List<BasicEntity> getAll() {
        return dataProvider.getDatastore().find(ResourceGroupEntity.class).stream().collect(Collectors.toList());
    }

    @Override
    public List<ResourceGroupEntity> getAllGroups() {
        return dataProvider.getDatastore().find(ResourceGroupEntity.class).stream().collect(Collectors.toList());
    }

    @Override
    public void delete(ResourceGroupEntity group) {
        if (group != null) {
            dataProvider.getDatastore().delete(group);
        }
    }
}
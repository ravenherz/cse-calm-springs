package com.ravenherz.cse.dal.dao.impl;

import com.ravenherz.cse.dal.DataProvider;
import com.ravenherz.cse.dal.dao.BasicService;
import com.ravenherz.cse.dal.dao.ResourceGroupService;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.ResourceGroupEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository(value = "resourceGroupService")
public class ResourceGroupServiceImpl extends BasicService implements ResourceGroupService {

    public ResourceGroupServiceImpl(DataProvider dataProvider) {
        super(dataProvider);
    }

    @Override
    public List<BasicEntity> getAll() {
        return new java.util.ArrayList<>(mongo().findAll(ResourceGroupEntity.class));
    }

    @Override
    public List<ResourceGroupEntity> getAllGroups() {
        return mongo().findAll(ResourceGroupEntity.class);
    }

    @Override
    public void delete(ResourceGroupEntity group) {
        if (group != null) {
            mongo().remove(group);
        }
    }
}

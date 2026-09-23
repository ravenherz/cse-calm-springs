package com.ravenherz.cse.dal.dao;

import com.ravenherz.cse.dal.dto.ResourceGroupEntity;

import java.util.List;

public interface ResourceGroupService extends Store {
    List<ResourceGroupEntity> getAllGroups();
    void delete(ResourceGroupEntity group);
}
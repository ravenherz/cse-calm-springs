package com.ravenherz.rhzwe.dal.dao;

import com.ravenherz.rhzwe.dal.dto.ResourceGroupEntity;

import java.util.List;

public interface ResourceGroupService extends Service {
    List<ResourceGroupEntity> getAllGroups();
    void delete(ResourceGroupEntity group);
}
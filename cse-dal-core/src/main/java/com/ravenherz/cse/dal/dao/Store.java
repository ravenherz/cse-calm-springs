package com.ravenherz.cse.dal.dao;

import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.dto.BasicEntity;

import java.util.List;

public interface Store {

    EntityId insert(BasicEntity entity);

    List<BasicEntity> getAll();

    BasicEntity getById(Class<? extends BasicEntity> entityClass, EntityId id);

    boolean replace(BasicEntity entity);

    void delete(BasicEntity entity);
}

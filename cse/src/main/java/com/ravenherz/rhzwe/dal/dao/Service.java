package com.ravenherz.rhzwe.dal.dao;

import com.ravenherz.rhzwe.dal.dto.BasicEntity;
import org.bson.types.ObjectId;

import java.util.List;

public interface Service {

    ObjectId insert (BasicEntity entity);

    List<BasicEntity> getAll ();

    BasicEntity getById (Class<? extends BasicEntity> entityClass, ObjectId objectId);

    boolean replace (BasicEntity entity);

    boolean remove (ObjectId id);
}

package com.ravenherz.cse.dal.dao;

import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.MongoIds;
import com.ravenherz.cse.dal.DataProvider;
import com.ravenherz.cse.dal.MongoRefs;
import com.ravenherz.cse.dal.dto.BasicEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

public abstract class BasicService implements Store {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasicService.class);

    protected final DataProvider dataProvider;

    protected BasicService(DataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    protected MongoTemplate mongo() {
        return dataProvider.getMongoTemplate();
    }

    public EntityId insert(BasicEntity entity) {
        try {
            MongoRefs.sync(entity);
            mongo().save(entity);
        } catch (DuplicateKeyException ex) {
            LOGGER.warn("Entity with PK already exists, skipping");
        }
        return entity.getId();
    }

    @Override
    public List<BasicEntity> getAll() {
        return null;
    }

    @Override
    public BasicEntity getById(Class<? extends BasicEntity> entityClass, EntityId id) {
        if (id == null) {
            return null;
        }
        return mongo().findById(MongoIds.toObjectId(id), entityClass);
    }

    @Override
    public void delete(BasicEntity entity) {
        if (entity != null && entity.getId() != null) {
            mongo().remove(entity);
        }
    }

    @Override
    public boolean replace(BasicEntity entity) {
        MongoRefs.sync(entity);
        mongo().save(entity);
        return true;
    }
}

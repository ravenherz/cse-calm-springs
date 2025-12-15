package com.ravenherz.rhzwe.dal.dao;

import com.mongodb.DuplicateKeyException;
import com.ravenherz.rhzwe.dal.DataProvider;
import com.ravenherz.rhzwe.dal.dto.BasicEntity;
import com.ravenherz.rhzwe.dal.dto.basic.Event;
import com.ravenherz.rhzwe.dal.dto.basic.HistoryData;
import dev.morphia.query.filters.Filters;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

@Lazy
@Component
@DependsOn("dataProvider")
public abstract class BasicService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasicService.class);

    protected DataProvider dataProvider;

    @Autowired @Lazy
    public void setDataProvider(DataProvider dataProviderImpl) {
        this.dataProvider = dataProviderImpl;
    }

    public ObjectId insert (BasicEntity entity) {
        try {
            dataProvider.getDatastore().save(entity);
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
    public BasicEntity getById(Class<? extends BasicEntity> entityClass, ObjectId objectId) {
        return dataProvider.getDatastore().find(entityClass).filter(Filters.eq("id", objectId)).first();
    }

    @Override
    public boolean replace(BasicEntity entity) {
        BasicEntity t = dataProvider.getDatastore().replace(entity);
        return true; // todo rework
    }

    @Override
    public boolean remove(ObjectId id) {
        return false;
    }
}

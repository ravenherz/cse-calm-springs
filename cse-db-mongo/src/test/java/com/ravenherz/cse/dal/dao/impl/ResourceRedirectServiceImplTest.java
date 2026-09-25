package com.ravenherz.cse.dal.dao.impl;

import com.ravenherz.cse.dal.DataProvider;
import com.ravenherz.cse.redirect.ResourceRedirectEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceRedirectServiceImplTest {

    private MongoTemplate mongo;
    private ResourceRedirectServiceImpl store;

    @BeforeEach
    void stubs() {
        DataProvider data = mock(DataProvider.class);
        mongo = mock(MongoTemplate.class);
        when(data.getMongoTemplate()).thenReturn(mongo);
        store = new ResourceRedirectServiceImpl(data);
    }

    @Test
    void blankSourceDoesNotQuery() {
        assertNull(store.getByFromPath(" "));
        verify(mongo, never()).findOne(any(Query.class), eq(ResourceRedirectEntity.class));
    }

    @Test
    void sourcePathQueriesFromPath() {
        ResourceRedirectEntity row = new ResourceRedirectEntity();
        when(mongo.findOne(any(Query.class), eq(ResourceRedirectEntity.class))).thenReturn(row);

        store.getByFromPath("/old-post");

        verify(mongo).findOne(any(Query.class), eq(ResourceRedirectEntity.class));
    }
}

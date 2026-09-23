package com.ravenherz.cse.dal;

import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Settings overlay that lives in the DBMS. {@link MongoTemplate} on this port
 * is temporary — strip it when Settings persist through an engine-neutral store.
 */
public interface SettingsDocuments {

    void bindMongoTemplate(MongoTemplate mongoTemplate);
}

package com.ravenherz.cse.dal;

import org.springframework.data.mongodb.core.MongoTemplate;

public interface DataProvider {

    MongoTemplate getMongoTemplate();

    boolean ping();

    boolean usesEnvironmentCredentials();

    boolean hasFormCredentials();

    void useFormCredentials(String type, String address, String port, String dbname,
            String user, String password);
}

package com.ravenherz.rhzwe.dal.impl;

import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.ravenherz.rhzwe.constants.Strings;
import com.ravenherz.rhzwe.dal.DataProvider;
import com.ravenherz.rhzwe.util.Settings;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Lazy
@Service("dataProvider")
@Scope(value = "singleton")
@DependsOn(value = {"settings"})
public class DataProviderImpl implements DataProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataProviderImpl.class);

    private static
    Settings settings;

    private static Datastore datastore;

    @Autowired
    public void setSettings(Settings settingsImpl) {
        settings = settingsImpl;
    }

    @PostConstruct
    public void dataProviderImpl() {
        String type = settings.getValue(Strings.CONTEXT_DATASOURCE_DBMS_INSTANCE, Strings.KEY_DBMS_TYPE);
        String username = settings
                .getValue(Strings.CONTEXT_DATASOURCE_DBMS_ACCESS, Strings.KEY_DBMS_ACCESS_USER);
        String password = settings
                .getValue(Strings.CONTEXT_DATASOURCE_DBMS_ACCESS, Strings.KEY_DBMS_ACCESS_PSWD);
        String address = settings
                .getValue(Strings.CONTEXT_DATASOURCE_DBMS_INSTANCE, Strings.KEY_DBMS_ADDRESS);
        String port = settings.getValue(Strings.CONTEXT_DATASOURCE_DBMS_INSTANCE, Strings.KEY_DBMS_PORT);
        String dbname = settings.getValue(Strings.CONTEXT_DATASOURCE_DBMS_INSTANCE, Strings.KEY_DBMS_DBNAME);
        MongoClientURI uri = new MongoClientURI(
                type + "://" + username + ":" + password + "@" + address + ":" + port
                        + "/?authSource=" + dbname);
        MongoClient mongoClient = MongoClients.create(uri.toString());
        datastore = Morphia.createDatastore(mongoClient, dbname);
    }

    @Override
    public Datastore getDatastore() {
        return datastore;
    }
}

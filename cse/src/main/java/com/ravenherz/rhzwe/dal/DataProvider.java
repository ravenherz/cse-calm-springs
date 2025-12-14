package com.ravenherz.rhzwe.dal;

import dev.morphia.Datastore;

public interface DataProvider {

    public Datastore getDatastore();
}

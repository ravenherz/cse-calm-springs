package com.ravenherz.cse.dal.business;

import com.ravenherz.cse.dal.ServiceProvider;

public abstract class Dealer {

    protected final ServiceProvider serviceProvider;

    protected Dealer(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
    }
}

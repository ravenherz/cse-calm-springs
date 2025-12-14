package com.ravenherz.rhzwe.dal.business;

import com.ravenherz.rhzwe.dal.ServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;

@DependsOn({"serviceProvider"})
public abstract class Dealer {

    protected ServiceProvider serviceProvider;

    @Autowired
    @Lazy
    public void setServiceProvider(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
    }
}

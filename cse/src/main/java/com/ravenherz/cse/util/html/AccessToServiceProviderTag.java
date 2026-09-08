package com.ravenherz.cse.util.html;

import com.ravenherz.cse.dal.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AccessToServiceProviderTag extends AccessToSettingsTag {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessToServiceProviderTag.class);

    protected ServiceProvider serviceProvider;

    @Autowired
    public void setServiceProvider(ServiceProvider serviceProviderImpl) {
        LOGGER.debug("tried to set " + serviceProviderImpl);
        serviceProvider = serviceProviderImpl;
    }
}

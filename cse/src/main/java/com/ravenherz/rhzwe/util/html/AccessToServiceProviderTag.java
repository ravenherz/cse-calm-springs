package com.ravenherz.rhzwe.util.html;

import com.ravenherz.rhzwe.dal.ServiceProvider;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@DependsOn(value = "serviceProvider")
public abstract class AccessToServiceProviderTag extends AccessToSettingsTag {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccessToServiceProviderTag.class);
    private static ServiceProvider serviceProvider;

    @Autowired @Lazy
    public void setServiceProvider(ServiceProvider serviceProviderImpl) {
        LOGGER.debug("tried to set " + serviceProviderImpl);
        serviceProvider = serviceProviderImpl;
    }

    public static void init(ServiceProvider serviceProviderImpl) {
        serviceProvider = serviceProviderImpl;
    }

    public static ServiceProvider getServiceProvider() {
        return serviceProvider;
    }
}

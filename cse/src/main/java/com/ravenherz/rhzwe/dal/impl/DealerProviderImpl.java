package com.ravenherz.rhzwe.dal.impl;

import com.ravenherz.rhzwe.dal.DealerProvider;
import com.ravenherz.rhzwe.dal.business.NavigationDealer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Lazy
@Service(value = "dealerProvider")
@Scope(value = "singleton")
@DependsOn({"navigationDealer"})
public class DealerProviderImpl implements DealerProvider {

    private static NavigationDealer navigationDealer;

    @Autowired @Lazy
    public void setNavigationDealer(NavigationDealer navigationDealerImpl) {
        navigationDealer = navigationDealerImpl;
    }

    @Override
    public NavigationDealer getNavigationDealer() {
        return navigationDealer;
    }
}

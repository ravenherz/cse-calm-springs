package com.ravenherz.cse.dal.impl;

import com.ravenherz.cse.dal.DealerProvider;
import com.ravenherz.cse.dal.business.NavigationDealer;
import org.springframework.stereotype.Service;

@Service(value = "dealerProvider")
public class DealerProviderImpl implements DealerProvider {

    private final NavigationDealer navigationDealer;

    public DealerProviderImpl(NavigationDealer navigationDealer) {
        this.navigationDealer = navigationDealer;
    }

    @Override
    public NavigationDealer getNavigationDealer() {
        return navigationDealer;
    }
}

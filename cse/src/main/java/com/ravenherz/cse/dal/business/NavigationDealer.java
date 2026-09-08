package com.ravenherz.cse.dal.business;

import com.ravenherz.cse.dal.business.basic.Navigation;
import com.ravenherz.cse.dal.dto.AccountEntity;

public interface NavigationDealer {

    Navigation getNavigation(AccountEntity accessor);
}

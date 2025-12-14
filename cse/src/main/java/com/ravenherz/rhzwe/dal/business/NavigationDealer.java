package com.ravenherz.rhzwe.dal.business;

import com.ravenherz.rhzwe.dal.business.basic.Navigation;
import com.ravenherz.rhzwe.dal.dto.AccountEntity;

public interface NavigationDealer {

    Navigation getNavigation(AccountEntity accessor);
}

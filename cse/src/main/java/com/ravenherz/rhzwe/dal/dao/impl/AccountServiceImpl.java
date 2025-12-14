package com.ravenherz.rhzwe.dal.dao.impl;

import com.ravenherz.rhzwe.dal.dao.AccountService;
import com.ravenherz.rhzwe.dal.dao.BasicService;
import com.ravenherz.rhzwe.dal.dto.AccountEntity;
import dev.morphia.query.filters.Filters;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

@Lazy
@Repository(value = "accountService")
@Scope(value = "singleton")
public class AccountServiceImpl extends BasicService implements AccountService {

    @Override
    public AccountEntity getByLogin(String login) {
        return dataProvider.getDatastore().find(AccountEntity.class)
                .filter(Filters.eq("accountData.login", login))
                .first();
    }

    @Override
    public AccountEntity getByEmail(String email) {
        return dataProvider.getDatastore().find(AccountEntity.class)
                .filter(Filters.eq("accountData.emailAddress", email))
                .first();
    }
}

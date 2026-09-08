package com.ravenherz.cse.dal.dao.impl;

import com.ravenherz.cse.dal.DataProvider;
import com.ravenherz.cse.dal.dao.AccountService;
import com.ravenherz.cse.dal.dao.BasicService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.BasicEntity;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository(value = "accountService")
public class AccountServiceImpl extends BasicService implements AccountService {

    public AccountServiceImpl(DataProvider dataProvider) {
        super(dataProvider);
    }

    @Override
    public AccountEntity getByLogin(String login) {
        return mongo().findOne(Query.query(Criteria.where("accountData.login").is(login)), AccountEntity.class);
    }

    @Override
    public AccountEntity getByEmail(String email) {
        return mongo().findOne(Query.query(Criteria.where("accountData.emailAddress").is(email)),
                AccountEntity.class);
    }

    @Override
    public List<BasicEntity> getAll() {
        return new java.util.ArrayList<>(mongo().findAll(AccountEntity.class));
    }

    @Override
    public void delete(AccountEntity account) {
        if (account != null) {
            mongo().remove(account);
        }
    }
}

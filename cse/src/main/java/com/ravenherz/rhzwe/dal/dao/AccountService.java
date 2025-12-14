package com.ravenherz.rhzwe.dal.dao;

import com.ravenherz.rhzwe.dal.dto.AccountEntity;

public interface AccountService extends Service{
    AccountEntity getByLogin(String login);

    AccountEntity getByEmail(String email);
}

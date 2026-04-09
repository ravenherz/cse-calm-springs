package com.ravenherz.rhzwe.dal.dao;

import com.ravenherz.rhzwe.dal.dto.AccountEntity;

import java.util.List;

public interface AccountService extends Service {
    AccountEntity getByLogin(String login);

    AccountEntity getByEmail(String email);

    void delete(AccountEntity account);

    default List<AccountEntity> getAllAccounts() {
        return getAll().stream()
                .map(e -> (AccountEntity) e)
                .collect(java.util.stream.Collectors.toList());
    }
}

package com.ravenherz.cse.dal.dao;

import com.ravenherz.cse.dal.dto.AccountEntity;

import java.util.List;

public interface AccountService extends Store {
    AccountEntity getByLogin(String login);

    AccountEntity getByEmail(String email);

    void delete(AccountEntity account);

    default List<AccountEntity> getAllAccounts() {
        return getAll().stream()
                .map(e -> (AccountEntity) e)
                .collect(java.util.stream.Collectors.toList());
    }
}

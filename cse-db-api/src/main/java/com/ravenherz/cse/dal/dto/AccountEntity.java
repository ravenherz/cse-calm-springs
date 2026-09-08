package com.ravenherz.cse.dal.dto;

import com.ravenherz.cse.constants.MongoCollections;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Objects;

@Document(collection = MongoCollections.DATABASE_ACCOUNTS)
public final class AccountEntity extends BasicEntity {

    private AccountData accountData;

    public AccountEntity() {
    }

    public AccountEntity(AccountData accountData) {
        super("0.1.0");
        this.accountData = accountData;
    }

    public AccountData getAccountData() {
        return accountData;
    }

    public void setAccountData(AccountData accountData) {
        this.accountData = accountData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AccountEntity that)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        return Objects.equals(accountData, that.accountData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), accountData);
    }

}

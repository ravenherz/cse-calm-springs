package com.ravenherz.cse.dal.dto;

import com.ravenherz.cse.constants.MongoCollections;
import com.ravenherz.cse.dal.AccessActor;
import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Objects;

@Document(collection = MongoCollections.DATABASE_ACCOUNTS)
public final class AccountEntity extends BasicEntity implements AccessActor {

    private AccountData accountData;

    public AccountEntity() {
    }

    public AccountEntity(AccountData accountData) {
        super(null, null);
        this.accountData = accountData;
    }

    public AccountData getAccountData() {
        return accountData;
    }

    @Override
    public EntityId id() {
        return getId();
    }

    @Override
    public String roleId() {
        return accountData == null ? null : accountData.getRoleId();
    }

    @Override
    public SecurityLevel level() {
        return accountData == null ? null : accountData.getLevel();
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

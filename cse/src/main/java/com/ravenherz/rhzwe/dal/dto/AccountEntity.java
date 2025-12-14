package com.ravenherz.rhzwe.dal.dto;

import com.ravenherz.rhzwe.constants.Strings;
import com.ravenherz.rhzwe.dal.dto.basic.AccountData;
import dev.morphia.annotations.Entity;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

@Entity(Strings.DATABASE_ACCOUNTS)
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
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

}

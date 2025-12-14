package com.ravenherz.rhzwe.util.html;

import com.ravenherz.rhzwe.dal.dto.AccountEntity;

public interface CustomAccessHtmlTag {
    HTMLElement getHtmlElement(AccountEntity accountEntity);
}

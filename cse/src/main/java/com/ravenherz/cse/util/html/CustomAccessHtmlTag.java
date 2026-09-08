package com.ravenherz.cse.util.html;

import com.ravenherz.cse.dal.dto.AccountEntity;

public interface CustomAccessHtmlTag {
    HTMLElement getHtmlElement(AccountEntity accountEntity);
}

package com.ravenherz.cse.security;

import com.ravenherz.cse.dal.dto.AccountEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Cookie session lookup. {@code AuthSupport} in the WAR is the implementation.
 */
public interface AccountAccessor {

    AccountEntity getAccessor(HttpServletRequest request, HttpServletResponse response);
}

package com.ravenherz.cse.redirect;

import com.ravenherz.cse.dal.dao.Store;

public interface ResourceRedirectService extends Store {

    ResourceRedirectEntity getByFromPath(String fromPath);
}

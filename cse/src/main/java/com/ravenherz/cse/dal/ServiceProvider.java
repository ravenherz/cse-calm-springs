package com.ravenherz.cse.dal;

import com.ravenherz.cse.redirect.ResourceRedirectService;
import com.ravenherz.cse.transfer.SiteServices;

public interface ServiceProvider extends SiteServices {

    ResourceRedirectService getResourceRedirectService();
}

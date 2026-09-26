package com.ravenherz.cse.dal;

import com.ravenherz.cse.redirect.ResourceRedirectService;
import com.ravenherz.cse.transfer.SiteServices;
import com.ravenherz.cse.util.video.VideoTranscodeService;

public interface ServiceProvider extends SiteServices {

    ResourceRedirectService getResourceRedirectService();

    VideoTranscodeService getVideoTranscodeService();
}

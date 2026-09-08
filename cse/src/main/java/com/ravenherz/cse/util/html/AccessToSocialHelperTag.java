package com.ravenherz.cse.util.html;

import com.ravenherz.cse.util.helpers.SocialNetworkHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;

@DependsOn(value = "socialHelper")
public abstract class AccessToSocialHelperTag extends AccessToSettingsTag {

    protected static SocialNetworkHelper socialNetworkHelper;

    @Autowired @Lazy
    public void setSocialHelper(SocialNetworkHelper socialNetworkHelperImpl) {
        socialNetworkHelper = socialNetworkHelperImpl;
    }
}

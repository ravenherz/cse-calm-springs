package com.ravenherz.rhzwe.util.pluggable.extensions;

import com.ravenherz.rhzwe.constants.Strings;
import com.ravenherz.rhzwe.util.pluggable.Validatable;
import com.ravenherz.rhzwe.util.pluggable.basic.LoaderInformation;

import java.io.Serializable;

public class SocialNetworkDefinition implements Serializable, Validatable {

    private LoaderInformation loader = null;
    private String tooltipText = null;
    private String address = null;
    private String protocol = null;
    private String logo = Strings.PATH_SOCIAL_LOGO_DEFAULT;

    public LoaderInformation getLoader() {
        return loader;
    }

    public void setLoader(LoaderInformation loader) {
        this.loader = loader;
    }

    public String getTooltipText() {
        return tooltipText;
    }

    public void setTooltipText(String tooltipText) {
        this.tooltipText = tooltipText;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    @Override
    public boolean isValid() {
//        return !someIsNull((Object[]) ReflectionUtils.getDeclaredAndInheritedFields(this.getClass(),
//                true));
        return true;
    }
}

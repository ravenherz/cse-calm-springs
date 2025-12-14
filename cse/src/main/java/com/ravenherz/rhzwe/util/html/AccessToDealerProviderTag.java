package com.ravenherz.rhzwe.util.html;

import com.ravenherz.rhzwe.dal.DealerProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;

@DependsOn(value = "dealerProvider")
public abstract class AccessToDealerProviderTag extends AccessToSettingsTag {

    protected static DealerProvider dealerProvider;

    @Autowired @Lazy
    public void setDealerProvider(DealerProvider dealerProviderImpl) {
        dealerProvider = dealerProviderImpl;
    }

    public static void init(DealerProvider dealerProviderImpl) {
        dealerProvider = dealerProviderImpl;
    }
}

package com.ravenherz.cse.engine.apps;

import com.ravenherz.cse.util.frontend.ShippedPackCatalog;

import com.ravenherz.cse.security.ShippedAppStems;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ShippedAppStemSource implements ShippedAppStems {

    @Override
    public Iterable<String> stems() {
        List<String> stems = new ArrayList<>();
        for (ShippedPackCatalog.Pack pack : ShippedPackCatalog.apps()) {
            if (pack != null && pack.stem() != null && !pack.stem().isBlank()) {
                stems.add(pack.stem());
            }
        }
        return stems;
    }
}

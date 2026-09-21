package com.ravenherz.cse.engine.themes;

import com.ravenherz.cse.util.themes.ShippedThemes;

import com.ravenherz.cse.util.frontend.ShippedPackCatalog;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ClasspathShippedThemes implements ShippedThemes {

    @Override
    public List<Pack> packs() {
        List<Pack> out = new ArrayList<>();
        for (ShippedPackCatalog.Pack pack : ShippedPackCatalog.themes()) {
            out.add(new Pack(pack.stem(), pack.bytes()));
        }
        return out;
    }
}

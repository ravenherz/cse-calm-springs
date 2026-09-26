package com.ravenherz.cse.engine.scripting;

import com.ravenherz.cse.present.ResourceGroupIndex;
import com.ravenherz.cse.scripting.ScriptChanged;
import org.springframework.stereotype.Component;

@Component
class ScriptCatalogRefresh implements ScriptChanged {

    private final ResourceGroupIndex index;

    ScriptCatalogRefresh(ResourceGroupIndex index) {
        this.index = index;
    }

    @Override
    public void scriptsChanged() {
        index.contentChanged();
    }
}

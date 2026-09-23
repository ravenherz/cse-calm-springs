package com.ravenherz.cse.engine.apps;

import com.ravenherz.cse.dal.dao.AppService;
import com.ravenherz.cse.dal.dto.AppEntity;
import com.ravenherz.cse.dal.dto.basic.AppData;
import com.ravenherz.cse.security.AppSlugSource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class InstalledAppSlugSource implements AppSlugSource {

    private final AppService apps;

    public InstalledAppSlugSource(AppService apps) {
        this.apps = apps;
    }

    @Override
    public List<String> slugs() {
        List<String> slugs = new ArrayList<>();
        if (apps == null) {
            return slugs;
        }
        for (var entity : apps.getAll()) {
            if (entity instanceof AppEntity app && app.getAppData() != null) {
                AppData data = app.getAppData();
                if (data.getSlug() != null && !data.getSlug().isBlank()) {
                    slugs.add(data.getSlug());
                }
            }
        }
        return slugs;
    }
}

package com.ravenherz.cse.engine.scripting;

import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.scripting.DeleteOrphansScript;
import com.ravenherz.cse.scripting.ScriptEntity;
import com.ravenherz.cse.scripting.ScriptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ScriptSeeds implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptSeeds.class);

    private final ScriptService scripts;

    public ScriptSeeds(ScriptService scripts) {
        this.scripts = scripts;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (scripts.findByScriptId(DeleteOrphansScript.ID) != null) {
                return;
            }
            ScriptEntity row = new ScriptEntity(DeleteOrphansScript.ID, DeleteOrphansScript.FOLDER,
                    DeleteOrphansScript.SOURCE, null);
            row.setId(EntityId.generate());
            scripts.insert(row);
            LOGGER.info("Seeded script {}", DeleteOrphansScript.ID);
        } catch (RuntimeException ex) {
            LOGGER.warn("Could not seed script {}: {}", DeleteOrphansScript.ID, ex.getMessage());
        }
    }
}

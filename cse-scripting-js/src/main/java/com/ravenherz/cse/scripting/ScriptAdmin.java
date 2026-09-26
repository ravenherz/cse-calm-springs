package com.ravenherz.cse.scripting;

import com.ravenherz.cse.core.admin.AdminField;
import com.ravenherz.cse.core.admin.AdminPresentation;
import com.ravenherz.cse.core.admin.AdminSection;
import com.ravenherz.cse.core.admin.AdminSectionSource;
import com.ravenherz.cse.core.admin.CardPlace;
import com.ravenherz.cse.core.admin.FieldType;
import com.ravenherz.cse.core.admin.TreeGlyph;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class ScriptAdmin implements AdminSectionSource {

    public static final String SECTION_ID = "scripts";

    @Override
    public AdminSection section() {
        return new AdminSection(SECTION_ID, "Scripts", List.of(
                new AdminField("scriptId", "Id", FieldType.TEXT, true, List.of(), CardPlace.SOURCE),
                new AdminField("folder", "Folder", FieldType.TEXT, false, List.of(), CardPlace.TARGET),
                new AdminField("source", "Source", FieldType.MULTILINE, false, List.of())),
                new AdminPresentation("{scriptId}", TreeGlyph.JS),
                "/editor/scripting/runs",
                "/editor/scripting/run");
    }
}

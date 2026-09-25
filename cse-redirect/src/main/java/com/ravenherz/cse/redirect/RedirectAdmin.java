package com.ravenherz.cse.redirect;

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
public final class RedirectAdmin implements AdminSectionSource {

    public static final String SECTION_ID = "redirects";

    @Override
    public AdminSection section() {
        return new AdminSection(SECTION_ID, "Redirects", List.of(
                new AdminField("fromPath", "From", FieldType.PATH, true, List.of(), CardPlace.SOURCE),
                new AdminField("targetPath", "Target", FieldType.PATH, true, List.of(), CardPlace.TARGET),
                new AdminField("targetEntityId", "Target document", FieldType.ENTITY_ID, false, List.of()),
                new AdminField("enabled", "Enabled", FieldType.BOOLEAN, true, List.of()),
                new AdminField("status", "Status", FieldType.ENUM, true, List.of("301", "302"), CardPlace.CODE),
                new AdminField("preserveQuery", "Preserve query", FieldType.BOOLEAN, false, List.of())),
                new AdminPresentation("{status} {fromPath} to {targetPath}", TreeGlyph.ARROW));
    }
}

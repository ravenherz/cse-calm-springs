package com.ravenherz.cse.core.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminSectionTest {

    @Test
    void sectionExposesFields() {
        AdminField status = new AdminField("status", "Status", FieldType.ENUM, true, List.of("301", "302"));
        AdminSection section = new AdminSection("redirects", "Redirects", List.of(status));

        assertEquals("redirects", section.id());
        assertEquals("status", section.fields().get(0).name());
        assertEquals(List.of("301", "302"), section.fields().get(0).options());
    }

    @Test
    void enumFieldRequiresOptions() {
        assertThrows(IllegalArgumentException.class,
                () -> new AdminField("status", "Status", FieldType.ENUM, true, List.of()));
    }

    @Test
    void sourceReturnsItsSection() {
        AdminSection section = new AdminSection("redirects", "Redirects", List.of(
                new AdminField("fromPath", "From", FieldType.PATH, true, List.of())));
        AdminSectionSource source = () -> section;

        assertEquals(section, source.section());
    }
}

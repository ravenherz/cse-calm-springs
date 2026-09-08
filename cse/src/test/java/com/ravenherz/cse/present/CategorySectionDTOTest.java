package com.ravenherz.cse.present;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CategorySectionDTOTest {

    @Test
    void slugifyNormalizesAndFallsBack() {
        assertEquals("section", CategorySectionDTO.slugify(null));
        assertEquals("section", CategorySectionDTO.slugify("   "));
        assertEquals("section", CategorySectionDTO.slugify("---"));
        assertEquals("music-releases", CategorySectionDTO.slugify("Music Releases"));
        assertEquals("hello-world", CategorySectionDTO.slugify("Hello--World!!"));
        assertEquals("photo", CategorySectionDTO.slugify("photo"));
        assertEquals("section", CategorySectionDTO.slugify("Å"));
    }
}

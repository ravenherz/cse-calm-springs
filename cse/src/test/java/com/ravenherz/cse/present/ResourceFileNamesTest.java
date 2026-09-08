package com.ravenherz.cse.present;

import com.ravenherz.cse.dal.dto.basic.enums.ResourceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ResourceFileNamesTest {

    @Test
    void keepsDirectoryAndExtensionWhenNameHasNone() {
        assertEquals("/u/res/image/dog.jpg",
                ResourceFileNames.nextPathPublic("/u/res/image/cat.jpg", "dog", ResourceType.IMAGE));
    }

    @Test
    void acceptsMatchingExtensionChange() {
        assertEquals("/u/res/image/dog.png",
                ResourceFileNames.nextPathPublic("/u/res/image/cat.jpg", "dog.png", ResourceType.IMAGE));
    }

    @Test
    void rejectsTypeMismatchAndPathTricks() {
        assertNull(ResourceFileNames.nextPathPublic("/u/res/image/cat.jpg", "song.mp3", ResourceType.IMAGE));
        assertNull(ResourceFileNames.nextPathPublic("/u/res/image/cat.jpg", "a/b.jpg", ResourceType.IMAGE));
        assertNull(ResourceFileNames.nextPathPublic("/u/res/image/cat.jpg", "..", ResourceType.IMAGE));
        assertNull(ResourceFileNames.nextPathPublic("/u/res/image/cat.jpg", "", ResourceType.IMAGE));
        assertNull(ResourceFileNames.nextPathPublic("/u/res/image/cat.jpg", "nope.txt", ResourceType.IMAGE));
    }
}

package com.ravenherz.cse.present;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountDirectoryTest {

    @Test
    void loginMatchesIsCaseInsensitiveAndIgnoresBlankQuery() {
        assertTrue(AccountDirectory.loginMatches("ravenherz", null));
        assertTrue(AccountDirectory.loginMatches("ravenherz", "  "));
        assertTrue(AccountDirectory.loginMatches("ravenherz", "Raven"));
        assertTrue(AccountDirectory.loginMatches("ravenherz", "herz"));
        assertFalse(AccountDirectory.loginMatches("ravenherz", "root"));
        assertFalse(AccountDirectory.loginMatches(null, "r"));
    }

    @Test
    void initialUsesFirstLetter() {
        assertEquals("R", AccountDirectory.initial("ravenherz"));
        assertEquals("?", AccountDirectory.initial(""));
        assertEquals("?", AccountDirectory.initial(null));
    }

    @Test
    void safeAvatarKeepsImageDataUrlsOnly() {
        assertEquals("data:image/jpeg;base64,abc",
                AccountDirectory.safeAvatar("data:image/jpeg;base64,abc"));
        assertNull(AccountDirectory.safeAvatar("javascript:alert(1)"));
        assertNull(AccountDirectory.safeAvatar(""));
    }
}

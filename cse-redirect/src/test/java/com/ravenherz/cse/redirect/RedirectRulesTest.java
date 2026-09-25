package com.ravenherz.cse.redirect;

import com.ravenherz.cse.core.route.PathTarget;
import com.ravenherz.cse.dal.EntityId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedirectRulesTest {

    @Test
    void normalizesAndAcceptsAPageTarget() {
        ResourceRedirectEntity row = row("/old-post/", "/?page=new-post", 301, true);

        List<RedirectFieldError> errors = RedirectRules.check(row, List.of());

        assertTrue(errors.isEmpty());
        assertEquals("/old-post", row.getFromPath());
        assertEquals("/?page=new-post", row.getTargetPath());
    }

    @Test
    void rejectsReservedSourceAndOpenRedirect() {
        ResourceRedirectEntity reserved = row("/editor/roles", "/?page=home", 301, true);
        ResourceRedirectEntity open = row("/old", "//evil.example", 301, true);

        assertEquals("fromPath", RedirectRules.check(reserved, List.of()).get(0).field());
        assertEquals("targetPath", RedirectRules.check(open, List.of()).get(0).field());
    }

    @Test
    void rejectsDuplicateAndLoop() {
        MemoryRedirectStore store = new MemoryRedirectStore();
        ResourceRedirectEntity existing = row("/old", "/newer", 301, true);
        store.insert(existing);
        ResourceRedirectEntity back = row("/newer", "/old", 302, true);
        store.insert(back);

        ResourceRedirectEntity duplicate = row("/old/", "/?page=other", 301, true);
        assertEquals("fromPath", RedirectRules.check(duplicate, List.of(existing)).get(0).field());

        ResourceRedirectEntity loop = row("/old", "/newer", 301, true);
        loop.setId(existing.getId());
        assertEquals("targetPath", RedirectRules.check(loop, List.of(existing, back)).get(0).field());
        assertEquals(existing.getId(), store.getByFromPath("/old/").getId());
    }

    @Test
    void disabledRowSkipsTheLoopCheck() {
        ResourceRedirectEntity other = row("/newer", "/old", 301, true);
        ResourceRedirectEntity row = row("/old", "/newer", 301, false);

        assertTrue(RedirectRules.check(row, List.of(other)).isEmpty());
    }

    private static ResourceRedirectEntity row(String from, String target, int status, boolean enabled) {
        return new ResourceRedirectEntity(from, target, EntityId.generate(), enabled, status, false, null);
    }
}

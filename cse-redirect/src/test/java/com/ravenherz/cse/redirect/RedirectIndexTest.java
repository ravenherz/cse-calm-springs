package com.ravenherz.cse.redirect;

import com.ravenherz.cse.core.admin.FieldType;
import com.ravenherz.cse.core.route.PathTarget;
import com.ravenherz.cse.dal.EntityId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedirectIndexTest {

    @Test
    void loadsEnabledRowsFromTheStore() {
        MemoryRedirectStore store = new MemoryRedirectStore();
        ResourceRedirectEntity live = new ResourceRedirectEntity(
                "/old-post/", "/?page=new-post", EntityId.generate(), true, 301, true, null);
        ResourceRedirectEntity off = new ResourceRedirectEntity(
                "/hidden", "/?page=hidden", null, false, 302, false, null);
        store.insert(live);
        store.insert(off);
        store.insert(new ResourceRedirectEntity("/rhz-we/*", "/*", null, true, 301, true, null));
        store.insert(new ResourceRedirectEntity("/static-pages/*", "/apps/*", null, true, 301, true, null));

        RedirectIndex index = new RedirectIndex();
        index.load(store);

        PathTarget target = index.resolve("/old-post").orElseThrow();
        assertEquals("/?page=new-post", target.targetPath());
        assertEquals(301, target.status());
        assertTrue(target.preserveQuery());
        assertTrue(index.resolve("/hidden").isEmpty());
        assertTrue(index.resolve("/editor").isEmpty());
        PathTarget legacy = index.resolve("/rhz-we/editor/resources").orElseThrow();
        assertEquals("/editor/resources", legacy.targetPath());
        assertEquals("/", index.resolve("/rhz-we").orElseThrow().targetPath());
        assertEquals("/apps/login", index.resolve("/static-pages/login").orElseThrow().targetPath());
        assertEquals("redirects", new RedirectAdmin().section().id());
        assertEquals(FieldType.ENUM, new RedirectAdmin().section().fields().get(4).type());
    }
}

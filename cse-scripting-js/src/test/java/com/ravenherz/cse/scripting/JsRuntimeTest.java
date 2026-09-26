package com.ravenherz.cse.scripting;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsRuntimeTest {

    @Test
    void deleteOrphansScriptCallsTheHost() {
        AtomicInteger calls = new AtomicInteger();
        String report = JsRuntime.run(DeleteOrphansScript.SOURCE, new ScriptApi(Map.of(
                "deleteOrphans", () -> {
                    calls.incrementAndGet();
                    return "resources 2, tracks 0, images 0, album-groups 0, transcodes 1";
                })));
        assertEquals(1, calls.get());
        assertTrue(report.startsWith("resources 2"));
    }
}

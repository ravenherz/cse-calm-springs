package com.ravenherz.cse.scripting;

import org.graalvm.polyglot.HostAccess;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Host object bound as {@code cse} inside a script.
 */
public final class ScriptApi {

    private final Map<String, Supplier<String>> calls;

    public ScriptApi(Map<String, Supplier<String>> calls) {
        this.calls = calls == null ? Map.of() : Map.copyOf(calls);
    }

    @HostAccess.Export
    public String deleteOrphans() {
        Supplier<String> call = calls.get("deleteOrphans");
        if (call == null) {
            throw new IllegalStateException("deleteOrphans is not bound");
        }
        String result = call.get();
        return result == null ? "" : result;
    }
}

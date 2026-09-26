package com.ravenherz.cse.scripting;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

public final class JsRuntime {

    private JsRuntime() {
    }

    public static String run(String source, ScriptApi api) {
        try (Context context = Context.newBuilder("js")
                .allowHostAccess(HostAccess.EXPLICIT)
                .allowHostClassLookup(name -> false)
                .allowIO(false)
                .allowCreateThread(false)
                .allowNativeAccess(false)
                .build()) {
            context.getBindings("js").putMember("cse", api);
            Value value = context.eval("js", source == null ? "" : source);
            if (value == null || value.isNull()) {
                return "";
            }
            return value.isString() ? value.asString() : value.toString();
        } catch (PolyglotException ex) {
            throw new IllegalStateException(ex.getMessage() == null ? "Script failed" : ex.getMessage(), ex);
        }
    }
}

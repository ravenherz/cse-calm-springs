package com.ravenherz.cse.core.route;

import java.util.Optional;

/**
 * One module's answer for a context-relative path. The WAR asks every resolver.
 * The first present target wins. A resolver does not send HTTP.
 */
public interface PathResolver {

    /**
     * @param contextRelativePath path with the servlet context already removed, starting with {@code /}
     */
    Optional<PathTarget> resolve(String contextRelativePath);
}

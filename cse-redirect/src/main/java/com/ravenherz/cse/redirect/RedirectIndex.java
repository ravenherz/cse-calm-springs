package com.ravenherz.cse.redirect;

import com.ravenherz.cse.core.route.PathResolver;
import com.ravenherz.cse.core.route.PathTarget;
import com.ravenherz.cse.core.route.ReservedPaths;
import com.ravenherz.cse.dal.dto.BasicEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RedirectIndex implements PathResolver {

    private volatile Map<String, PathTarget> routes = Map.of();
    private volatile List<PrefixRule> prefixes = List.of();

    public void load(List<? extends BasicEntity> rows) {
        Map<String, PathTarget> next = new HashMap<>();
        List<PrefixRule> prefixRules = new ArrayList<>();
        if (rows != null) {
            for (BasicEntity row : rows) {
                if (!(row instanceof ResourceRedirectEntity redirect) || !redirect.isEnabled()) {
                    continue;
                }
                Optional<String> from = RedirectPaths.normalize(redirect.getFromPath());
                Optional<String> target = RedirectPaths.normalize(redirect.getTargetPath());
                if (from.isEmpty() || target.isEmpty() || ReservedPaths.isReserved(from.get())) {
                    continue;
                }
                if (redirect.getStatus() != PathTarget.MOVED_PERMANENTLY
                        && redirect.getStatus() != PathTarget.FOUND) {
                    continue;
                }
                PathTarget pathTarget = new PathTarget(target.get(), redirect.getStatus(),
                        redirect.isPreserveQuery());
                if (from.get().endsWith("/*") && from.get().length() > 2) {
                    prefixRules.add(PrefixRule.of(from.get(), pathTarget));
                } else if (!from.get().endsWith("/*")) {
                    next.put(from.get(), pathTarget);
                }
            }
        }
        prefixRules.sort((left, right) -> Integer.compare(right.base().length(), left.base().length()));
        this.routes = Map.copyOf(next);
        this.prefixes = List.copyOf(prefixRules);
    }

    public void load(ResourceRedirectService store) {
        load(store == null ? List.of() : store.getAll());
    }

    @Override
    public Optional<PathTarget> resolve(String contextRelativePath) {
        Optional<String> key = RedirectPaths.normalize(contextRelativePath);
        if (key.isEmpty() || ReservedPaths.isReserved(key.get())) {
            return Optional.empty();
        }
        PathTarget exact = routes.get(key.get());
        if (exact != null) {
            return Optional.of(exact);
        }
        String path = key.get();
        for (PrefixRule rule : prefixes) {
            if (path.equals(rule.base()) || path.startsWith(rule.base() + "/")) {
                return Optional.of(rule.apply(path));
            }
        }
        return Optional.empty();
    }

    private record PrefixRule(String base, String targetBase, boolean splice, int status, boolean preserveQuery) {

        static PrefixRule of(String from, PathTarget target) {
            String base = from.substring(0, from.length() - 2);
            boolean splice = target.targetPath().endsWith("/*");
            String targetBase = splice
                    ? target.targetPath().substring(0, target.targetPath().length() - 2)
                    : target.targetPath();
            return new PrefixRule(base, targetBase, splice, target.status(), target.preserveQuery());
        }

        PathTarget apply(String path) {
            String suffix = path.length() == base.length() ? "" : path.substring(base.length());
            String dest = targetBase;
            if (splice) {
                dest = targetBase.isEmpty() ? suffix : targetBase + suffix;
            }
            if (dest.isEmpty()) {
                dest = "/";
            }
            return new PathTarget(dest, status, preserveQuery);
        }
    }
}

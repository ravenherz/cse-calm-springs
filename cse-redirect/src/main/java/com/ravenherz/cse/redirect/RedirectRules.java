package com.ravenherz.cse.redirect;

import com.ravenherz.cse.core.route.PathTarget;
import com.ravenherz.cse.core.route.ReservedPaths;
import com.ravenherz.cse.dal.EntityId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class RedirectRules {

    private RedirectRules() {
    }

    public static List<RedirectFieldError> check(ResourceRedirectEntity row, List<ResourceRedirectEntity> existing) {
        List<RedirectFieldError> errors = new ArrayList<>();
        Optional<String> from = RedirectPaths.normalize(row.getFromPath());
        Optional<String> target = RedirectPaths.normalize(row.getTargetPath());
        if (from.isEmpty()) {
            errors.add(new RedirectFieldError("fromPath", "Use a path that starts with /"));
        } else if ("/*".equals(from.get())) {
            errors.add(new RedirectFieldError("fromPath", "Name the prefix before /*"));
        } else if (ReservedPaths.isReserved(from.get())) {
            errors.add(new RedirectFieldError("fromPath", "That path is reserved"));
        } else {
            row.setFromPath(from.get());
        }
        if (target.isEmpty()) {
            errors.add(new RedirectFieldError("targetPath", "Use a path that starts with /"));
        } else {
            row.setTargetPath(target.get());
        }
        if (row.getStatus() != PathTarget.MOVED_PERMANENTLY && row.getStatus() != PathTarget.FOUND) {
            errors.add(new RedirectFieldError("status", "Status must be 301 or 302"));
        }
        if (!errors.isEmpty()) {
            return List.copyOf(errors);
        }
        String fromPath = row.getFromPath();
        if (duplicate(row, fromPath, existing)) {
            errors.add(new RedirectFieldError("fromPath", "A redirect from this path already exists"));
        }
        if (row.isEnabled() && (loops(row, existing) || prefixLoops(row.getFromPath(), row.getTargetPath()))) {
            errors.add(new RedirectFieldError("targetPath", "That target loops"));
        }
        return List.copyOf(errors);
    }

    private static boolean duplicate(ResourceRedirectEntity row, String fromPath, List<ResourceRedirectEntity> existing) {
        EntityId id = row.getId();
        for (ResourceRedirectEntity other : existing) {
            if (other == row || sameId(id, other.getId())) {
                continue;
            }
            Optional<String> otherFrom = RedirectPaths.normalize(other.getFromPath());
            if (otherFrom.isPresent() && otherFrom.get().equals(fromPath)) {
                return true;
            }
        }
        return false;
    }

    private static boolean loops(ResourceRedirectEntity row, List<ResourceRedirectEntity> existing) {
        Map<String, String> edges = new HashMap<>();
        for (ResourceRedirectEntity other : existing) {
            if (!other.isEnabled() || other == row || sameId(row.getId(), other.getId())) {
                continue;
            }
            Optional<String> from = RedirectPaths.normalize(other.getFromPath());
            Optional<String> target = RedirectPaths.normalize(other.getTargetPath());
            if (from.isPresent() && target.isPresent()) {
                edges.put(from.get(), target.get());
            }
        }
        edges.put(row.getFromPath(), row.getTargetPath());
        Set<String> seen = new HashSet<>();
        String cursor = row.getFromPath();
        while (cursor != null && edges.containsKey(cursor)) {
            if (!seen.add(cursor)) {
                return true;
            }
            cursor = edges.get(cursor);
        }
        return false;
    }

    private static boolean prefixLoops(String from, String target) {
        if (from == null || target == null || !from.endsWith("/*")) {
            return false;
        }
        String base = from.substring(0, from.length() - 2);
        String targetBase = target.endsWith("/*") ? target.substring(0, target.length() - 2) : target;
        return targetBase.equals(base) || targetBase.startsWith(base + "/");
    }

    private static boolean sameId(EntityId left, EntityId right) {
        return left != null && left.equals(right);
    }
}

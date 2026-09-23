package com.ravenherz.cse.dal;

import com.ravenherz.cse.dal.AccessActor;
import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.basic.AccessRule;
import com.ravenherz.cse.dal.dto.basic.enums.AccessType;
import com.ravenherz.cse.dal.dto.basic.enums.EventType;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import com.ravenherz.cse.dal.role.CapabilityIds;
import com.ravenherz.cse.dal.role.RoleSeeds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;

public final class EntityAccess {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityAccess.class);

    private static volatile EntityAccessLookup lookup = FallbackLookup.INSTANCE;

    private EntityAccess() {
    }

    public static void setLookup(EntityAccessLookup next) {
        lookup = next == null ? FallbackLookup.INSTANCE : next;
    }

    public static EntityAccessLookup lookup() {
        return lookup;
    }

    public static boolean isAccessible(BasicEntity obtainable, AccessType accessType, AccessActor accessor) {
        if (obtainable == null || accessType == null) {
            return false;
        }
        if (accessor != null && accessor.id() != null && accessor.id().equals(obtainable.getId())
                && obtainable instanceof AccessActor) {
            return true;
        }
        try {
            EntityAccessLookup current = lookup();
            if (current.isSiteOwner(accessor)) {
                return true;
            }
            EntityId creator = ownerId(obtainable);
            if (accessor != null && creator != null && accessor.id() != null && accessor.id().equals(creator)) {
                return true;
            }
            AccessRule leaf = ruleOf(obtainable, accessType);
            if (accessor != null && accessor.id() != null && leaf.hasAccount(accessor.id().toHexString())) {
                return true;
            }
            Set<String> roleIds = resolveRoleIds(obtainable, accessType, current, Collections.newSetFromMap(new IdentityHashMap<>()));
            if (accessor == null) {
                return containsRole(roleIds, current.guestRoleId(), current);
            }
            return containsRole(roleIds, current.roleId(accessor), current);
        } catch (Exception ex) {
            LOGGER.warn("EntityAccess failed closed for {} {}", obtainable.getClass().getSimpleName(), accessType, ex);
            return false;
        }
    }

    public static EntityId ownerId(BasicEntity entity) {
        try {
            return getLastEventByType(EventType.ENTITY_CREATED, entity).getOwnerId();
        } catch (Exception ex) {
            return null;
        }
    }

    public static com.ravenherz.cse.dal.dto.basic.Event getLastEventByType(
            EventType eventType, BasicEntity entity) {
        return java.util.Arrays.stream(entity.getHistoryData().getEvents()).filter(event ->
                event.getEventType().equals(eventType)).findFirst().orElse(null);
    }

    static AccessRule ruleOf(BasicEntity entity, AccessType accessType) {
        if (entity.getSecurityData() == null) {
            return AccessRule.inheritAll();
        }
        return entity.getSecurityData().rule(accessType);
    }

    private static Set<String> resolveRoleIds(BasicEntity entity, AccessType accessType,
            EntityAccessLookup current, Set<BasicEntity> visiting) {
        if (entity == null || !visiting.add(entity)) {
            return current.defaultRoleIds(accessType);
        }
        AccessRule rule = ruleOf(entity, accessType);
        if (!rule.isInherit() && entity.getSecurityData() != null
                && entity.getSecurityData().getAccessSettings() != null
                && entity.getSecurityData().getAccessSettings().containsKey(accessType)) {
            return current.expandRoleIds(rule.getRoleIds());
        }
        BasicEntity parent = parentOf(entity, current);
        if (parent == null) {
            return current.defaultRoleIds(accessType);
        }
        return resolveRoleIds(parent, accessType, current, visiting);
    }

    private static BasicEntity parentOf(BasicEntity entity, EntityAccessLookup current) {
        return current.parentOf(entity);
    }

    private static boolean containsRole(Set<String> roleIds, String roleId, EntityAccessLookup current) {
        if (roleId == null || roleIds == null || roleIds.isEmpty()) {
            return false;
        }
        if (roleIds.contains(roleId)) {
            return true;
        }
        Set<String> expanded = current.expandRoleIds(roleIds);
        return expanded.contains(roleId);
    }

    static final class FallbackLookup implements EntityAccessLookup {

        static final FallbackLookup INSTANCE = new FallbackLookup();

        @Override
        public boolean isSiteOwner(AccessActor account) {
            if (account == null) {
                return false;
            }
            String roleId = roleId(account);
            return RoleSeeds.OWNER.equals(roleId)
                    || RoleSeeds.SYSTEM_OWNER.equals(account.roleId())
                    || SecurityLevel.OWNER.equals(account.level());
        }

        @Override
        public String roleId(AccessActor account) {
            if (account == null) {
                return null;
            }
            String stored = account.roleId();
            if (stored != null && !stored.isBlank()) {
                return stored;
            }
            return RoleSeeds.slugFor(account.level());
        }

        @Override
        public String guestRoleId() {
            return RoleSeeds.GUEST;
        }

        @Override
        public Set<String> defaultRoleIds(AccessType accessType) {
            String capability = switch (accessType) {
                case ACCESS_READ -> CapabilityIds.CONTENT_READ;
                case ACCESS_EDIT -> CapabilityIds.CONTENT_EDIT;
                case ACCESS_DELETE -> CapabilityIds.CONTENT_DELETE;
            };
            return new HashSet<>(RoleSeeds.contentDefaultSlugs(capability));
        }

        @Override
        public BasicEntity parentOf(BasicEntity entity) {
            return null;
        }

        @Override
        public Set<String> expandRoleIds(Iterable<String> storedIds) {
            Set<String> out = new HashSet<>();
            if (storedIds == null) {
                return out;
            }
            for (String id : storedIds) {
                if (id != null && !id.isBlank()) {
                    out.add(id);
                }
            }
            return out;
        }
    }
}

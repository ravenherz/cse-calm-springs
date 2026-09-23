package com.ravenherz.cse.security;

import com.ravenherz.cse.dal.AccessActor;
import com.ravenherz.cse.dal.EntityAccessLookup;
import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.dao.RoleMatrixService;
import com.ravenherz.cse.dal.dao.RoleService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.CategoryEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.ResourceGroupEntity;
import com.ravenherz.cse.dal.dto.RoleEntity;
import com.ravenherz.cse.dal.dto.basic.enums.AccessType;
import com.ravenherz.cse.dal.role.CapabilityIds;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class EntityAccessLookupImpl implements EntityAccessLookup {

    private final RoleService roles;
    private final RoleMatrixService matrix;
    private final ServiceProvider services;

    public EntityAccessLookupImpl(RoleService roles, RoleMatrixService matrix, ServiceProvider services) {
        this.roles = roles;
        this.matrix = matrix;
        this.services = services;
    }

    @Override
    public boolean isSiteOwner(AccessActor account) {
        return account instanceof AccountEntity entity && AccountRoles.isOwner(entity, roles);
    }

    @Override
    public String roleId(AccessActor account) {
        return account instanceof AccountEntity entity ? AccountRoles.roleId(entity, roles) : null;
    }

    @Override
    public String guestRoleId() {
        RoleEntity guest = roles.guest();
        return guest == null ? "guest" : guest.idHex();
    }

    @Override
    public Set<String> defaultRoleIds(AccessType accessType) {
        String capability = switch (accessType) {
            case ACCESS_READ -> CapabilityIds.CONTENT_READ;
            case ACCESS_EDIT -> CapabilityIds.CONTENT_EDIT;
            case ACCESS_DELETE -> CapabilityIds.CONTENT_DELETE;
        };
        Set<String> ids = new HashSet<>();
        for (RoleEntity role : roles.getAll()) {
            if (role == null || role.getId() == null || role.isOwner()) {
                continue;
            }
            if (matrix.allows(role.idHex(), capability)) {
                ids.add(role.idHex());
                ids.add(role.getSlug());
            }
        }
        return ids;
    }

    @Override
    public BasicEntity parentOf(BasicEntity entity) {
        if (entity instanceof ItemEntity item && item.getRefCategoryId() != null) {
            if (item.getRefCategory() != null) {
                return item.getRefCategory();
            }
            return services.getCategoryService().getById(CategoryEntity.class,
                    com.ravenherz.cse.dal.StoredIds.entityId(item.getRefCategoryId()));
        }
        if (entity instanceof ResourceEntity resource && resource.refResourceGroupObjectId() != null) {
            if (resource.getRefResourceGroup() != null) {
                return resource.getRefResourceGroup();
            }
            return services.getResourceGroupService()
                    .getById(ResourceGroupEntity.class,
                            com.ravenherz.cse.dal.StoredIds.entityId(resource.refResourceGroupObjectId()));
        }
        if (entity instanceof ResourceGroupEntity group && group.refParentGroupObjectId() != null) {
            if (group.getRefParentGroup() != null) {
                return group.getRefParentGroup();
            }
            return services.getResourceGroupService()
                    .getById(ResourceGroupEntity.class,
                            com.ravenherz.cse.dal.StoredIds.entityId(group.refParentGroupObjectId()));
        }
        return null;
    }

    @Override
    public Set<String> expandRoleIds(Iterable<String> storedIds) {
        Set<String> out = new HashSet<>();
        if (storedIds == null) {
            return out;
        }
        List<RoleEntity> all = roles.getAll();
        for (String stored : storedIds) {
            if (stored == null || stored.isBlank()) {
                continue;
            }
            out.add(stored);
            RoleEntity role = roles.getById(stored);
            if (role == null) {
                role = roles.getBySlug(stored);
            }
            if (role == null) {
                for (RoleEntity candidate : all) {
                    if (stored.equals(candidate.getSlug()) || stored.equals(candidate.idHex())) {
                        role = candidate;
                        break;
                    }
                }
            }
            if (role != null) {
                if (role.getId() != null) {
                    out.add(role.idHex());
                }
                if (role.getSlug() != null) {
                    out.add(role.getSlug());
                }
            }
        }
        return out;
    }
}

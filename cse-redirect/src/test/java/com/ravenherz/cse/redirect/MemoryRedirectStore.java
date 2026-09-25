package com.ravenherz.cse.redirect;

import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.dto.BasicEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class MemoryRedirectStore implements ResourceRedirectService {

    private final Map<EntityId, ResourceRedirectEntity> rows = new LinkedHashMap<>();

    @Override
    public EntityId insert(BasicEntity entity) {
        ResourceRedirectEntity redirect = (ResourceRedirectEntity) entity;
        if (redirect.getId() == null) {
            redirect.setId(EntityId.generate());
        }
        rows.put(redirect.getId(), redirect);
        return redirect.getId();
    }

    @Override
    public List<BasicEntity> getAll() {
        return new ArrayList<>(rows.values());
    }

    @Override
    public BasicEntity getById(Class<? extends BasicEntity> entityClass, EntityId id) {
        ResourceRedirectEntity row = rows.get(id);
        if (row == null || !entityClass.isInstance(row)) {
            return null;
        }
        return row;
    }

    @Override
    public boolean replace(BasicEntity entity) {
        ResourceRedirectEntity redirect = (ResourceRedirectEntity) entity;
        if (redirect.getId() == null || !rows.containsKey(redirect.getId())) {
            return false;
        }
        rows.put(redirect.getId(), redirect);
        return true;
    }

    @Override
    public void delete(BasicEntity entity) {
        if (entity != null && entity.getId() != null) {
            rows.remove(entity.getId());
        }
    }

    @Override
    public ResourceRedirectEntity getByFromPath(String fromPath) {
        Optional<String> key = RedirectPaths.normalize(fromPath);
        if (key.isEmpty()) {
            return null;
        }
        for (ResourceRedirectEntity row : rows.values()) {
            Optional<String> from = RedirectPaths.normalize(row.getFromPath());
            if (from.isPresent() && from.get().equals(key.get())) {
                return row;
            }
        }
        return null;
    }
}

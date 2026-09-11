package com.ravenherz.cse.dal.dao.impl;

import com.ravenherz.cse.dal.DataProvider;
import com.ravenherz.cse.dal.dao.RoleService;
import com.ravenherz.cse.dal.dto.RoleEntity;
import com.ravenherz.cse.dal.role.RoleSeeds;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository("roleService")
public class RoleServiceImpl implements RoleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoleServiceImpl.class);

    private final DataProvider dataProvider;
    private final Map<String, RoleEntity> byId = new ConcurrentHashMap<>();
    private final Map<String, RoleEntity> bySlug = new ConcurrentHashMap<>();
    private volatile boolean cacheLoaded;

    public RoleServiceImpl(DataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    private MongoTemplate mongo() {
        return dataProvider.getMongoTemplate();
    }

    @Override
    public List<RoleEntity> getAll() {
        ensureCache();
        List<RoleEntity> all = new ArrayList<>(byId.values());
        all.sort(Comparator.comparingInt(RoleEntity::getSortOrder).thenComparing(RoleEntity::getSlug,
                String.CASE_INSENSITIVE_ORDER));
        return all;
    }

    @Override
    public RoleEntity getById(String idHex) {
        if (idHex == null || idHex.isBlank()) {
            return null;
        }
        ensureCache();
        RoleEntity cached = byId.get(idHex);
        if (cached != null) {
            return cached;
        }
        if (!ObjectId.isValid(idHex)) {
            return getBySlug(idHex);
        }
        RoleEntity found = mongo().findById(new ObjectId(idHex), RoleEntity.class);
        if (dropRetiredGuide(found)) {
            return null;
        }
        remember(found);
        return found;
    }

    @Override
    public RoleEntity getBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return null;
        }
        ensureCache();
        return bySlug.get(slug.trim().toLowerCase(Locale.ROOT));
    }

    @Override
    public RoleEntity guest() {
        return getBySlug(RoleSeeds.GUEST);
    }

    @Override
    public RoleEntity owner() {
        return getBySlug(RoleSeeds.OWNER);
    }

    @Override
    public RoleEntity insert(RoleEntity role) {
        if (role == null || RoleSeeds.isRetiredSlug(role.getSlug())) {
            return null;
        }
        mongo().save(role);
        remember(role);
        return role;
    }

    @Override
    public boolean replace(RoleEntity role) {
        if (role == null || RoleSeeds.isRetiredSlug(role.getSlug())) {
            return false;
        }
        mongo().save(role);
        remember(role);
        return true;
    }

    @Override
    public boolean delete(RoleEntity role) {
        if (role == null) {
            return false;
        }
        mongo().remove(role);
        forget(role);
        return true;
    }

    @Override
    public synchronized void ensureSeeded() {
        if (!dataProvider.ping()) {
            return;
        }
        ensureIndexes();
        List<RoleEntity> existing = mongo().findAll(RoleEntity.class);
        for (RoleEntity role : existing) {
            if (dropRetiredGuide(role)) {
                continue;
            }
            remember(role);
        }
        for (RoleSeeds.SeedRole seed : RoleSeeds.roles()) {
            if (bySlug.containsKey(seed.slug())) {
                continue;
            }
            RoleEntity role = new RoleEntity();
            role.setSlug(seed.slug());
            role.setName(seed.name());
            role.setSystem(seed.system());
            role.setLoginable(seed.loginable());
            role.setSortOrder(seed.sortOrder());
            role.setArchived(false);
            mongo().save(role);
            remember(role);
            LOGGER.info("Seeded role {}", seed.slug());
        }
        cacheLoaded = true;
    }

    @Override
    public synchronized void invalidateCache() {
        byId.clear();
        bySlug.clear();
        cacheLoaded = false;
    }

    @Override
    public void ensureIndexes() {
        try {
            mongo().indexOps(RoleEntity.class).createIndex(new Index().on("slug", Sort.Direction.ASC).unique());
        } catch (Exception ex) {
            LOGGER.debug("Role slug index: {}", ex.getMessage());
        }
    }

    private void ensureCache() {
        if (cacheLoaded) {
            return;
        }
        synchronized (this) {
            if (cacheLoaded) {
                return;
            }
            if (!dataProvider.ping()) {
                return;
            }
            for (RoleEntity role : mongo().findAll(RoleEntity.class)) {
                if (dropRetiredGuide(role)) {
                    continue;
                }
                remember(role);
            }
            cacheLoaded = true;
        }
    }

    private boolean dropRetiredGuide(RoleEntity role) {
        if (role == null || !RoleSeeds.isRetiredSlug(role.getSlug())) {
            return false;
        }
        LOGGER.info("Removing retired Guide role");
        mongo().remove(role);
        forget(role);
        return true;
    }

    private void remember(RoleEntity role) {
        if (role == null) {
            return;
        }
        if (role.getId() != null) {
            byId.put(role.getId().toHexString(), role);
        }
        if (role.getSlug() != null) {
            bySlug.put(role.getSlug().toLowerCase(Locale.ROOT), role);
        }
    }

    private void forget(RoleEntity role) {
        if (role == null) {
            return;
        }
        if (role.getId() != null) {
            byId.remove(role.getId().toHexString());
        }
        if (role.getSlug() != null) {
            bySlug.remove(role.getSlug().toLowerCase(Locale.ROOT));
        }
    }
}

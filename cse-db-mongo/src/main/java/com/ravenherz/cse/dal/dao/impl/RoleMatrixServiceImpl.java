package com.ravenherz.cse.dal.dao.impl;

import com.ravenherz.cse.dal.DataProvider;
import com.ravenherz.cse.dal.dao.RoleMatrixService;
import com.ravenherz.cse.dal.dao.RoleService;
import com.ravenherz.cse.dal.dto.RoleEntity;
import com.ravenherz.cse.dal.dto.RoleMatrixDocument;
import com.ravenherz.cse.dal.dto.basic.AppAccountGrant;
import com.ravenherz.cse.dal.dto.basic.RoleGrant;
import com.ravenherz.cse.dal.role.CapabilityIds;
import com.ravenherz.cse.dal.role.RoleSeeds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository("roleMatrixService")
public class RoleMatrixServiceImpl implements RoleMatrixService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoleMatrixServiceImpl.class);

    private final DataProvider dataProvider;
    private volatile RoleMatrixDocument cached;

    public RoleMatrixServiceImpl(DataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    private MongoTemplate mongo() {
        return dataProvider.getMongoTemplate();
    }

    @Override
    public RoleMatrixDocument get() {
        RoleMatrixDocument local = cached;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (cached != null) {
                return cached;
            }
            RoleMatrixDocument doc = mongo().findById(RoleMatrixDocument.SINGLETON_ID, RoleMatrixDocument.class);
            if (doc == null) {
                doc = new RoleMatrixDocument();
                doc.setId(RoleMatrixDocument.SINGLETON_ID);
            }
            cached = doc;
            return doc;
        }
    }

    @Override
    public synchronized void replaceGrants(List<RoleGrant> grants) {
        RoleMatrixDocument doc = get();
        doc.setGrants(grants == null ? new ArrayList<>() : new ArrayList<>(grants));
        mongo().save(doc);
        cached = doc;
    }

    @Override
    public synchronized void replaceAppGrants(List<AppAccountGrant> appGrants) {
        RoleMatrixDocument doc = get();
        doc.setAppGrants(appGrants == null ? new ArrayList<>() : new ArrayList<>(appGrants));
        mongo().save(doc);
        cached = doc;
    }

    @Override
    public boolean allows(String roleId, String capabilityId) {
        if (roleId == null || capabilityId == null) {
            return false;
        }
        for (RoleGrant grant : get().getGrants()) {
            if (grant != null && capabilityId.equals(grant.getCapabilityId())
                    && roleId.equals(grant.getRoleId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean allowsAppAccount(String accountId, String capabilityId) {
        if (accountId == null || capabilityId == null) {
            return false;
        }
        for (AppAccountGrant grant : get().getAppGrants()) {
            if (grant != null && capabilityId.equals(grant.getCapabilityId())
                    && accountId.equals(grant.getAccountId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized void ensureSeeded(RoleService roles) {
        if (!dataProvider.ping()) {
            return;
        }
        RoleMatrixDocument existing = mongo().findById(RoleMatrixDocument.SINGLETON_ID, RoleMatrixDocument.class);
        if (existing != null && existing.getGrants() != null && !existing.getGrants().isEmpty()) {
            dropUnknownRoleGrants(roles, existing);
            backfillInstanceGrant(existing);
            if (existing.getGrants() != null && !existing.getGrants().isEmpty()) {
                cached = existing;
                return;
            }
        }
        List<RoleGrant> grants = new ArrayList<>();
        Map<String, List<String>> bySlug = RoleSeeds.seedGrantsBySlug();
        for (Map.Entry<String, List<String>> entry : bySlug.entrySet()) {
            RoleEntity role = roles.getBySlug(entry.getKey());
            if (role == null || role.getId() == null) {
                continue;
            }
            String roleId = role.getId().toHexString();
            Set<String> seen = new HashSet<>();
            for (String capabilityId : entry.getValue()) {
                if (capabilityId == null || !seen.add(capabilityId)) {
                    continue;
                }
                grants.add(new RoleGrant(capabilityId, roleId));
            }
        }
        RoleMatrixDocument doc = existing == null ? new RoleMatrixDocument() : existing;
        doc.setId(RoleMatrixDocument.SINGLETON_ID);
        doc.setGrants(grants);
        if (doc.getAppGrants() == null) {
            doc.setAppGrants(new ArrayList<>());
        }
        mongo().save(doc);
        cached = doc;
        LOGGER.info("Seeded role matrix with {} grants", grants.size());
    }

    private void backfillInstanceGrant(RoleMatrixDocument doc) {
        if (doc == null || doc.getGrants() == null) {
            return;
        }
        Set<String> have = new HashSet<>();
        Set<String> candidates = new HashSet<>();
        for (RoleGrant grant : doc.getGrants()) {
            if (grant == null || grant.getRoleId() == null || grant.getCapabilityId() == null) {
                continue;
            }
            if (CapabilityIds.EDITOR_INSTANCE.equals(grant.getCapabilityId())) {
                have.add(grant.getRoleId());
            }
            if (CapabilityIds.EDITOR_ACCESS.equals(grant.getCapabilityId())
                    || CapabilityIds.EDITOR_LOGS.equals(grant.getCapabilityId())
                    || CapabilityIds.EDITOR_SETTINGS.equals(grant.getCapabilityId())) {
                candidates.add(grant.getRoleId());
            }
        }
        List<RoleGrant> next = new ArrayList<>(doc.getGrants());
        int added = 0;
        for (String roleId : candidates) {
            if (have.add(roleId)) {
                next.add(new RoleGrant(CapabilityIds.EDITOR_INSTANCE, roleId));
                added++;
            }
        }
        if (added > 0) {
            doc.setGrants(next);
            mongo().save(doc);
            LOGGER.info("Granted Instance to {} existing editor role(s)", added);
        }
    }

    private void dropUnknownRoleGrants(RoleService roles, RoleMatrixDocument doc) {
        Set<String> known = new HashSet<>();
        if (roles != null) {
            for (RoleEntity role : roles.getAll()) {
                if (role != null && role.idHex() != null) {
                    known.add(role.idHex());
                }
            }
        }
        List<RoleGrant> next = new ArrayList<>();
        boolean changed = false;
        for (RoleGrant grant : doc.getGrants()) {
            if (grant != null && grant.getRoleId() != null && !known.isEmpty()
                    && !known.contains(grant.getRoleId())) {
                changed = true;
                continue;
            }
            if (grant != null) {
                next.add(grant);
            }
        }
        if (changed) {
            doc.setGrants(next);
            mongo().save(doc);
            LOGGER.info("Dropped grants for retired roles");
        }
    }

    @Override
    public synchronized void invalidateCache() {
        cached = null;
    }
}

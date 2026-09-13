package com.ravenherz.cse.transfer;

import com.ravenherz.cse.constants.MongoCollections;
import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.AppEntity;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.CategoryEntity;
import com.ravenherz.cse.dal.dto.DataChunkEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.PlaylistEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.ResourceGroupEntity;
import com.ravenherz.cse.dal.dto.RoleEntity;
import com.ravenherz.cse.dal.dto.RoleMatrixDocument;
import com.ravenherz.cse.dal.dto.SettingContextEntity;
import com.ravenherz.cse.dal.dto.ThemeEntity;
import com.ravenherz.cse.dal.dao.RoleMatrixService;
import com.ravenherz.cse.dal.dao.RoleService;
import com.ravenherz.cse.dal.dao.impl.AppStoreServiceImpl;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.role.CapabilityIds;
import com.ravenherz.cse.dal.role.RoleSeeds;
import com.ravenherz.cse.present.ResourceGroupIndex;
import com.ravenherz.cse.security.AccountRoles;
import com.ravenherz.cse.store.AppStoreNames;
import com.ravenherz.cse.util.Settings;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Component
public class CseSiteImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CseSiteImporter.class);

    private final ResourceGroupIndex resourceGroupIndex;
    private final RoleService roleService;
    private final RoleMatrixService roleMatrixService;

    public CseSiteImporter() {
        this(null, null, null);
    }

    @Autowired
    public CseSiteImporter(ResourceGroupIndex resourceGroupIndex, RoleService roleService,
            RoleMatrixService roleMatrixService) {
        this.resourceGroupIndex = resourceGroupIndex;
        this.roleService = roleService;
        this.roleMatrixService = roleMatrixService;
    }

    public ImportResult apply(InputStream zip, MongoTemplate mongo, Settings settings) throws IOException {
        if (mongo == null) {
            throw new CseSiteImportException("Database is not reachable.");
        }
        CseSiteArchive archive = CseSiteArchive.read(zip);
        ParsedSite parsed = parse(archive);
        Map<String, Integer> counts = new LinkedHashMap<>();
        importRoles(mongo, parsed, counts);
        backfillAccountRoleIds(parsed.accounts);
        counts.put(MongoCollections.DATABASE_ACCOUNTS,
                replace(mongo, AccountEntity.class, parsed.accounts));
        counts.put(MongoCollections.DATABASE_CATEGORIES,
                replace(mongo, CategoryEntity.class, parsed.categories));
        counts.put(MongoCollections.DATABASE_RESOURCE_GROUPS,
                replace(mongo, ResourceGroupEntity.class, parsed.groups));
        counts.put(MongoCollections.DATABASE_DATACHUNKS,
                replace(mongo, DataChunkEntity.class, parsed.chunks));
        counts.put(MongoCollections.DATABASE_RESOURCES,
                replace(mongo, ResourceEntity.class, parsed.resources));
        counts.put(MongoCollections.DATABASE_ITEMS,
                replace(mongo, ItemEntity.class, parsed.items));
        counts.put(MongoCollections.DATABASE_PLAYLISTS,
                replace(mongo, PlaylistEntity.class, parsed.playlists));
        counts.put(MongoCollections.DATABASE_APPS,
                replace(mongo, AppEntity.class, parsed.apps));
        counts.put(MongoCollections.DATABASE_THEMES,
                replace(mongo, ThemeEntity.class, parsed.themes));
        counts.put(MongoCollections.DATABASE_SETTINGS, replaceSettings(mongo, parsed.settings, settings));
        importAppStores(archive, mongo, counts);
        if (settings != null) {
            settings.reloadFromMongo();
        }
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            LOGGER.info("csesite import {}: {} documents", entry.getKey(), entry.getValue());
        }
        if (resourceGroupIndex != null) {
            resourceGroupIndex.rebuild();
        }
        return new ImportResult(counts);
    }

    private static ParsedSite parse(CseSiteArchive archive) {
        ParsedSite parsed = new ParsedSite();
        parsed.accounts = readAll(archive.collection(MongoCollections.DATABASE_ACCOUNTS), CseSiteReaders::account);
        parsed.categories = readAll(archive.collection(MongoCollections.DATABASE_CATEGORIES), CseSiteReaders::category);
        parsed.groups = readAll(archive.collection(MongoCollections.DATABASE_RESOURCE_GROUPS),
                CseSiteReaders::resourceGroup);
        parsed.chunks = readAll(archive.collection(MongoCollections.DATABASE_DATACHUNKS), CseSiteReaders::chunk);
        parsed.resources = readAll(archive.collection(MongoCollections.DATABASE_RESOURCES), CseSiteReaders::resource);
        parsed.items = readAll(archive.collection(MongoCollections.DATABASE_ITEMS), CseSiteReaders::item);
        parsed.playlists = readAll(archive.collection(MongoCollections.DATABASE_PLAYLISTS), CseSiteReaders::playlist);
        parsed.apps = readAll(archive.collection(MongoCollections.DATABASE_APPS), CseSiteReaders::app);
        parsed.themes = readAll(archive.collection(MongoCollections.DATABASE_THEMES), CseSiteReaders::theme);
        parsed.roles = readAll(archive.collection(MongoCollections.DATABASE_ROLES), CseSiteReaders::role);
        parsed.matrix = readAll(archive.collection(MongoCollections.DATABASE_ROLE_MATRIX),
                CseSiteReaders::roleMatrix);
        parsed.settings = new ArrayList<>();
        for (Map<String, Object> doc : archive.collection(MongoCollections.DATABASE_SETTINGS)) {
            SettingContextEntity entity = CseSiteReaders.setting(doc);
            if (entity != null) {
                parsed.settings.add(entity);
            }
        }
        return parsed;
    }

    private static <T> List<T> readAll(List<Map<String, Object>> docs, Function<Map<String, Object>, T> reader) {
        List<T> out = new ArrayList<>();
        if (docs == null) {
            return out;
        }
        for (Map<String, Object> doc : docs) {
            T entity = reader.apply(doc);
            if (entity != null) {
                out.add(entity);
            }
        }
        return out;
    }

    private static <T extends BasicEntity> int replace(MongoTemplate mongo, Class<T> type, List<T> incoming) {
        Set<ObjectId> keep = new HashSet<>();
        for (T entity : incoming) {
            if (entity == null || entity.getId() == null) {
                continue;
            }
            mongo.save(entity);
            keep.add(entity.getId());
        }
        for (T existing : mongo.findAll(type)) {
            if (existing != null && existing.getId() != null && !keep.contains(existing.getId())) {
                mongo.remove(existing);
            }
        }
        return incoming.size();
    }

    private static int replaceSettings(MongoTemplate mongo, List<SettingContextEntity> incoming, Settings settings) {
        Set<String> keep = new HashSet<>();
        int stored = 0;
        for (SettingContextEntity entity : incoming) {
            if (entity == null || entity.getContext() == null) {
                continue;
            }
            if (settings != null && !settings.isOverlayContext(entity.getContext())) {
                continue;
            }
            if (settings == null && !isOverlayWithoutSettings(entity.getContext())) {
                continue;
            }
            mongo.save(entity);
            keep.add(entity.getContext());
            stored++;
        }
        for (SettingContextEntity existing : mongo.findAll(SettingContextEntity.class)) {
            if (existing == null || existing.getContext() == null) {
                continue;
            }
            boolean overlay = settings == null
                    ? isOverlayWithoutSettings(existing.getContext())
                    : settings.isOverlayContext(existing.getContext());
            if (overlay && !keep.contains(existing.getContext())) {
                mongo.remove(existing);
            }
        }
        return stored;
    }

    private void importRoles(MongoTemplate mongo, ParsedSite parsed, Map<String, Integer> counts) {
        if (parsed.roles.isEmpty()) {
            if (roleService != null) {
                roleService.ensureSeeded();
                if (roleMatrixService != null) {
                    roleMatrixService.ensureSeeded(roleService);
                }
            }
            counts.put(MongoCollections.DATABASE_ROLES, 0);
            counts.put(MongoCollections.DATABASE_ROLE_MATRIX, 0);
            return;
        }
        counts.put(MongoCollections.DATABASE_ROLES, replaceDocuments(mongo, RoleEntity.class, parsed.roles));
        if (parsed.matrix.isEmpty()) {
            if (roleService != null) {
                roleService.invalidateCache();
                if (roleMatrixService != null) {
                    roleMatrixService.ensureSeeded(roleService);
                }
            }
            counts.put(MongoCollections.DATABASE_ROLE_MATRIX, 0);
        } else {
            counts.put(MongoCollections.DATABASE_ROLE_MATRIX,
                    replaceDocuments(mongo, RoleMatrixDocument.class, parsed.matrix));
        }
        if (roleService != null) {
            roleService.invalidateCache();
        }
        if (roleMatrixService != null) {
            roleMatrixService.invalidateCache();
        }
    }

    private void backfillAccountRoleIds(List<AccountEntity> accounts) {
        if (accounts == null) {
            return;
        }
        for (AccountEntity account : accounts) {
            if (account == null || account.getAccountData() == null) {
                continue;
            }
            AccountData data = account.getAccountData();
            RoleEntity role = null;
            if (roleService != null && data.getRoleId() != null && !data.getRoleId().isBlank()) {
                role = roleService.getById(data.getRoleId());
                if (role == null) {
                    role = roleService.getBySlug(data.getRoleId());
                }
            }
            if (role == null && roleService != null) {
                String slug = RoleSeeds.slugFor(data.getLevel());
                if (slug == null) {
                    slug = data.isLoginable() ? RoleSeeds.MEMBER : RoleSeeds.INACTIVE;
                }
                role = roleService.getBySlug(slug);
            }
            if (role != null) {
                boolean editor = role.isOwner()
                        || (roleMatrixService != null
                        && roleMatrixService.allows(role.idHex(), CapabilityIds.EDITOR_ACCESS));
                AccountRoles.assign(data, role, editor);
            } else if (data.getRoleId() == null || data.getRoleId().isBlank()) {
                String slug = RoleSeeds.slugFor(data.getLevel());
                data.setRoleId(slug == null
                        ? (data.isLoginable() ? RoleSeeds.MEMBER : RoleSeeds.INACTIVE)
                        : slug);
            }
        }
    }

    private static <T> int replaceDocuments(MongoTemplate mongo, Class<T> type, List<T> incoming) {
        Set<ObjectId> keep = new HashSet<>();
        for (T entity : incoming) {
            ObjectId id = idOf(entity);
            if (id == null) {
                continue;
            }
            mongo.save(entity);
            keep.add(id);
        }
        for (T existing : mongo.findAll(type)) {
            ObjectId id = idOf(existing);
            if (id != null && !keep.contains(id)) {
                mongo.remove(existing);
            }
        }
        return incoming.size();
    }

    private static ObjectId idOf(Object entity) {
        if (entity instanceof RoleEntity role) {
            return role.getId();
        }
        if (entity instanceof RoleMatrixDocument matrix) {
            return matrix.getId();
        }
        if (entity instanceof BasicEntity basic) {
            return basic.getId();
        }
        return null;
    }

    private static void importAppStores(CseSiteArchive archive, MongoTemplate mongo,
            Map<String, Integer> counts) {
        Set<String> imported = new HashSet<>();
        for (Map.Entry<String, List<Map<String, Object>>> extra : archive.extraCollections().entrySet()) {
            String name = extra.getKey();
            if (name != null && name.startsWith("cse-")) {
                LOGGER.warn("csesite: skip unknown CMS collection {}", name);
                continue;
            }
            if (!AppStoreNames.isAppCollection(name)) {
                LOGGER.warn("csesite: skip extra collection {}", name);
                continue;
            }
            List<Map<String, Object>> docs = extra.getValue() == null ? List.of() : extra.getValue();
            replaceAppStore(mongo, name, docs);
            imported.add(name);
            counts.put(name, docs.size());
        }
        java.util.Collection<String> live = mongo.getCollectionNames();
        if (live == null) {
            return;
        }
        for (String name : live) {
            if (AppStoreNames.isAppCollection(name) && !imported.contains(name)) {
                mongo.dropCollection(name);
            }
        }
    }

    private static void replaceAppStore(MongoTemplate mongo, String collection,
            List<Map<String, Object>> docs) {
        mongo.dropCollection(collection);
        List<Document> rows = new ArrayList<>();
        for (Map<String, Object> doc : docs) {
            Document bson = AppStoreServiceImpl.toBson(doc);
            if (bson != null) {
                rows.add(bson);
            }
        }
        if (!rows.isEmpty()) {
            mongo.insert(rows, collection);
        }
    }

    private static boolean isOverlayWithoutSettings(String context) {
        return context != null
                && !SettingKeys.isSecretContext(context)
                && !SettingKeys.CONTEXT_DATASOURCE_BUILD_INFO.equals(context)
                && !"config-dbms-instance".equals(context)
                && !"config-dbms-access".equals(context);
    }

    public record ImportResult(Map<String, Integer> counts) {
    }

    private static final class ParsedSite {
        private List<AccountEntity> accounts = List.of();
        private List<CategoryEntity> categories = List.of();
        private List<ResourceGroupEntity> groups = List.of();
        private List<DataChunkEntity> chunks = List.of();
        private List<ResourceEntity> resources = List.of();
        private List<ItemEntity> items = List.of();
        private List<PlaylistEntity> playlists = List.of();
        private List<AppEntity> apps = List.of();
        private List<ThemeEntity> themes = List.of();
        private List<RoleEntity> roles = List.of();
        private List<RoleMatrixDocument> matrix = List.of();
        private List<SettingContextEntity> settings = List.of();
    }
}

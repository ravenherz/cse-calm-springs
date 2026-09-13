package com.ravenherz.cse.transfer;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.AppEntity;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.CategoryEntity;
import com.ravenherz.cse.dal.dto.DataChunkEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.PlaylistEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.ResourceGroupEntity;
import com.ravenherz.cse.dal.dto.SettingContextEntity;
import com.ravenherz.cse.dal.dto.ThemeEntity;
import com.ravenherz.cse.store.AppStoreTableSpec;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.dto.basic.AlbumData;
import com.ravenherz.cse.dal.dto.basic.AppData;
import com.ravenherz.cse.dal.dto.basic.AppStoreSettings;
import com.ravenherz.cse.dal.dto.basic.CategoryData;
import com.ravenherz.cse.dal.dto.basic.Event;
import com.ravenherz.cse.dal.dto.basic.HistoryData;
import com.ravenherz.cse.dal.dto.basic.PageData;
import com.ravenherz.cse.dal.dto.basic.PlaylistData;
import com.ravenherz.cse.dal.dto.basic.PlaylistTrack;
import com.ravenherz.cse.dal.dto.basic.ResourceData;
import com.ravenherz.cse.dal.dto.basic.ResourceGroupData;
import com.ravenherz.cse.dal.dto.RoleEntity;
import com.ravenherz.cse.dal.dto.RoleMatrixDocument;
import com.ravenherz.cse.dal.dto.basic.AccessRule;
import com.ravenherz.cse.dal.dto.basic.AppAccountGrant;
import com.ravenherz.cse.dal.dto.basic.RoleGrant;
import com.ravenherz.cse.dal.dto.basic.SecurityData;
import com.ravenherz.cse.dal.dto.basic.ThemeData;
import com.ravenherz.cse.dal.dto.basic.enums.AccessType;
import com.ravenherz.cse.dal.dto.basic.enums.EventType;
import com.ravenherz.cse.dal.dto.basic.enums.ResourceType;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import com.ravenherz.cse.dal.role.RoleSeeds;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class CseSiteReaders {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private CseSiteReaders() {
    }

    static AccountEntity account(Map<String, Object> doc) {
        AccountEntity entity = new AccountEntity();
        if (!applyBasic(entity, doc)) {
            return null;
        }
        Map<String, Object> data = map(doc.get("accountData"));
        if (data != null) {
            AccountData accountData = new AccountData();
            accountData.setLogin(text(data.get("login")));
            accountData.setHash(text(data.get("hash")));
            accountData.setEmailAddress(text(data.get("emailAddress")));
            accountData.setBio(text(data.get("bio")));
            accountData.setShownName(text(data.get("shownName")));
            accountData.setAvatar(text(data.get("avatar")));
            accountData.setLevel(enumOrNull(SecurityLevel.class, data.get("level")));
            accountData.setContacts(stringHash(data.get("contacts")));
            accountData.setExtensibleData(stringHash(data.get("extensibleData")));
            accountData.setStylesTheme(text(data.get("stylesTheme")));
            accountData.setStylesSchema(text(data.get("stylesSchema")));
            accountData.setLoginable(bool(data.get("loginable"), true));
            accountData.setRoleId(text(data.get("roleId")));
            if (accountData.getRoleId() == null || accountData.getRoleId().isBlank()) {
                String slug = RoleSeeds.slugFor(accountData.getLevel());
                if (slug == null) {
                    slug = accountData.isLoginable() ? RoleSeeds.MEMBER : RoleSeeds.INACTIVE;
                }
                accountData.setRoleId(slug);
            }
            accountData.setActivationToken(text(data.get("activationToken")));
            accountData.setSessions(null);
            entity.setAccountData(accountData);
        }
        return entity;
    }

    static RoleEntity role(Map<String, Object> doc) {
        if (doc == null) {
            return null;
        }
        RoleEntity entity = new RoleEntity();
        ObjectId id = objectId(doc.get("id"));
        if (id == null) {
            id = new ObjectId();
        }
        entity.setId(id);
        entity.setEntityVersion(text(doc.get("entityVersion")));
        String slug = text(doc.get("slug"));
        if (slug == null || slug.isBlank()) {
            return null;
        }
        entity.setSlug(slug.trim().toLowerCase(java.util.Locale.ROOT));
        entity.setName(text(doc.get("name")));
        entity.setSystem(text(doc.get("system")));
        entity.setLoginable(bool(doc.get("loginable"), false));
        entity.setSortOrder(intValue(doc.get("sortOrder"), 100));
        entity.setArchived(bool(doc.get("archived"), false));
        return entity;
    }

    static RoleMatrixDocument roleMatrix(Map<String, Object> doc) {
        RoleMatrixDocument entity = new RoleMatrixDocument();
        ObjectId id = objectId(doc == null ? null : doc.get("id"));
        entity.setId(id == null ? RoleMatrixDocument.SINGLETON_ID : id);
        if (doc == null) {
            return entity;
        }
        List<RoleGrant> grants = new ArrayList<>();
        for (Object row : list(doc.get("grants"))) {
            Map<String, Object> grantDoc = map(row);
            if (grantDoc == null) {
                continue;
            }
            String capabilityId = text(grantDoc.get("capabilityId"));
            String roleId = text(grantDoc.get("roleId"));
            if (capabilityId == null || roleId == null || capabilityId.isBlank() || roleId.isBlank()) {
                continue;
            }
            grants.add(new RoleGrant(capabilityId, roleId));
        }
        entity.setGrants(grants);
        List<AppAccountGrant> appGrants = new ArrayList<>();
        for (Object row : list(doc.get("appGrants"))) {
            Map<String, Object> grantDoc = map(row);
            if (grantDoc == null) {
                continue;
            }
            String capabilityId = text(grantDoc.get("capabilityId"));
            String accountId = text(grantDoc.get("accountId"));
            if (capabilityId == null || accountId == null || capabilityId.isBlank() || accountId.isBlank()) {
                continue;
            }
            appGrants.add(new AppAccountGrant(capabilityId, accountId));
        }
        entity.setAppGrants(appGrants);
        return entity;
    }

    static CategoryEntity category(Map<String, Object> doc) {
        CategoryEntity entity = new CategoryEntity();
        if (!applyBasic(entity, doc)) {
            return null;
        }
        Map<String, Object> data = map(doc.get("categoryData"));
        if (data != null) {
            CategoryData categoryData = new CategoryData();
            categoryData.setItemName(text(data.get("itemName")));
            categoryData.setNavigationTitle(text(data.get("navigationTitle")));
            categoryData.setNavigationDescription(text(data.get("navigationDescription")));
            categoryData.setVisible(bool(data.get("visible"), false));
            categoryData.setActive(bool(data.get("active"), false));
            categoryData.setDisplayCount(intValue(data.get("displayCount"), 0));
            categoryData.setDisplayPriority(intValue(data.get("displayPriority"), 0));
            entity.setCategoryData(categoryData);
        }
        return entity;
    }

    static ResourceGroupEntity resourceGroup(Map<String, Object> doc) {
        ResourceGroupEntity entity = new ResourceGroupEntity();
        if (!applyBasic(entity, doc)) {
            return null;
        }
        Map<String, Object> data = map(doc.get("resourceGroupData"));
        if (data != null) {
            ResourceGroupData groupData = new ResourceGroupData();
            groupData.setHumanReadableId(text(data.get("humanReadableId")));
            entity.setResourceGroupData(groupData);
        }
        ObjectId parentId = objectId(doc.get("refParentGroupId"));
        if (parentId != null) {
            ResourceGroupEntity stub = new ResourceGroupEntity();
            stub.setId(parentId);
            entity.setRefParentGroup(stub);
            entity.attachRefParentGroup(null);
        }
        return entity;
    }

    static ResourceEntity resource(Map<String, Object> doc) {
        ResourceEntity entity = new ResourceEntity();
        if (!applyBasic(entity, doc)) {
            return null;
        }
        entity.setResourceData(resourceData(map(doc.get("resourceData"))));
        entity.setPreviewData(resourceData(map(doc.get("previewData"))));
        ObjectId groupId = objectId(doc.get("refResourceGroupId"));
        if (groupId != null) {
            ResourceGroupEntity stub = new ResourceGroupEntity();
            stub.setId(groupId);
            entity.setRefResourceGroup(stub);
            entity.attachRefResourceGroup(null);
        }
        return entity;
    }

    static ItemEntity item(Map<String, Object> doc) {
        ItemEntity entity = new ItemEntity();
        if (!applyBasic(entity, doc)) {
            return null;
        }
        entity.setItemType(enumOrNull(ItemEntity.ItemType.class, doc.get("itemType")));
        entity.setUniqueUriName(text(doc.get("uniqueUriName")));
        ObjectId categoryId = objectId(doc.get("refCategoryId"));
        if (categoryId != null) {
            CategoryEntity stub = new CategoryEntity();
            stub.setId(categoryId);
            entity.setRefCategory(stub);
            entity.attachRefCategory(null);
        }
        entity.setPageData(pageData(map(doc.get("pageData"))));
        entity.setAlbumData(albumData(map(doc.get("albumData"))));
        return entity;
    }

    static PlaylistEntity playlist(Map<String, Object> doc) {
        PlaylistEntity entity = new PlaylistEntity();
        if (!applyBasic(entity, doc)) {
            return null;
        }
        entity.setPlaylistId(text(doc.get("playlistId")));
        Map<String, Object> data = map(doc.get("playlistData"));
        if (data != null) {
            PlaylistData playlistData = new PlaylistData();
            playlistData.setTitle(text(data.get("title")));
            playlistData.setDescription(text(data.get("description")));
            List<PlaylistTrack> tracks = new ArrayList<>();
            for (Object row : list(data.get("tracks"))) {
                Map<String, Object> trackDoc = map(row);
                if (trackDoc == null) {
                    continue;
                }
                PlaylistTrack track = new PlaylistTrack();
                ObjectId resourceId = objectId(trackDoc.get("refResourceId"));
                if (resourceId != null) {
                    ResourceEntity stub = new ResourceEntity();
                    stub.setId(resourceId);
                    track.setRefResource(stub);
                    track.attachRefResource(null);
                }
                track.setTitle(text(trackDoc.get("title")));
                track.setArtist(text(trackDoc.get("artist")));
                tracks.add(track);
            }
            playlistData.setTracks(tracks);
            entity.setPlaylistData(playlistData);
        }
        return entity;
    }

    static AppEntity app(Map<String, Object> doc) {
        AppEntity entity = new AppEntity();
        if (!applyBasic(entity, doc)) {
            return null;
        }
        Map<String, Object> data = map(doc.get("appData"));
        if (data != null) {
            AppData appData = new AppData();
            appData.setSlug(text(data.get("slug")));
            appData.setOriginalFilename(text(data.get("originalFilename")));
            appData.setSizeInBytes(longValue(data.get("sizeInBytes"), 0));
            appData.setContentRaw(text(data.get("contentRaw")));
            appData.setLargeFile(bool(data.get("largeFile"), false));
            appData.setDataChunkIds(objectIdList(data.get("dataChunkIds")));
            appData.setAppName(text(data.get("appName")));
            appData.setAppVersion(text(data.get("appVersion")));
            appData.setAuthor(text(data.get("author")));
            appData.setCompany(text(data.get("company")));
            appData.setDescription(text(data.get("description")));
            appData.setStoreEnabled(bool(data.get("storeEnabled"), false));
            appData.setStoreOpen(bool(data.get("storeOpen"), false));
            AppStoreSettings imported = AppStoreSettings.fromMap(map(data.get("storeSettings")));
            if (imported != null) {
                appData.applyStoreSettings(imported);
            }
            appData.setStoreTables(AppStoreTableSpec.listFrom(data.get("storeTables")));
            entity.setAppData(appData);
        }
        return entity;
    }

    static ThemeEntity theme(Map<String, Object> doc) {
        ThemeEntity entity = new ThemeEntity();
        if (!applyBasic(entity, doc)) {
            return null;
        }
        Map<String, Object> data = map(doc.get("themeData"));
        if (data != null) {
            ThemeData themeData = new ThemeData();
            themeData.setThemeId(text(data.get("themeId")));
            themeData.setOriginalFilename(text(data.get("originalFilename")));
            themeData.setSizeInBytes(longValue(data.get("sizeInBytes"), 0));
            themeData.setContentRaw(text(data.get("contentRaw")));
            themeData.setLargeFile(bool(data.get("largeFile"), false));
            themeData.setDataChunkIds(objectIdList(data.get("dataChunkIds")));
            themeData.setTitle(text(data.get("title")));
            themeData.setAuthor(text(data.get("author")));
            themeData.setShell(text(data.get("shell")));
            themeData.setDefaultSchema(text(data.get("defaultSchema")));
            themeData.setSchemas(stringList(data.get("schemas")));
            themeData.setSortOrder(intValue(data.get("sortOrder"), 0));
            entity.setThemeData(themeData);
        }
        return entity;
    }

    static DataChunkEntity chunk(Map<String, Object> doc) {
        DataChunkEntity entity = new DataChunkEntity();
        if (!applyBasic(entity, doc)) {
            return null;
        }
        entity.setData(text(doc.get("data")));
        return entity;
    }

    static SettingContextEntity setting(Map<String, Object> doc) {
        if (doc == null) {
            return null;
        }
        String context = text(doc.get("context"));
        if (context == null || context.isBlank()) {
            return null;
        }
        SettingContextEntity entity = new SettingContextEntity();
        ObjectId id = objectId(doc.get("id"));
        if (id != null) {
            entity.setId(id);
        }
        entity.setContext(context);
        HashMap<String, String> values = stringHash(doc.get("values"));
        entity.setValues(values == null ? new HashMap<>() : values);
        return entity;
    }

    static boolean looksLikeBson(Object node) {
        if (node instanceof Map<?, ?> map) {
            for (Object key : map.keySet()) {
                String name = String.valueOf(key);
                if ("$oid".equals(name) || "$date".equals(name) || "_class".equals(name)) {
                    return true;
                }
            }
            for (Object value : map.values()) {
                if (looksLikeBson(value)) {
                    return true;
                }
            }
        } else if (node instanceof List<?> list) {
            for (Object value : list) {
                if (looksLikeBson(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean applyBasic(BasicEntity entity, Map<String, Object> doc) {
        if (doc == null) {
            return false;
        }
        ObjectId id = objectId(doc.get("id"));
        if (id == null) {
            return false;
        }
        entity.setId(id);
        entity.setEntityVersion(text(doc.get("entityVersion")));
        entity.setSecurityData(security(map(doc.get("securityData"))));
        entity.setHistoryData(history(map(doc.get("historyData"))));
        return true;
    }

    private static SecurityData security(Map<String, Object> doc) {
        if (doc == null) {
            return null;
        }
        Map<AccessType, AccessRule> access = new LinkedHashMap<>();
        Map<String, Object> settings = map(doc.get("accessSettings"));
        if (settings != null) {
            for (Map.Entry<String, Object> entry : settings.entrySet()) {
                AccessType type = enumOrNull(AccessType.class, entry.getKey());
                AccessRule rule = accessRule(entry.getValue());
                if (type != null && rule != null) {
                    access.put(type, rule);
                }
            }
        }
        SecurityData data = new SecurityData();
        if (!access.isEmpty()) {
            data.setAccessSettings(access);
        }
        return data;
    }

    private static AccessRule accessRule(Object raw) {
        if (raw == null) {
            return AccessRule.inheritAll();
        }
        Map<String, Object> object = map(raw);
        if (object != null) {
            AccessRule rule = new AccessRule();
            rule.setInherit(bool(object.get("inherit"), true));
            List<String> roleIds = new ArrayList<>();
            for (Object id : list(object.get("roleIds"))) {
                if (id != null && !id.toString().isBlank()) {
                    roleIds.add(id.toString().trim());
                }
            }
            rule.setRoleIds(roleIds);
            List<String> accountIds = new ArrayList<>();
            for (Object id : list(object.get("accountIds"))) {
                if (id != null && !id.toString().isBlank()) {
                    accountIds.add(id.toString().trim());
                }
            }
            rule.setAccountIds(accountIds);
            if (object.get("legacyThreshold") != null) {
                rule.setLegacyThreshold(object.get("legacyThreshold").toString());
            }
            return rule;
        }
        SecurityLevel level = enumOrNull(SecurityLevel.class, raw);
        if (level == null) {
            return AccessRule.inheritAll();
        }
        return AccessRule.fromLegacy(level);
    }

    private static HistoryData history(Map<String, Object> doc) {
        if (doc == null) {
            return null;
        }
        List<?> rows = list(doc.get("events"));
        Event[] events = new Event[rows.size()];
        int index = 0;
        for (Object row : rows) {
            Map<String, Object> eventDoc = map(row);
            Event event = new Event();
            if (eventDoc != null) {
                event.setEventType(enumOrNull(EventType.class, eventDoc.get("eventType")));
                event.setLocalDateTime(time(eventDoc.get("localDateTime")));
                ObjectId ownerId = objectId(eventDoc.get("ownerId"));
                if (ownerId != null) {
                    AccountEntity stub = new AccountEntity();
                    stub.setId(ownerId);
                    event.setOwner(stub);
                    event.attachOwner(null);
                }
            }
            events[index++] = event;
        }
        HistoryData data = new HistoryData();
        data.setEvents(events);
        return data;
    }

    private static ResourceData resourceData(Map<String, Object> doc) {
        if (doc == null) {
            return null;
        }
        ResourceData data = new ResourceData();
        data.setType(enumOrNull(ResourceType.class, doc.get("type")));
        data.setSizeInBytes(longValue(doc.get("sizeInBytes"), 0));
        data.setPathPublic(text(doc.get("pathPublic")));
        data.setPathProtected(text(doc.get("pathProtected")));
        data.setImageDescription(text(doc.get("imageDescription")));
        data.setContentRaw(text(doc.get("contentRaw")));
        data.setContentPreview(text(doc.get("contentPreview")));
        data.setLargeFile(bool(doc.get("largeFile"), false));
        data.setDataChunkIds(objectIdList(doc.get("dataChunkIds")));
        HashMap<String, String> metadata = stringHash(doc.get("metadata"));
        if (metadata != null) {
            data.setMetadata(metadata);
        }
        String waveform = text(doc.get("waveform"));
        if (waveform != null && !waveform.isBlank()) {
            try {
                data.setWaveform(Base64.getDecoder().decode(waveform));
            } catch (IllegalArgumentException ignored) {
                // keep waveform unset
            }
        }
        return data;
    }

    private static PageData pageData(Map<String, Object> doc) {
        if (doc == null) {
            return null;
        }
        PageData data = new PageData();
        data.setTitle(text(doc.get("title")));
        data.setSubHeader(text(doc.get("subHeader")));
        data.setHeader(text(doc.get("header")));
        data.setDescription(text(doc.get("description")));
        data.setTags(stringList(doc.get("tags")));
        ObjectId imageId = objectId(doc.get("refImageId"));
        if (imageId != null) {
            ResourceEntity stub = new ResourceEntity();
            stub.setId(imageId);
            data.setRefImage(stub);
            data.attachRefImage(null);
        }
        List<PageData.Comment> comments = new ArrayList<>();
        for (Object row : list(doc.get("comments"))) {
            Map<String, Object> commentDoc = map(row);
            if (commentDoc == null) {
                continue;
            }
            PageData.Comment comment = new PageData.Comment();
            ObjectId authorId = objectId(commentDoc.get("authorId"));
            if (authorId != null) {
                AccountEntity stub = new AccountEntity();
                stub.setId(authorId);
                comment.setAuthor(stub);
                comment.attachAuthor(null);
            }
            comment.setPublicationTime(time(commentDoc.get("publicationTime")));
            comment.setMessage(text(commentDoc.get("message")));
            comments.add(comment);
        }
        if (!comments.isEmpty() || doc.containsKey("comments")) {
            data.setComments(comments);
        }
        return data;
    }

    private static AlbumData albumData(Map<String, Object> doc) {
        if (doc == null) {
            return null;
        }
        AlbumData data = new AlbumData();
        data.setTitle(text(doc.get("title")));
        data.setSubHeader(text(doc.get("subHeader")));
        data.setHeader(text(doc.get("header")));
        data.setDescription(text(doc.get("description")));
        data.setTags(stringList(doc.get("tags")));
        ObjectId groupId = objectId(doc.get("refResourceGroupId"));
        if (groupId != null) {
            ResourceGroupEntity stub = new ResourceGroupEntity();
            stub.setId(groupId);
            data.setRefResourceGroup(stub);
            data.attachRefResourceGroup(null);
        }
        return data;
    }

    static ObjectId objectId(Object value) {
        if (value == null) {
            return null;
        }
        String hex = value.toString().trim();
        if (!ObjectId.isValid(hex)) {
            return null;
        }
        return new ObjectId(hex);
    }

    private static List<ObjectId> objectIdList(Object value) {
        List<ObjectId> ids = new ArrayList<>();
        for (Object element : list(value)) {
            ObjectId id = objectId(element);
            if (id != null) {
                ids.add(id);
            }
        }
        return ids.isEmpty() ? null : ids;
    }

    private static List<String> stringList(Object value) {
        if (value == null) {
            return null;
        }
        List<String> out = new ArrayList<>();
        for (Object element : list(value)) {
            if (element != null) {
                out.add(element.toString());
            }
        }
        return out;
    }

    private static HashMap<String, String> stringHash(Object value) {
        Map<String, Object> map = map(value);
        if (map == null) {
            return null;
        }
        HashMap<String, String> out = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            out.put(entry.getKey(), entry.getValue() == null ? null : entry.getValue().toString());
        }
        return out;
    }

    private static Map<String, Object> map(Object value) {
        if (!(value instanceof Map<?, ?> raw)) {
            return null;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (entry.getKey() != null) {
                out.put(entry.getKey().toString(), entry.getValue());
            }
        }
        return out;
    }

    private static List<?> list(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }

    private static boolean bool(Object value, boolean fallback) {
        if (value instanceof Boolean flag) {
            return flag;
        }
        if (value == null) {
            return fallback;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private static int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static long longValue(Object value, long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static LocalDateTime time(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        if (text.endsWith("Z") || text.endsWith("z")) {
            text = text.substring(0, text.length() - 1);
        }
        try {
            return LocalDateTime.parse(text, TIME);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static <E extends Enum<E>> E enumOrNull(Class<E> type, Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.toString().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

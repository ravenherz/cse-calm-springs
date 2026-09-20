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
import com.ravenherz.cse.dal.dto.RoleEntity;
import com.ravenherz.cse.dal.dto.RoleMatrixDocument;
import com.ravenherz.cse.dal.dto.ThemeEntity;
import com.ravenherz.cse.dal.dto.UrlTemplateEntity;
import com.ravenherz.cse.dal.dto.basic.AccessRule;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.dto.basic.AlbumData;
import com.ravenherz.cse.dal.dto.basic.AppAccountGrant;
import com.ravenherz.cse.dal.dto.basic.AppData;
import com.ravenherz.cse.dal.dto.basic.CategoryData;
import com.ravenherz.cse.dal.dto.basic.Event;
import com.ravenherz.cse.dal.dto.basic.HistoryData;
import com.ravenherz.cse.dal.dto.basic.PageData;
import com.ravenherz.cse.dal.dto.basic.PlaylistData;
import com.ravenherz.cse.dal.dto.basic.PlaylistTrack;
import com.ravenherz.cse.dal.dto.basic.ResourceData;
import com.ravenherz.cse.dal.dto.basic.ResourceGroupData;
import com.ravenherz.cse.dal.dto.basic.RoleGrant;
import com.ravenherz.cse.dal.dto.basic.SecurityData;
import com.ravenherz.cse.dal.dto.basic.ThemeData;
import com.ravenherz.cse.dal.dto.basic.UrlTemplateData;
import com.ravenherz.cse.dal.dto.basic.enums.AccessType;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class CseSiteDocuments {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private CseSiteDocuments() {
    }

    static Map<String, Object> account(AccountEntity entity) {
        Map<String, Object> doc = basic(entity);
        AccountData data = entity.getAccountData();
        if (data == null) {
            return doc;
        }
        Map<String, Object> accountData = new LinkedHashMap<>();
        put(accountData, "login", data.getLogin());
        put(accountData, "hash", data.getHash());
        put(accountData, "emailAddress", data.getEmailAddress());
        put(accountData, "bio", data.getBio());
        put(accountData, "shownName", data.getShownName());
        put(accountData, "avatar", data.getAvatar());
        if (data.getLevel() != null) {
            accountData.put("level", data.getLevel().name());
        }
        put(accountData, "roleId", data.getRoleId());
        put(accountData, "contacts", data.getContacts());
        put(accountData, "extensibleData", data.getExtensibleData());
        put(accountData, "stylesTheme", data.getStylesTheme());
        put(accountData, "stylesSchema", data.getStylesSchema());
        accountData.put("loginable", data.isLoginable());
        put(accountData, "activationToken", data.getActivationToken());
        doc.put("accountData", accountData);
        return doc;
    }

    static Map<String, Object> category(CategoryEntity entity) {
        Map<String, Object> doc = basic(entity);
        CategoryData data = entity.getCategoryData();
        if (data == null) {
            return doc;
        }
        Map<String, Object> categoryData = new LinkedHashMap<>();
        put(categoryData, "itemName", data.getItemName());
        put(categoryData, "navigationTitle", data.getNavigationTitle());
        put(categoryData, "navigationDescription", data.getNavigationDescription());
        categoryData.put("visible", data.isVisible());
        categoryData.put("active", data.isActive());
        categoryData.put("displayCount", data.getDisplayCount());
        categoryData.put("displayPriority", data.getDisplayPriority());
        doc.put("categoryData", categoryData);
        return doc;
    }

    static Map<String, Object> resourceGroup(ResourceGroupEntity entity) {
        Map<String, Object> doc = basic(entity);
        ResourceGroupData data = entity.getResourceGroupData();
        if (data != null) {
            Map<String, Object> groupData = new LinkedHashMap<>();
            put(groupData, "humanReadableId", data.getHumanReadableId());
            doc.put("resourceGroupData", groupData);
        }
        put(doc, "refParentGroupId", hex(entity.refParentGroupObjectId()));
        return doc;
    }

    static Map<String, Object> resource(ResourceEntity entity) {
        Map<String, Object> doc = basic(entity);
        put(doc, "resourceData", resourceData(entity.getResourceData()));
        put(doc, "previewData", resourceData(entity.getPreviewData()));
        put(doc, "refResourceGroupId", hex(entity.refResourceGroupObjectId()));
        return doc;
    }

    static Map<String, Object> item(ItemEntity entity) {
        Map<String, Object> doc = basic(entity);
        if (entity.getItemType() != null) {
            doc.put("itemType", entity.getItemType().name());
        }
        put(doc, "uniqueUriName", entity.getUniqueUriName());
        put(doc, "refCategoryId", hex(entity.getRefCategoryId()));
        put(doc, "pageData", pageData(entity.getPageData()));
        put(doc, "albumData", albumData(entity.getAlbumData()));
        return doc;
    }

    static Map<String, Object> playlist(PlaylistEntity entity) {
        Map<String, Object> doc = basic(entity);
        put(doc, "playlistId", entity.getPlaylistId());
        PlaylistData data = entity.getPlaylistData();
        if (data != null) {
            Map<String, Object> playlistData = new LinkedHashMap<>();
            put(playlistData, "title", data.getTitle());
            put(playlistData, "description", data.getDescription());
            put(playlistData, "refImageId", hex(data.getRefImageId()));
            List<Map<String, Object>> tracks = new ArrayList<>();
            for (PlaylistTrack track : data.getTracks()) {
                if (track == null) {
                    continue;
                }
                Map<String, Object> row = new LinkedHashMap<>();
                put(row, "refResourceId", hex(track.getRefResourceId()));
                put(row, "title", track.getTitle());
                put(row, "artist", track.getArtist());
                tracks.add(row);
            }
            playlistData.put("tracks", tracks);
            doc.put("playlistData", playlistData);
        }
        return doc;
    }

    static Map<String, Object> urlTemplate(UrlTemplateEntity entity) {
        Map<String, Object> doc = basic(entity);
        put(doc, "urlTemplateId", entity.getUrlTemplateId());
        UrlTemplateData data = entity.getUrlTemplateData();
        if (data != null) {
            Map<String, Object> templateData = new LinkedHashMap<>();
            put(templateData, "urlImage", data.getUrlImage());
            put(templateData, "urlDefaultText", data.getUrlDefaultText());
            put(templateData, "urlPattern", data.getUrlPattern());
            doc.put("urlTemplateData", templateData);
        }
        return doc;
    }

    static Map<String, Object> app(AppEntity entity) {
        Map<String, Object> doc = basic(entity);
        AppData data = entity.getAppData();
        if (data == null) {
            return doc;
        }
        Map<String, Object> appData = new LinkedHashMap<>();
        put(appData, "slug", data.getSlug());
        put(appData, "originalFilename", data.getOriginalFilename());
        appData.put("sizeInBytes", data.getSizeInBytes());
        put(appData, "contentRaw", data.getContentRaw());
        appData.put("largeFile", data.isLargeFile());
        put(appData, "dataChunkIds", hexList(data.getDataChunkIds()));
        put(appData, "appName", data.getAppName());
        put(appData, "appVersion", data.getAppVersion());
        put(appData, "author", data.getAuthor());
        put(appData, "company", data.getCompany());
        put(appData, "description", data.getDescription());
        appData.put("storeEnabled", data.isStoreEnabled());
        appData.put("storeOpen", data.isStoreOpen());
        appData.put("storeSettings", data.storeSettings().toMap());
        List<Map<String, Object>> tables = new ArrayList<>();
        for (var spec : data.getStoreTables()) {
            if (spec != null) {
                tables.add(spec.toMap());
            }
        }
        if (!tables.isEmpty()) {
            appData.put("storeTables", tables);
        }
        doc.put("appData", appData);
        return doc;
    }

    static Map<String, Object> theme(ThemeEntity entity) {
        Map<String, Object> doc = basic(entity);
        ThemeData data = entity.getThemeData();
        if (data == null) {
            return doc;
        }
        Map<String, Object> themeData = new LinkedHashMap<>();
        put(themeData, "themeId", data.getThemeId());
        put(themeData, "originalFilename", data.getOriginalFilename());
        themeData.put("sizeInBytes", data.getSizeInBytes());
        put(themeData, "contentRaw", data.getContentRaw());
        themeData.put("largeFile", data.isLargeFile());
        put(themeData, "dataChunkIds", hexList(data.getDataChunkIds()));
        put(themeData, "title", data.getTitle());
        put(themeData, "author", data.getAuthor());
        put(themeData, "shell", data.getShell());
        put(themeData, "defaultSchema", data.getDefaultSchema());
        put(themeData, "schemas", data.getSchemas());
        themeData.put("sortOrder", data.getSortOrder());
        doc.put("themeData", themeData);
        return doc;
    }

    static Map<String, Object> chunk(DataChunkEntity entity) {
        Map<String, Object> doc = basic(entity);
        put(doc, "data", entity.getData());
        return doc;
    }

    /**
     * Whole {@code cse-datachunks} row from BSON so Morphia leftovers still keep {@code data}.
     */
    static Map<String, Object> chunkDocument(Document bson) {
        Map<String, Object> doc = new LinkedHashMap<>();
        if (bson == null) {
            return doc;
        }
        put(doc, "id", bsonId(bson.get("_id")));
        Object version = bson.get("entityVersion");
        if (version != null) {
            doc.put("entityVersion", version.toString());
        }
        put(doc, "data", chunkBytes(bson.get("data")));
        return doc;
    }

    static Map<String, Object> setting(String context, Map<String, String> values) {
        Map<String, Object> doc = new LinkedHashMap<>();
        put(doc, "context", context);
        Map<String, String> copy = new LinkedHashMap<>();
        if (values != null) {
            copy.putAll(values);
        }
        doc.put("values", copy);
        return doc;
    }

    static List<ObjectId> chunkIds(ResourceEntity resource) {
        List<ObjectId> ids = new ArrayList<>();
        addChunkIds(ids, resource == null ? null : resource.getResourceData());
        addChunkIds(ids, resource == null ? null : resource.getPreviewData());
        return ids;
    }

    static List<ObjectId> chunkIds(AppEntity app) {
        List<ObjectId> ids = new ArrayList<>();
        if (app != null && app.getAppData() != null) {
            addChunkIds(ids, app.getAppData().getDataChunkIds());
        }
        return ids;
    }

    static List<ObjectId> chunkIds(ThemeEntity theme) {
        List<ObjectId> ids = new ArrayList<>();
        if (theme != null && theme.getThemeData() != null) {
            addChunkIds(ids, theme.getThemeData().getDataChunkIds());
        }
        return ids;
    }

    static Map<String, Object> role(RoleEntity entity) {
        Map<String, Object> doc = new LinkedHashMap<>();
        put(doc, "id", hex(entity.getId()));
        put(doc, "entityVersion", entity.getEntityVersion());
        put(doc, "slug", entity.getSlug());
        put(doc, "name", entity.getName());
        put(doc, "system", entity.getSystem());
        doc.put("loginable", entity.isLoginable());
        doc.put("sortOrder", entity.getSortOrder());
        doc.put("archived", entity.isArchived());
        return doc;
    }

    static Map<String, Object> roleMatrix(RoleMatrixDocument entity) {
        Map<String, Object> doc = new LinkedHashMap<>();
        put(doc, "id", hex(entity.getId()));
        List<Map<String, Object>> grants = new ArrayList<>();
        for (RoleGrant grant : entity.getGrants()) {
            if (grant == null) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            put(row, "capabilityId", grant.getCapabilityId());
            put(row, "roleId", grant.getRoleId());
            grants.add(row);
        }
        doc.put("grants", grants);
        List<Map<String, Object>> appGrants = new ArrayList<>();
        for (AppAccountGrant grant : entity.getAppGrants()) {
            if (grant == null) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            put(row, "capabilityId", grant.getCapabilityId());
            put(row, "accountId", grant.getAccountId());
            appGrants.add(row);
        }
        doc.put("appGrants", appGrants);
        return doc;
    }

    private static Map<String, Object> basic(BasicEntity entity) {
        Map<String, Object> doc = new LinkedHashMap<>();
        put(doc, "id", hex(entity.getId()));
        put(doc, "entityVersion", entity.getEntityVersion());
        put(doc, "securityData", security(entity.getSecurityData()));
        put(doc, "historyData", history(entity.getHistoryData()));
        return doc;
    }

    private static Map<String, Object> security(SecurityData data) {
        if (data == null || data.getAccessSettings() == null) {
            return null;
        }
        Map<String, Object> access = new LinkedHashMap<>();
        for (Map.Entry<AccessType, AccessRule> entry : data.getAccessSettings().entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            AccessRule rule = entry.getValue();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("inherit", rule.isInherit());
            row.put("roleIds", new ArrayList<>(rule.getRoleIds()));
            row.put("accountIds", new ArrayList<>(rule.getAccountIds()));
            access.put(entry.getKey().name(), row);
        }
        Map<String, Object> security = new LinkedHashMap<>();
        security.put("accessSettings", access);
        return security;
    }

    private static Map<String, Object> history(HistoryData data) {
        if (data == null || data.getEvents() == null) {
            return null;
        }
        List<Map<String, Object>> events = new ArrayList<>();
        for (Event event : data.getEvents()) {
            if (event == null) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            if (event.getEventType() != null) {
                row.put("eventType", event.getEventType().name());
            }
            put(row, "localDateTime", time(event.getLocalDateTime()));
            put(row, "ownerId", hex(event.getOwnerId()));
            events.add(row);
        }
        Map<String, Object> history = new LinkedHashMap<>();
        history.put("events", events);
        return history;
    }

    private static Map<String, Object> resourceData(ResourceData data) {
        if (data == null) {
            return null;
        }
        Map<String, Object> doc = new LinkedHashMap<>();
        if (data.getType() != null) {
            doc.put("type", data.getType().name());
        }
        doc.put("sizeInBytes", data.getSizeInBytes());
        put(doc, "pathPublic", data.getPathPublic());
        put(doc, "pathProtected", data.getPathProtected());
        put(doc, "imageDescription", data.getImageDescription());
        put(doc, "contentRaw", data.getContentRaw());
        put(doc, "contentPreview", data.getContentPreview());
        doc.put("largeFile", data.isLargeFile());
        put(doc, "dataChunkIds", hexList(data.getDataChunkIds()));
        put(doc, "metadata", data.getMetadata());
        if (data.getWaveform() != null && data.getWaveform().length > 0) {
            doc.put("waveform", Base64.getEncoder().encodeToString(data.getWaveform()));
        }
        return doc;
    }

    private static Map<String, Object> pageData(PageData data) {
        if (data == null) {
            return null;
        }
        Map<String, Object> doc = new LinkedHashMap<>();
        put(doc, "title", data.getTitle());
        put(doc, "subHeader", data.getSubHeader());
        put(doc, "header", data.getHeader());
        put(doc, "description", data.getDescription());
        put(doc, "tags", data.getTags());
        put(doc, "refImageId", hex(data.getRefImageId()));
        if (data.getComments() != null) {
            List<Map<String, Object>> comments = new ArrayList<>();
            for (PageData.Comment comment : data.getComments()) {
                if (comment == null) {
                    continue;
                }
                Map<String, Object> row = new LinkedHashMap<>();
                put(row, "authorId", hex(comment.getAuthorId()));
                put(row, "publicationTime", time(comment.getPublicationTime()));
                put(row, "message", comment.getMessage());
                comments.add(row);
            }
            doc.put("comments", comments);
        }
        return doc;
    }

    private static Map<String, Object> albumData(AlbumData data) {
        if (data == null) {
            return null;
        }
        Map<String, Object> doc = new LinkedHashMap<>();
        put(doc, "title", data.getTitle());
        put(doc, "subHeader", data.getSubHeader());
        put(doc, "header", data.getHeader());
        put(doc, "description", data.getDescription());
        put(doc, "tags", data.getTags());
        put(doc, "refResourceGroupId", hex(data.getRefResourceGroupId()));
        return doc;
    }

    private static void addChunkIds(List<ObjectId> into, ResourceData data) {
        if (data != null) {
            addChunkIds(into, data.getDataChunkIds());
        }
    }

    private static void addChunkIds(List<ObjectId> into, List<ObjectId> ids) {
        if (ids == null) {
            return;
        }
        for (ObjectId id : ids) {
            if (id != null) {
                into.add(id);
            }
        }
    }

    private static List<String> hexList(List<ObjectId> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        List<String> out = new ArrayList<>();
        for (ObjectId id : ids) {
            String hex = hex(id);
            if (hex != null) {
                out.add(hex);
            }
        }
        return out.isEmpty() ? null : out;
    }

    static String hex(ObjectId id) {
        return id == null ? null : id.toHexString();
    }

    private static String bsonId(Object id) {
        if (id instanceof ObjectId oid) {
            return oid.toHexString();
        }
        return id == null ? null : id.toString();
    }

    private static String chunkBytes(Object data) {
        if (data == null) {
            return null;
        }
        if (data instanceof String text) {
            return text;
        }
        if (data instanceof byte[] bytes) {
            return Base64.getEncoder().encodeToString(bytes);
        }
        if (data instanceof Binary binary) {
            return Base64.getEncoder().encodeToString(binary.getData());
        }
        return data.toString();
    }

    static String time(LocalDateTime value) {
        return value == null ? null : TIME.format(value) + "Z";
    }

    private static void put(Map<String, Object> doc, String key, Object value) {
        if (value != null) {
            doc.put(key, value);
        }
    }
}

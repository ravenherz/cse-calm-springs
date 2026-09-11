package com.ravenherz.cse.dal.impl;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.dal.ConfigSource;
import com.ravenherz.cse.dal.DataProvider;
import com.ravenherz.cse.dal.EntityVersions;
import com.ravenherz.cse.dal.MongoTimeConversions;
import com.ravenherz.cse.dal.ReferenceHydrator;
import com.ravenherz.cse.dal.SettingsDocuments;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.AppEntity;
import com.ravenherz.cse.dal.dto.ThemeEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.PlaylistEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.RoleEntity;
import com.ravenherz.cse.dal.dto.SettingContextEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.callback.EntityCallbacks;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.bson.Document;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexField;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service("dataProvider")
public class DataProviderImpl implements DataProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataProviderImpl.class);

    private final ConfigSource config;
    private final SettingsDocuments settingsDocuments;
    private volatile MongoTemplate mongoTemplate;
    private volatile FormMongo formMongo;

    public DataProviderImpl(ConfigSource config, SettingsDocuments settingsDocuments) {
        this.config = config;
        this.settingsDocuments = settingsDocuments;
        EntityVersions.bind(config);
    }

    @Override
    public boolean usesEnvironmentCredentials() {
        return envOrProp("CSE_MONGODB_URI", "cse.mongodb.uri") != null
                || envOrProp("CSE_MONGO_ADDRESS", "cse.mongo.address") != null
                || envOrProp("CSE_MONGO_USER", "cse.mongo.user") != null;
    }

    @Override
    public boolean hasFormCredentials() {
        return formMongo != null;
    }

    @Override
    public void useFormCredentials(String type, String address, String port, String dbname,
            String user, String password) {
        synchronized (this) {
            this.formMongo = new FormMongo(
                    firstNonBlank(type, "mongodb"),
                    address,
                    firstNonBlank(port, "27017"),
                    dbname,
                    user,
                    password);
            this.mongoTemplate = null;
        }
    }

    @Override
    public boolean ping() {
        try {
            getMongoTemplate().getDb().runCommand(new Document("ping", 1));
            return true;
        } catch (RuntimeException ex) {
            LOGGER.warn("Mongo ping failed: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public MongoTemplate getMongoTemplate() {
        MongoTemplate local = mongoTemplate;
        if (local == null) {
            synchronized (this) {
                local = mongoTemplate;
                if (local == null) {
                    local = connect();
                    mongoTemplate = local;
                }
            }
        }
        return local;
    }

    private MongoTemplate connect() {
        FormMongo form = formMongo;
        String dbname = firstNonBlank(
                envOrProp("CSE_MONGO_DBNAME", "cse.mongo.dbname"),
                form == null ? null : form.dbname,
                config.getValue(SettingKeys.CONTEXT_DATASOURCE_DBMS_INSTANCE, SettingKeys.KEY_DBMS_DBNAME));
        String uri = envOrProp("CSE_MONGODB_URI", "cse.mongodb.uri");
        MongoClient mongoClient;
        if (uri != null) {
            mongoClient = createClient(uri);
            if (dbname == null || dbname.isBlank()) {
                dbname = dbNameFromUri(uri);
            }
        } else if (form != null) {
            mongoClient = createClient(mongoConnectionUri(form.type, form.user, form.password,
                    form.address, form.port, dbname));
        } else {
            String type = firstNonBlank(
                    envOrProp("CSE_MONGO_TYPE", "cse.mongo.type"),
                    config.getValue(SettingKeys.CONTEXT_DATASOURCE_DBMS_INSTANCE, SettingKeys.KEY_DBMS_TYPE));
            String username = firstNonBlank(
                    envOrProp("CSE_MONGO_USER", "cse.mongo.user"),
                    config.getValue(SettingKeys.CONTEXT_DATASOURCE_DBMS_ACCESS, SettingKeys.KEY_DBMS_ACCESS_USER));
            String password = firstNonBlank(
                    envOrProp("CSE_MONGO_PASSWORD", "cse.mongo.password"),
                    config.getValue(SettingKeys.CONTEXT_DATASOURCE_DBMS_ACCESS, SettingKeys.KEY_DBMS_ACCESS_PSWD));
            String address = firstNonBlank(
                    envOrProp("CSE_MONGO_ADDRESS", "cse.mongo.address"),
                    config.getValue(SettingKeys.CONTEXT_DATASOURCE_DBMS_INSTANCE, SettingKeys.KEY_DBMS_ADDRESS));
            String port = firstNonBlank(
                    envOrProp("CSE_MONGO_PORT", "cse.mongo.port"),
                    config.getValue(SettingKeys.CONTEXT_DATASOURCE_DBMS_INSTANCE, SettingKeys.KEY_DBMS_PORT));
            mongoClient = createClient(
                    mongoConnectionUri(type, username, password, address, port, dbname));
        }
        SimpleMongoClientDatabaseFactory factory = new SimpleMongoClientDatabaseFactory(mongoClient, dbname);
        MongoCustomConversions conversions = MongoTimeConversions.create();
        MongoMappingContext mappingContext = new MongoMappingContext();
        mappingContext.setAutoIndexCreation(false);
        mappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        mappingContext.afterPropertiesSet();
        MappingMongoConverter converter = new MappingMongoConverter(new DefaultDbRefResolver(factory), mappingContext);
        converter.setCustomConversions(conversions);
        converter.setCodecRegistryProvider(factory);
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        converter.afterPropertiesSet();
        MongoTemplate template = new MongoTemplate(factory, converter);
        ReferenceHydrator hydrator = new ReferenceHydrator(template);
        template.setEntityCallbacks(EntityCallbacks.create(
                (AfterConvertCallback<Object>) (entity, document, collection) -> {
                    hydrator.hydrate(entity);
                    return entity;
                }));
        ensureIndexes(template);
        settingsDocuments.bindMongoTemplate(template);
        return template;
    }

    private static void ensureIndexes(MongoTemplate mongo) {
        ensureIndex(mongo, ItemEntity.class,
                new Index().on("uniqueUriName", Sort.Direction.ASC).unique());
        ensureIndex(mongo, AccountEntity.class,
                new Index().on("accountData.login", Sort.Direction.ASC));
        ensureIndex(mongo, AccountEntity.class,
                new Index().on("accountData.emailAddress", Sort.Direction.ASC));
        ensureIndex(mongo, SettingContextEntity.class,
                new Index().on("context", Sort.Direction.ASC).unique());
        ensureIndex(mongo, AppEntity.class,
                new Index().on("appData.slug", Sort.Direction.ASC));
        ensureIndex(mongo, ThemeEntity.class,
                new Index().on("themeData.themeId", Sort.Direction.ASC));
        ensureIndex(mongo, ResourceEntity.class,
                new Index().on("resourceData.pathPublic", Sort.Direction.ASC));
        ensureIndex(mongo, ResourceEntity.class,
                new Index().on("resourceData.pathProtected", Sort.Direction.ASC));
        ensureIndex(mongo, PlaylistEntity.class,
                new Index().on("playlistId", Sort.Direction.ASC).unique());
        ensureIndex(mongo, RoleEntity.class,
                new Index().on("slug", Sort.Direction.ASC).unique());
    }

    private static void ensureIndex(MongoTemplate mongo, Class<?> type, Index index) {
        IndexOperations ops = mongo.indexOps(type);
        Document requestedKeys = index.getIndexKeys();
        for (IndexInfo existing : ops.getIndexInfo()) {
            if (sameKeyPattern(existing, requestedKeys)) {
                return;
            }
        }
        ops.createIndex(index);
    }

    static boolean sameKeyPattern(IndexInfo existing, Document requestedKeys) {
        if (existing.getIndexFields().size() != requestedKeys.size()) {
            return false;
        }
        for (IndexField field : existing.getIndexFields()) {
            Object spec = requestedKeys.get(field.getKey());
            if (!(spec instanceof Number)) {
                return false;
            }
            int requestedDir = ((Number) spec).intValue();
            int existingDir = field.getDirection() == Sort.Direction.DESC ? -1 : 1;
            if (requestedDir != existingDir) {
                return false;
            }
        }
        return true;
    }

    static String mongoConnectionUri(String type, String username, String password,
            String address, String port, String dbname) {
        String rawAddress = address == null ? "" : address.trim();
        if (looksLikeMongoUri(rawAddress)) {
            return rawAddress;
        }
        String scheme = type == null || type.isBlank() ? "mongodb" : type.trim();
        if (looksLikeMongoUri(scheme)) {
            scheme = scheme.toLowerCase(Locale.ROOT).startsWith("mongodb+srv") ? "mongodb+srv" : "mongodb";
        }
        boolean srv = "mongodb+srv".equalsIgnoreCase(scheme);
        String hostPort = srv ? srvHost(rawAddress) : hostAndPort(rawAddress, port);
        if (hostPort.startsWith(":") || hostPort.isEmpty()) {
            throw new IllegalArgumentException("Mongo address is empty");
        }
        String authSource = encodeUserInfo(dbname == null ? "" : dbname);
        if (username == null || username.isBlank()) {
            return scheme + "://" + hostPort + "/?authSource=" + authSource;
        }
        return scheme + "://" + encodeUserInfo(username) + ":" + encodeUserInfo(
                password == null ? "" : password) + "@" + hostPort + "/?authSource=" + authSource;
    }

    static boolean looksLikeMongoUri(String value) {
        if (value == null) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("mongodb://") || lower.startsWith("mongodb+srv://");
    }

    static String hostAndPort(String address, String port) {
        String host = address == null ? "" : address.trim();
        String portPart = port == null ? "" : port.trim();
        if (host.startsWith("[")) {
            int close = host.indexOf(']');
            if (close > 0) {
                String rest = host.substring(close + 1);
                host = host.substring(0, close + 1);
                if (portPart.isEmpty() && rest.startsWith(":")) {
                    portPart = rest.substring(1);
                }
            }
        } else {
            int colon = host.lastIndexOf(':');
            if (colon > 0 && host.indexOf(':') == colon) {
                String after = host.substring(colon + 1);
                if (!after.isEmpty() && after.chars().allMatch(Character::isDigit)) {
                    if (portPart.isEmpty()) {
                        portPart = after;
                    }
                    host = host.substring(0, colon);
                }
            }
        }
        if (portPart.isEmpty()) {
            portPart = "27017";
        }
        return host + ":" + portPart;
    }

    private static String srvHost(String address) {
        String host = address == null ? "" : address.trim();
        int colon = host.lastIndexOf(':');
        if (colon > 0 && host.indexOf(':') == colon) {
            String after = host.substring(colon + 1);
            if (!after.isEmpty() && after.chars().allMatch(Character::isDigit)) {
                host = host.substring(0, colon);
            }
        }
        return host;
    }

    private static String encodeUserInfo(String value) {
        return UriUtils.encodeUserInfo(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static MongoClient createClient(String uri) {
        return MongoClients.create(MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .applyToClusterSettings(builder -> builder.serverSelectionTimeout(8, TimeUnit.SECONDS))
                .build());
    }

    private static String envOrProp(String env, String prop) {
        String value = System.getenv(env);
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        value = System.getProperty(prop);
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    static String dbNameFromUri(String uri) {
        if (uri == null) {
            return null;
        }
        int scheme = uri.indexOf("://");
        String rest = scheme >= 0 ? uri.substring(scheme + 3) : uri;
        int slash = rest.indexOf('/');
        if (slash < 0) {
            return null;
        }
        String path = rest.substring(slash + 1);
        int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        return path.isBlank() ? null : path;
    }

    private static final class FormMongo {
        private final String type;
        private final String address;
        private final String port;
        private final String dbname;
        private final String user;
        private final String password;

        private FormMongo(String type, String address, String port, String dbname,
                String user, String password) {
            this.type = type;
            this.address = address;
            this.port = port;
            this.dbname = dbname;
            this.user = user;
            this.password = password;
        }
    }
}

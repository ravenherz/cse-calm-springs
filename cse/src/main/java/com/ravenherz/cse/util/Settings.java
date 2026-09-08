package com.ravenherz.cse.util;

import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.constants.Strings;
import com.ravenherz.cse.controller.objects.FormDescription;
import com.ravenherz.cse.dal.ConfigSource;
import com.ravenherz.cse.dal.SettingsDocuments;
import com.ravenherz.cse.dal.dto.SettingContextEntity;
import com.ravenherz.cse.util.io.ServletFile;
import com.ravenherz.cse.util.io.CseDisk;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.DependsOn;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component("settings")
@DependsOn("cseDiskBinder")
public class Settings implements Serializable, ConfigSource, SettingsDocuments {

    private List<FormDescription> formDescriptions;

    private ServletContext servletContext;
    private static final Logger LOGGER = LoggerFactory.getLogger(Settings.class);

    @Autowired
    public void setServletContext(ServletContext servletContextImpl) {
        servletContext = servletContextImpl;
    }

    // VARIABLES NEEDED IN CASE OF MISCONFIGURATION
    public final static int DEFAULT_NUMBER_OF_COLUMS_NAV_FOOTER = 3;

    private String path = null;
    /*
     A collection of 'context's
     */
    private Map<String, Map<String, String>> storage;
    private MongoTemplate mongoTemplate;

    /**
     * Private constructor which loads all json-datasources from configuration directory from filesystem
     */

    @PostConstruct
    private void postConstruct() {
        path = servletContext.getRealPath("");
        storage = new HashMap<>();
        formDescriptions = new ArrayList<>();
        loadFormDescriptions();
        loadConfigurationJson();
        loadSecretsFromDisk();
    }

    private void loadFormDescriptions() {
        try {
            for (Resource resource : ServletFile.list("/static/content-private/forms/", "*")) {
                String name = resource.getFilename();
                if (name == null || !resource.isReadable() || !name.endsWith(Strings.EXTENSION_JSON)) {
                    continue;
                }
                try (InputStream in = resource.getInputStream()) {
                    FormDescription formDescription = Json.read(in, FormDescription.class);
                    if (formDescription != null) {
                        formDescriptions.add(formDescription);
                    }
                } catch (IOException e) {
                    LOGGER.warn("Cannot read form description {}", name, e);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Cannot list form descriptions", e);
        }
    }

    private void loadConfigurationJson() {
        try {
            for (Resource resource : ServletFile.list(Strings.PATH_CONFIGURATION, "*")) {
                String name = resource.getFilename();
                if (name == null || !resource.isReadable() || skipClasspathConfig(name)) {
                    continue;
                }
                try (InputStream in = resource.getInputStream()) {
                    storage.put(name.replace(Strings.EXTENSION_JSON, Strings.STR_EMPTY),
                            new HashMap<>(Json.stringMap(in)));
                } catch (IOException e) {
                    LOGGER.warn("Cannot read settings file {}", name, e);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Cannot list configuration files", e);
        }
    }

    /**
     * Get filesystem context path
     *
     * @return context path as a String
     */
    public String getPath() {
        return path;
    }

	/*
		So, first of all, we are trying to get a 'context' from 'storage'
		Then we are trying to get 'value' by 'key' from 'context'
		If the reference is not null then we are trimming it to find out
		if value is a string with whitespaces, and if it actually contains
		some date we return the 'value' to user.
	 */

    /**
     * Returns 'value' defined by 'key' from some 'context' dal source
     *
     * @param context context, dal source (usually json in configuration folder)
     * @param key a key to find value by
     * @return value defined by key
     */
    @Override
    public String getValue(String context, String key) {
        String result = storage
                .getOrDefault(context, new HashMap<>())
                .getOrDefault(key, null);
        if (result == null) return null;
        else return result.trim();
    }


	/*
		If context is null we create a new hashmap,
		anyway we put key:value pair in existed or newly created context
	 */

    /**
     * Put 'value' defined by 'key' into some 'context' dal source
     *
     * @param context context, dal source (usually json in configuration folder; a new one, or already existing)
     * @param key key (a new one, or already existing)
     * @param value new value
     */
    public void putValue(String context, String key, String value) {
        storage.computeIfAbsent(context, k -> new HashMap<>());
        storage.get(context).put(key, value == null ? "" : value);
    }

    public boolean hasContext(String context) {
        return context != null && storage != null && storage.containsKey(context);
    }

    public Map<String, Map<String, String>> getStorageSnapshot() {
        Map<String, Map<String, String>> snapshot = new TreeMap<>();
        if (storage == null) {
            return snapshot;
        }
        for (Map.Entry<String, Map<String, String>> context : storage.entrySet()) {
            if (SettingKeys.isSecretContext(context.getKey())) {
                continue;
            }
            Map<String, String> keys = new TreeMap<>();
            if (context.getValue() != null) {
                for (Map.Entry<String, String> entry : context.getValue().entrySet()) {
                    keys.put(entry.getKey(), stringify(entry.getValue()));
                }
            }
            snapshot.put(context.getKey(), keys);
        }
        return snapshot;
    }

    /**
     * DBMS settings overlay only (not WAR classpath JSON, not secret-dbms files).
     */
    public Map<String, Map<String, String>> listPersistedContexts() {
        Map<String, Map<String, String>> overlay = new TreeMap<>();
        if (mongoTemplate == null) {
            return overlay;
        }
        try {
            mongoTemplate.findAll(SettingContextEntity.class).forEach(document -> {
                if (document == null || document.getContext() == null || skipMongo(document.getContext())) {
                    return;
                }
                Map<String, String> ordered = new TreeMap<>();
                if (document.getValues() != null) {
                    for (Map.Entry<String, String> entry : document.getValues().entrySet()) {
                        if (entry.getKey() == null) {
                            continue;
                        }
                        ordered.put(entry.getKey(), stringify(entry.getValue()));
                    }
                }
                overlay.put(document.getContext(), ordered);
            });
        } catch (Exception e) {
            LOGGER.error("Could not list persisted settings for export", e);
        }
        return overlay;
    }

    public boolean isOverlayContext(String context) {
        return context != null && !skipMongo(context);
    }

    public void reloadFromMongo() {
        applyMongoOverrides();
    }

    public void bindMongoTemplate(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
        applyMongoOverrides();
    }

    private void applyMongoOverrides() {
        if (mongoTemplate == null || storage == null) {
            return;
        }
        try {
            mongoTemplate.findAll(SettingContextEntity.class).forEach(document -> {
                if (document == null || document.getContext() == null
                        || skipMongo(document.getContext()) || document.getValues() == null) {
                    return;
                }
                for (Map.Entry<String, String> entry : document.getValues().entrySet()) {
                    if (entry.getKey() == null) {
                        continue;
                    }
                    putValue(document.getContext(), entry.getKey(), stringify(entry.getValue()));
                }
                LOGGER.info("Applied Mongo settings overrides for " + document.getContext());
            });
        } catch (Exception e) {
            LOGGER.error("Could not load settings from Mongo; using WAR defaults", e);
        }
    }

    public boolean persistContext(String context) {
        if (context == null || !context.matches("[A-Za-z0-9_-]+")) {
            LOGGER.error("Refusing to persist invalid settings context name");
            return false;
        }
        if (skipMongo(context)) {
            return true;
        }
        if (storage == null || !storage.containsKey(context)) {
            return false;
        }
        if (mongoTemplate == null) {
            LOGGER.error("Cannot persist settings, Mongo is not ready: " + context);
            return false;
        }
        Map<String, String> data = storage.get(context);
        if (data == null) {
            return false;
        }
        Map<String, String> ordered = new TreeMap<>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            ordered.put(entry.getKey(), stringify(entry.getValue()));
        }
        try {
            SettingContextEntity document = mongoTemplate.findOne(
                    Query.query(Criteria.where("context").is(context)), SettingContextEntity.class);
            if (document == null) {
                document = new SettingContextEntity();
                document.setContext(context);
            }
            document.setValues(ordered);
            mongoTemplate.save(document);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to persist settings context " + context, e);
            return false;
        }
    }

    private boolean skipMongo(String context) {
        return SettingKeys.CONTEXT_DATASOURCE_BUILD_INFO.equals(context)
                || SettingKeys.isSecretContext(context)
                || "config-dbms-instance".equals(context)
                || "config-dbms-access".equals(context);
    }

    private boolean skipClasspathConfig(String filename) {
        String context = filename.endsWith(Strings.EXTENSION_JSON)
                ? filename.substring(0, filename.length() - Strings.EXTENSION_JSON.length())
                : filename;
        return SettingKeys.isSecretContext(context)
                || "config-dbms-instance".equals(context)
                || "config-dbms-access".equals(context);
    }

    public void loadSecretsFromDisk() {
        loadSecretFile(SettingKeys.CONTEXT_DATASOURCE_DBMS_INSTANCE);
        loadSecretFile(SettingKeys.CONTEXT_DATASOURCE_DBMS_ACCESS);
    }

    public boolean persistSecretsToDisk() {
        if (!CseDisk.ensureInstanceConfigurationWritable()) {
            LOGGER.warn("Instance configuration dir is not writable");
            return false;
        }
        try {
            writeSecretFile(SettingKeys.CONTEXT_DATASOURCE_DBMS_INSTANCE);
            writeSecretFile(SettingKeys.CONTEXT_DATASOURCE_DBMS_ACCESS);
            return true;
        } catch (IOException ex) {
            LOGGER.warn("Could not write secret DBMS JSON: {}", ex.getMessage());
            return false;
        }
    }

    private void loadSecretFile(String context) {
        File file = CseDisk.secretFile(context + Strings.EXTENSION_JSON);
        if (!file.isFile()) {
            return;
        }
        try (InputStream in = new java.io.FileInputStream(file)) {
            storage.put(context, new HashMap<>(Json.stringMap(in)));
        } catch (IOException e) {
            LOGGER.warn("Cannot read secret settings file {}", file.getAbsolutePath(), e);
        }
    }

    private void writeSecretFile(String context) throws IOException {
        Map<String, String> data = storage.get(context);
        if (data == null) {
            return;
        }
        Map<String, String> ordered = new TreeMap<>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            ordered.put(entry.getKey(), stringify(entry.getValue()));
        }
        File file = CseDisk.secretFile(context + Strings.EXTENSION_JSON);
        Json.MAPPER.writeValue(file, ordered);
        CseDisk.restrictOwnerOnly(file);
    }

    private String stringify(Object raw) {
        return raw == null ? "" : String.valueOf(raw);
    }

	/*
		If locally defined path is null the application is not ready.
	 */

    public List<FormDescription> getFormDescriptions() {
        return formDescriptions;
    }
}

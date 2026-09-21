package com.ravenherz.cse.install;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.core.SiteConfigured;
import com.ravenherz.cse.dal.DataProvider;
import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.security.InstanceConfigured;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Derived site readiness: DBMS binding + owner, cached after success.
 * Finish and a successful boot against an already-owned database write
 * {@code content-private/.site-ready} so a later Mongo blip does not reopen setup.
 * While the installer is exploded this JVM, owner exists is not enough — Finish must run.
 * Marker plus a reachable empty account list reopens setup (failed install / empty import).
 * {@code CSE_FORCE_SETUP=true} reopens setup even when accounts exist.
 */
@Component
public class SiteReady implements InstanceConfigured, SiteConfigured {

    private static final Logger LOGGER = LoggerFactory.getLogger(SiteReady.class);
    private static final ObjectMapper JSON = new JsonMapper();
    private static final String JSON_EXT = ".json";

    private final DataProvider dataProvider;
    private final ServiceProvider serviceProvider;
    private final InstallDisk disk;

    private volatile Boolean cachedReady;
    private volatile Boolean cachedMongoReady;
    private volatile boolean installerHeldOpen;

    public SiteReady(DataProvider dataProvider, ServiceProvider serviceProvider, InstallDisk disk) {
        this.dataProvider = dataProvider;
        this.serviceProvider = serviceProvider;
        this.disk = disk;
    }

    public boolean hasDbmsBinding() {
        return dataProvider.usesEnvironmentCredentials()
                || dataProvider.hasFormCredentials()
                || secretFilesPresent();
    }

    public boolean isConfigured() {
        if (Boolean.TRUE.equals(cachedReady) && !installerHeldOpen) {
            return true;
        }
        if (forceSetup()) {
            return false;
        }
        if (!hasDbmsBinding()) {
            return false;
        }
        if (disk.siteReadyMarkerPresent()) {
            if (installerHeldOpen) {
                return false;
            }
            cachedReady = true;
            return true;
        }
        if (installerHeldOpen) {
            return false;
        }
        if (!mongoReadyCached()) {
            return false;
        }
        if (Boolean.TRUE.equals(ownerPresent())) {
            disk.writeSiteReadyMarker();
            cachedReady = true;
            return true;
        }
        return false;
    }

    /**
     * Boot-time check: owned site with binding, or a previous Finish marker.
     * Does not set {@link #holdInstallerOpen()}.
     */
    public boolean evaluateAtBoot() {
        if (forceSetup()) {
            LOGGER.warn("CSE_FORCE_SETUP is set; keeping the installer open");
            cachedReady = false;
            return false;
        }
        if (!hasDbmsBinding()) {
            return false;
        }
        if (disk.siteReadyMarkerPresent()) {
            if (!pingAndCache()) {
                cachedReady = true;
                cachedMongoReady = true;
                return true;
            }
            Boolean owner = ownerPresent();
            if (Boolean.FALSE.equals(owner)) {
                LOGGER.warn("Site-ready marker exists but Mongo has no accounts; reopening setup");
                disk.deleteSiteReadyMarker();
                cachedReady = false;
                return false;
            }
            cachedReady = true;
            return true;
        }
        if (!pingAndCache()) {
            return false;
        }
        if (Boolean.TRUE.equals(ownerPresent())) {
            disk.writeSiteReadyMarker();
            cachedReady = true;
            return true;
        }
        return false;
    }

    public void holdInstallerOpen() {
        installerHeldOpen = true;
        cachedReady = false;
    }

    public void markFinished() {
        installerHeldOpen = false;
        disk.writeSiteReadyMarker();
        cachedReady = true;
        cachedMongoReady = true;
    }

    public void noteMongoReady() {
        cachedMongoReady = true;
    }

    /**
     * After CMS import (or any bulk account change): drop a stale ready marker
     * when Mongo is empty so setup can run again.
     */
    public void refreshAfterDataChange() {
        cachedMongoReady = null;
        cachedReady = null;
        Boolean owner = ownerPresent();
        if (Boolean.TRUE.equals(owner)) {
            installerHeldOpen = false;
            disk.writeSiteReadyMarker();
            cachedReady = true;
            cachedMongoReady = true;
            return;
        }
        if (Boolean.FALSE.equals(owner)) {
            LOGGER.warn("No accounts after data change; reopening setup");
            installerHeldOpen = true;
            disk.deleteSiteReadyMarker();
            cachedReady = false;
        }
    }

    public static boolean forceSetup() {
        return truthy(envOrProp("CSE_FORCE_SETUP", "cse.force.setup"));
    }

    private boolean mongoReadyCached() {
        if (Boolean.TRUE.equals(cachedMongoReady)) {
            return true;
        }
        return pingAndCache();
    }

    private boolean pingAndCache() {
        try {
            if (dataProvider.ping()) {
                cachedMongoReady = true;
                return true;
            }
        } catch (RuntimeException ex) {
            LOGGER.warn("Mongo ping failed while evaluating site ready: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * {@code true} / {@code false} when the list succeeds; {@code null} if Mongo errors
     * so a blip does not look like an empty site.
     */
    private Boolean ownerPresent() {
        try {
            List<AccountEntity> accounts = serviceProvider.getAccountService().getAllAccounts();
            return accounts != null && !accounts.isEmpty();
        } catch (RuntimeException ex) {
            LOGGER.warn("Could not list accounts while evaluating site ready: {}", ex.getMessage());
            return null;
        }
    }

    private boolean secretFilesPresent() {
        File instance = disk.secretFile(SettingKeys.CONTEXT_DATASOURCE_DBMS_INSTANCE + JSON_EXT);
        File access = disk.secretFile(SettingKeys.CONTEXT_DATASOURCE_DBMS_ACCESS + JSON_EXT);
        return parsesInstance(instance) && parsesAccess(access);
    }

    private static boolean parsesInstance(File file) {
        Map<String, String> map = readMap(file);
        if (map == null) {
            return false;
        }
        String address = map.get(SettingKeys.KEY_DBMS_ADDRESS);
        String dbname = map.get(SettingKeys.KEY_DBMS_DBNAME);
        return address != null && !address.isBlank() && dbname != null && !dbname.isBlank();
    }

    private static boolean parsesAccess(File file) {
        return readMap(file) != null;
    }

    private static Map<String, String> readMap(File file) {
        if (file == null || !file.isFile()) {
            return null;
        }
        try (InputStream in = new FileInputStream(file)) {
            return stringMap(in);
        } catch (IOException ex) {
            return null;
        }
    }

    private static Map<String, String> stringMap(InputStream in) throws IOException {
        JsonNode node = JSON.readTree(in);
        Map<String, String> out = new HashMap<>();
        if (node == null || !node.isObject()) {
            return out;
        }
        node.properties().forEach(entry -> {
            JsonNode value = entry.getValue();
            if (value == null || value.isNull()) {
                out.put(entry.getKey(), "");
            } else if (value.isTextual()) {
                out.put(entry.getKey(), value.asText());
            } else {
                out.put(entry.getKey(), value.toString());
            }
        });
        return out;
    }

    private static boolean truthy(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String trimmed = value.trim();
        return "1".equals(trimmed) || "true".equalsIgnoreCase(trimmed) || "yes".equalsIgnoreCase(trimmed);
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
}

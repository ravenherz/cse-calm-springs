package com.ravenherz.cse.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ravenherz.cse.dal.dao.AppStoreService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.AppEntity;
import com.ravenherz.cse.dal.dto.basic.AppStoreSettings;
import com.ravenherz.cse.store.AppStoreAccess;
import com.ravenherz.cse.store.AppStoreAcl;
import com.ravenherz.cse.store.AppStoreDocument;
import com.ravenherz.cse.store.AppStoreException;
import com.ravenherz.cse.store.AppStoreRateLimiter;
import com.ravenherz.cse.store.AppStoreTableSpec;
import com.ravenherz.cse.engine.util.Json;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/app-data")
public class AppDataController {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};

    private final AppStoreService appStores;
    private final AuthSupport authSupport;
    private final AppStoreRateLimiter rateLimiter;

    public AppDataController(AppStoreService appStores, AuthSupport authSupport,
            AppStoreRateLimiter rateLimiter) {
        this.appStores = appStores;
        this.authSupport = authSupport;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping(value = "/{slug}/_schema", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> schema(@PathVariable String slug, HttpServletRequest request,
            HttpServletResponse response) {
        return guarded(false, slug, request, () -> {
            AppEntity app = appStores.requireApp(slug);
            if (!app.getAppData().isStoreEnabled()) {
                throw new AppStoreException(403, "Data store is not enabled");
            }
            AccountEntity accessor = authSupport.getAccessor(request, response);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("slug", app.getAppData().getSlug());
            var store = app.getAppData().storeSettings();
            body.put("storeEnabled", true);
            body.put("storeOpen", store.isSchemaOpen());
            body.put("maxDataBytes", store.resolvedMaxDataBytes());
            body.put("maxDocs", store.resolvedMaxDocs());
            body.put("maxBytes", store.resolvedMaxBytes());
            body.put("admin", AppStoreAcl.isSiteAdmin(accessor));
            List<Map<String, Object>> tables = new ArrayList<>();
            for (AppStoreTableSpec spec : app.getAppData().getStoreTables()) {
                tables.add(spec.toMap());
            }
            body.put("tables", tables);
            return ResponseEntity.ok(body);
        });
    }

    @PutMapping(value = "/{slug}/_schema/{table:[a-z][a-z0-9]{0,31}}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> defineTable(@PathVariable String slug, @PathVariable String table,
            @RequestBody(required = false) String body, HttpServletRequest request,
            HttpServletResponse response) {
        return guarded(true, slug, request, () -> {
            AppEntity app = appStores.requireApp(slug);
            if (!app.getAppData().isStoreEnabled()) {
                throw new AppStoreException(403, "Data store is not enabled");
            }
            AccountEntity accessor = authSupport.getAccessor(request, response);
            if (!AppStoreAcl.canDefineSchema(accessor, app.getAppData().isStoreOpen())) {
                throw deny(accessor, "Not allowed to define tables");
            }
            Map<String, Object> json = parseObject(body);
            Object accessRaw = json.get("access");
            AppStoreAccess access = AppStoreAccess.parse(
                    accessRaw == null ? null : accessRaw.toString());
            Map<String, Object> schema = nestedMap(json.get("schema"));
            AppStoreTableSpec spec = new AppStoreTableSpec(table, access, schema);
            appStores.defineTable(slug, spec);
            return ResponseEntity.ok(spec.toMap());
        });
    }

    @PutMapping(value = "/{slug}/_grant", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> grant(@PathVariable String slug,
            @RequestBody(required = false) String body, HttpServletRequest request,
            HttpServletResponse response) {
        return guarded(true, slug, request, () -> {
            AccountEntity accessor = authSupport.getAccessor(request, response);
            if (!AppStoreAcl.isSiteAdmin(accessor)) {
                throw deny(accessor, "Not allowed");
            }
            Map<String, Object> json = parseObject(body);
            AppEntity app = appStores.requireApp(slug);
            AppStoreSettings settings = AppStoreSettings.copyOf(app.getAppData().storeSettings());
            settings.setEnabled(bool(json.get("storeEnabled"), settings.isEnabled()));
            settings.setSchemaOpen(bool(json.get("storeOpen"), settings.isSchemaOpen()));
            if (json.get("maxDataBytes") instanceof Number dataBytes) {
                settings.setMaxDataBytes(dataBytes.intValue());
            }
            if (json.get("maxDocs") instanceof Number docs) {
                settings.setMaxDocs(docs.intValue());
            }
            if (json.get("maxBytes") instanceof Number bytes) {
                settings.setMaxBytes(bytes.longValue());
            }
            appStores.updateStoreSettings(slug, settings);
            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "storeEnabled", settings.isEnabled(),
                    "storeOpen", settings.isSchemaOpen(),
                    "maxDataBytes", settings.resolvedMaxDataBytes(),
                    "maxDocs", settings.resolvedMaxDocs(),
                    "maxBytes", settings.resolvedMaxBytes()));
        });
    }

    @GetMapping(value = "/{slug}/{table:[a-z][a-z0-9]{0,31}}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> list(@PathVariable String slug, @PathVariable String table,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "after", required = false) String after,
            @RequestParam(value = "mine", required = false) String mine,
            HttpServletRequest request, HttpServletResponse response) {
        return guarded(false, slug, request, () -> {
            AppStoreTableSpec spec = appStores.requireTable(slug, table);
            AccountEntity accessor = authSupport.getAccessor(request, response);
            if (!AppStoreAcl.canReadList(spec.getAccess(), accessor)) {
                throw deny(accessor, "Not allowed to list this table");
            }
            boolean mineOnly = "1".equals(mine) || "true".equalsIgnoreCase(mine);
            if (mineOnly && !AppStoreAcl.isMember(accessor)) {
                throw deny(accessor, "Sign in to list your rows");
            }
            boolean filterMine = mineOnly || !AppStoreAcl.listSeesAll(spec.getAccess(), accessor);
            String ownerId = filterMine ? AppStoreAcl.ownerId(accessor) : null;
            AppStoreService.AppStoreList page = appStores.list(slug, table, ownerId, filterMine,
                    after, limit == null ? 50 : limit);
            Map<String, Object> body = new LinkedHashMap<>();
            List<Map<String, Object>> items = new ArrayList<>();
            for (AppStoreDocument document : page.items()) {
                items.add(document.toMap());
            }
            body.put("items", items);
            body.put("after", page.after());
            return ResponseEntity.ok(body);
        });
    }

    @GetMapping(value = "/{slug}/{table:[a-z][a-z0-9]{0,31}}/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> get(@PathVariable String slug, @PathVariable String table,
            @PathVariable String id, HttpServletRequest request, HttpServletResponse response) {
        return guarded(false, slug, request, () -> {
            AppStoreTableSpec spec = appStores.requireTable(slug, table);
            AccountEntity accessor = authSupport.getAccessor(request, response);
            AppStoreDocument document = appStores.get(slug, table, id);
            if (!AppStoreAcl.canReadRow(spec.getAccess(), accessor, document.getOwnerId())) {
                throw deny(accessor, "Not allowed to read this document");
            }
            return ResponseEntity.ok(document.toMap());
        });
    }

    @PostMapping(value = "/{slug}/{table:[a-z][a-z0-9]{0,31}}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> insert(@PathVariable String slug, @PathVariable String table,
            @RequestBody(required = false) String body, HttpServletRequest request,
            HttpServletResponse response) {
        return guarded(true, slug, request, () -> {
            AppStoreTableSpec spec = appStores.requireTableOrCreate(slug, table);
            AccountEntity accessor = authSupport.getAccessor(request, response);
            if (!AppStoreAcl.canCreate(spec.getAccess(), accessor)) {
                throw deny(accessor, "Not allowed to write this table");
            }
            AppStoreDocument document = appStores.insert(slug, table, AppStoreAcl.ownerId(accessor),
                    extractData(parseObject(body)));
            return ResponseEntity.status(201).body(document.toMap());
        });
    }

    @PatchMapping(value = "/{slug}/{table:[a-z][a-z0-9]{0,31}}/{id}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> patch(@PathVariable String slug, @PathVariable String table,
            @PathVariable String id, @RequestBody(required = false) String body,
            HttpServletRequest request, HttpServletResponse response) {
        return guarded(true, slug, request, () -> {
            AppStoreTableSpec spec = appStores.requireTable(slug, table);
            AccountEntity accessor = authSupport.getAccessor(request, response);
            AppStoreDocument existing = appStores.get(slug, table, id);
            if (!AppStoreAcl.canWriteRow(spec.getAccess(), accessor, existing.getOwnerId())) {
                throw deny(accessor, "Not allowed to write this document");
            }
            AppStoreDocument document = appStores.patch(slug, table, id, extractData(parseObject(body)));
            return ResponseEntity.ok(document.toMap());
        });
    }

    @DeleteMapping(value = "/{slug}/{table:[a-z][a-z0-9]{0,31}}/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> delete(@PathVariable String slug, @PathVariable String table,
            @PathVariable String id, HttpServletRequest request, HttpServletResponse response) {
        return guarded(true, slug, request, () -> {
            AppStoreTableSpec spec = appStores.requireTable(slug, table);
            AccountEntity accessor = authSupport.getAccessor(request, response);
            AppStoreDocument existing = appStores.get(slug, table, id);
            if (!AppStoreAcl.canWriteRow(spec.getAccess(), accessor, existing.getOwnerId())) {
                throw deny(accessor, "Not allowed to delete this document");
            }
            appStores.delete(slug, table, id);
            return ResponseEntity.ok(Map.of("status", 200));
        });
    }

    private ResponseEntity<?> guarded(boolean write, String slug, HttpServletRequest request, Action action) {
        if (write) {
            String key = "app-data:" + authSupport.clientIp(request) + ":" + slug;
            if (!rateLimiter.allow(key)) {
                return ResponseEntity.status(429).body(error(429, "Too many writes. Try again later."));
            }
        }
        try {
            return action.run();
        } catch (AppStoreException ex) {
            return ResponseEntity.status(ex.getStatus()).body(error(ex.getStatus(), ex.getMessage()));
        }
    }

    private static AppStoreException deny(AccountEntity accessor, String message) {
        if (accessor == null) {
            return new AppStoreException(401, "Sign in required");
        }
        return new AppStoreException(403, message);
    }

    private static Map<String, Object> error(int status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("message", message);
        return body;
    }

    private static Map<String, Object> parseObject(String body) {
        if (body == null || body.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> parsed = Json.MAPPER.readValue(body, MAP);
            return parsed == null ? new LinkedHashMap<>() : parsed;
        } catch (Exception ex) {
            throw new AppStoreException(400, "Invalid JSON");
        }
    }

    private static Map<String, Object> extractData(Map<String, Object> body) {
        Object nested = body.get("data");
        if (nested instanceof Map<?, ?>) {
            return nestedMap(nested);
        }
        return nestedMap(body);
    }

    private static Map<String, Object> nestedMap(Object raw) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (!(raw instanceof Map<?, ?> map)) {
            return out;
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            String key = entry.getKey().toString();
            if ("id".equals(key) || "ownerId".equals(key)
                    || "createdAt".equals(key) || "updatedAt".equals(key)) {
                continue;
            }
            out.put(key, entry.getValue());
        }
        return out;
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

    @FunctionalInterface
    private interface Action {
        ResponseEntity<?> run();
    }
}

package com.ravenherz.cse.util.themes;

import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.dal.ConfigSource;
import com.ravenherz.cse.dal.dao.ThemeService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.ThemeEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@DependsOn("themeRootsBinder")
public class ThemeCatalog {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThemeCatalog.class);
    private static final String CLASSPATH_PREVIEW = "/static/content-public/themes/%s/preview.jpg";

    private final ConfigSource settings;
    private final ObjectProvider<ThemeService> themeService;
    private final ShippedThemes shipped;

    public ThemeCatalog(ConfigSource settings) {
        this(settings, null, (ShippedThemes) null);
    }

    public ThemeCatalog(ConfigSource settings, ObjectProvider<ThemeService> themeService, ShippedThemes shipped) {
        this.settings = settings;
        this.themeService = themeService;
        this.shipped = shipped == null ? List::of : shipped;
    }

    @Autowired
    public ThemeCatalog(ConfigSource settings, ObjectProvider<ThemeService> themeService,
            ObjectProvider<ShippedThemes> shippedThemes) {
        this.settings = settings;
        this.themeService = themeService;
        ShippedThemes resolved = shippedThemes == null ? null : shippedThemes.getIfAvailable();
        this.shipped = resolved == null ? List::of : resolved;
    }

    public List<ThemeInfo> list() {
        Map<String, ThemeInfo> byId = new LinkedHashMap<>();
        Set<String> shippedIds = new HashSet<>();
        for (ThemeInfo shipped : loadShipped()) {
            if (shipped.getId() != null) {
                shippedIds.add(shipped.getId().toLowerCase(Locale.ROOT));
            }
            byId.putIfAbsent(shipped.getId(), shipped);
        }
        ThemeService service = themes();
        for (ThemeInfo installed : loadInstalled()) {
            String installedId = installed.getId() == null ? "" : installed.getId().toLowerCase(Locale.ROOT);
            if (ThemePackDeployer.isReservedId(installedId) || shippedIds.contains(installedId)) {
                continue;
            }
            if (service != null && service.getByThemeId(installed.getId()) == null) {
                continue;
            }
            byId.putIfAbsent(installed.getId(), installed);
        }
        List<ThemeInfo> list = new ArrayList<>(byId.values());
        if (service != null) {
            for (int i = 0; i < list.size(); i++) {
                ThemeInfo info = list.get(i);
                if (info.isBuiltin()) {
                    continue;
                }
                ThemeEntity entity = service.getByThemeId(info.getId());
                if (entity != null && entity.getThemeData() != null) {
                    list.set(i, info.withSortOrder(entity.getThemeData().getSortOrder()));
                }
            }
        }
        list.sort(Comparator.comparingInt(ThemeInfo::getSortOrder)
                .thenComparing(ThemeInfo::getId, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    public ThemeInfo find(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        for (ThemeInfo info : list()) {
            if (id.equalsIgnoreCase(info.getId())) {
                return info;
            }
        }
        return null;
    }

    public boolean isKnownTheme(String id) {
        return find(id) != null;
    }

    public boolean isKnownSchema(String themeId, String schemaId) {
        ThemeInfo info = find(themeId);
        return info != null && info.hasSchema(schemaId);
    }

    public ThemeSelection resolve() {
        return resolve(null);
    }

    public ThemeSelection resolve(AccountEntity account) {
        ThemeInfo theme = find(settings.getValue(SettingKeys.CONTEXT_DATASOURCE_VIEW,
                SettingKeys.KEY_STYLES_THEME));
        if (theme == null) {
            theme = find(ThemeSelection.FALLBACK_THEME);
        }
        if (theme == null) {
            List<ThemeInfo> all = list();
            theme = all.isEmpty() ? null : all.get(0);
        }
        if (theme == null) {
            return ThemeSelection.fallback();
        }
        String schema = settings.getValue(SettingKeys.CONTEXT_DATASOURCE_VIEW, SettingKeys.KEY_STYLES_SCHEMA);
        if (!theme.hasSchema(schema)) {
            schema = theme.getDefaultSchema();
        }
        if (schema == null && !theme.getSchemas().isEmpty()) {
            schema = theme.getSchemas().get(0);
        }
        if (schema == null) {
            schema = ThemeSelection.FALLBACK_SCHEMA;
        }
        return new ThemeSelection(theme.getId(), schema, layoutShell(theme));
    }

    private List<ThemeInfo> loadShipped() {
        List<ThemeInfo> out = new ArrayList<>();
        ThemePackDeployer reader = new ThemePackDeployer();
        for (ShippedThemes.Pack pack : shipped.packs()) {
            ThemeManifest manifest = reader.readManifest(pack.bytes());
            if (manifest == null) {
                continue;
            }
            String id = firstNonBlank(manifest.getId(), pack.stem());
            if (id == null || id.isBlank()) {
                continue;
            }
            String shell = firstNonBlank(manifest.getShell(), isBuiltinShell(id) ? id : null);
            if (!isAllowedShell(shell)) {
                LOGGER.warn("Skipping shipped theme '{}': shell '{}' is not modern, 2000s, own, or js", id, shell);
                continue;
            }
            List<String> schemas = new ArrayList<>(ThemePackDeployer.schemasInZip(pack.bytes()));
            if (schemas.isEmpty()) {
                schemas.addAll(manifest.getSchemas());
            }
            if (schemas.isEmpty()) {
                LOGGER.warn("Skipping shipped theme '{}': no color schemas", id);
                continue;
            }
            String defaultSchema = manifest.getDefaultSchema();
            if (defaultSchema == null || !schemas.contains(defaultSchema)) {
                defaultSchema = defaultSchemaFor(id, schemas);
            }
            warnEngine(id, manifest);
            out.add(new ThemeInfo(id, firstNonBlank(manifest.getTitle(), id), manifest.getAuthor(),
                    manifest.getDescription(), shell, defaultSchema, schemas, true,
                    manifest.getSortOrder(),
                    ThemePackDeployer.zipHasPreview(pack.bytes()) || hasClasspathPreview(id)));
        }
        return out;
    }

    private List<ThemeInfo> loadInstalled() {
        List<ThemeInfo> out = new ArrayList<>();
        Path root;
        try {
            root = ThemeRoots.themesDir();
        } catch (IOException ex) {
            return out;
        }
        if (!Files.isDirectory(root)) {
            return out;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path dir : stream) {
                if (!Files.isDirectory(dir) || dir.getFileName().toString().startsWith(".")) {
                    continue;
                }
                ThemeInfo info = fromDisk(dir);
                if (info != null) {
                    out.add(info);
                }
            }
        } catch (IOException ex) {
            LOGGER.warn("Cannot list installed themes under {}", root, ex);
        }
        return out;
    }

    private ThemeInfo fromDisk(Path dir) {
        Path manifestFile = dir.resolve(ThemeManifest.FILE_NAME);
        Path styles = dir.resolve("css").resolve("styles.css");
        if (!Files.isRegularFile(manifestFile) || !Files.isRegularFile(styles)) {
            return null;
        }
        ThemeManifest manifest;
        try {
            manifest = ThemeManifest.parse(Files.readString(manifestFile, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            LOGGER.warn("Cannot read {}", manifestFile, ex);
            return null;
        }
        if (manifest == null) {
            return null;
        }
        String id = firstNonBlank(manifest.getId(), dir.getFileName().toString());
        if (!id.equals(dir.getFileName().toString())) {
            LOGGER.warn("Skipping theme folder '{}' because theme.json id is '{}'", dir.getFileName(), id);
            return null;
        }
        String shell = manifest.getShell();
        if (!isAllowedShell(shell)) {
            LOGGER.warn("Skipping installed theme '{}': shell '{}' is not modern, 2000s, own, or js", id, shell);
            return null;
        }
        List<String> schemas = listDiskSchemas(dir);
        if (schemas.isEmpty()) {
            return null;
        }
        String defaultSchema = manifest.getDefaultSchema();
        if (defaultSchema == null || !schemas.contains(defaultSchema)) {
            defaultSchema = schemas.get(0);
        }
        warnEngine(id, manifest);
        boolean preview = Files.isRegularFile(dir.resolve("preview.jpg"))
                || Files.isRegularFile(dir.resolve("preview.png"));
        return new ThemeInfo(id, firstNonBlank(manifest.getTitle(), id), manifest.getAuthor(),
                manifest.getDescription(), shell, defaultSchema, schemas, false,
                manifest.getSortOrder(), preview);
    }

    private List<String> listDiskSchemas(Path themeDir) {
        List<String> names = new ArrayList<>();
        Path dir = themeDir.resolve("css").resolve("color-schemas");
        if (!Files.isDirectory(dir)) {
            return names;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.css")) {
            for (Path file : stream) {
                String filename = file.getFileName().toString();
                names.add(filename.substring(0, filename.length() - 4));
            }
        } catch (IOException ex) {
            LOGGER.warn("Cannot list color schemas under {}", dir, ex);
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public static boolean isBuiltinShell(String id) {
        return "modern".equals(id) || "2000s".equals(id);
    }

    public static boolean isOwnShell(String shell) {
        return "own".equalsIgnoreCase(shell);
    }

    public static boolean isJsShell(String shell) {
        return "js".equalsIgnoreCase(shell);
    }

    public static boolean isCustomShell(String shell) {
        return isOwnShell(shell) || isJsShell(shell);
    }

    public static boolean isAllowedShell(String shell) {
        return isBuiltinShell(shell) || isCustomShell(shell);
    }

    private static String layoutShell(ThemeInfo theme) {
        String shell = theme.getShell();
        if (isCustomShell(shell)) {
            return theme.getId();
        }
        if (shell == null || shell.isBlank()) {
            return theme.isBuiltin() ? theme.getId() : ThemeSelection.FALLBACK_SHELL;
        }
        return shell;
    }

    private void warnEngine(String id, ThemeManifest manifest) {
        if (manifest == null || manifest.getEngine() == null || manifest.getEngine().isBlank()) {
            return;
        }
        String current = settings.getValue(SettingKeys.CONTEXT_DATASOURCE_BUILD_INFO,
                SettingKeys.KEY_TAG_SOFT_VERSION_VERSION);
        if (current != null && !current.isBlank() && !current.startsWith(manifest.getEngine())
                && !manifest.getEngine().equals(current)) {
            LOGGER.warn("Theme '{}' engine '{}' differs from CSE {}", id, manifest.getEngine(), current);
        }
    }

    private boolean hasClasspathPreview(String id) {
        return ThemeCatalog.class.getResource(String.format(CLASSPATH_PREVIEW, id)) != null;
    }

    private ThemeService themes() {
        return themeService == null ? null : themeService.getIfAvailable();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String defaultSchemaFor(String themeId, List<String> schemas) {
        if ("modern".equals(themeId) && schemas.contains("linen")) {
            return "linen";
        }
        if ("2000s".equals(themeId) && schemas.contains("snowy")) {
            return "snowy";
        }
        return schemas.isEmpty() ? ThemeSelection.FALLBACK_SCHEMA : schemas.get(0);
    }
}

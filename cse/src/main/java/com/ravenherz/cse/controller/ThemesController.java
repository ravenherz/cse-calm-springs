package com.ravenherz.cse.controller;

import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.dal.StoredIds;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.DataChunkEntity;
import com.ravenherz.cse.dal.dto.ThemeEntity;
import com.ravenherz.cse.dal.dto.basic.Event;
import com.ravenherz.cse.dal.dto.basic.HistoryData;
import com.ravenherz.cse.dal.dto.basic.ThemeData;
import com.ravenherz.cse.dal.dto.basic.enums.EventType;
import com.ravenherz.cse.present.EditorTree;
import com.ravenherz.cse.present.ResourceGroupIndex;
import com.ravenherz.cse.util.frontend.ShippedPackCatalog;
import com.ravenherz.cse.util.themes.ThemeCatalog;
import com.ravenherz.cse.util.themes.ThemeInfo;
import com.ravenherz.cse.util.themes.ThemeManifest;
import com.ravenherz.cse.util.themes.ThemePackDeployer;
import com.ravenherz.cse.util.themes.ThemeSelection;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/editor/themes")
public class ThemesController extends AbstractController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThemesController.class);

    @Autowired
    private ThemePackDeployer themePackDeployer;

    @Autowired
    private ThemeCatalog themeCatalog;

    @Autowired
    private ResourceGroupIndex resourceGroupIndex;

    @GetMapping
    public String list(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        response.sendRedirect(request.getContextPath() + EditorTree.THEMES_HREF);
        return null;
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "returnGroup", required = false) String returnGroup,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        if (file == null || file.isEmpty()) {
            return listWithError(request, response, ".csetheme file is required");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null
                || !originalFilename.toLowerCase(Locale.ROOT).endsWith(ThemePackDeployer.THEME_PACKAGE_EXT)) {
            return listWithError(request, response, "Only .csetheme files are accepted");
        }
        byte[] zipBytes = file.getBytes();
        try {
            themePackDeployer.validateZip(zipBytes);
        } catch (IOException ex) {
            return listWithError(request, response, ex.getMessage());
        }
        ThemeManifest manifest = themePackDeployer.readManifest(zipBytes);
        if (manifest == null || manifest.getId() == null || manifest.getId().isBlank()) {
            return listWithError(request, response, "theme.json must set id");
        }
        String normalizedId = manifest.getId().trim().toLowerCase(Locale.ROOT);
        try {
            themePackDeployer.validateId(normalizedId);
        } catch (IllegalArgumentException ex) {
            return listWithError(request, response,
                    "theme.json id must be lowercase letters, digits, and hyphens, and cannot replace a built-in theme");
        }
        if (!ThemeCatalog.isAllowedShell(manifest.getShell())) {
            return listWithError(request, response, "theme.json must set shell to modern, 2000s, own, or js");
        }

        String base64 = Base64.getEncoder().encodeToString(zipBytes);
        ThemeData themeData = new ThemeData();
        themeData.setThemeId(normalizedId);
        themeData.setOriginalFilename(originalFilename);
        themeData.setSizeInBytes(zipBytes.length);
        themeData.setTitle(manifest.getTitle());
        themeData.setAuthor(manifest.getAuthor());
        themeData.setShell(manifest.getShell());
        themeData.setDefaultSchema(manifest.getDefaultSchema());
        themeData.setSchemas(manifest.getSchemas());
        ThemeEntity existing = serviceProvider.getThemeService().getByThemeId(normalizedId);
        if (existing != null && existing.getThemeData() != null) {
            themeData.setSortOrder(existing.getThemeData().getSortOrder());
        } else {
            themeData.setSortOrder(manifest.getSortOrder() > 0 ? manifest.getSortOrder() : nextSortOrder());
        }
        if (base64.length() > ThemeData.CHUNK_SIZE) {
            themeData.setLargeFile(true);
            int chunkSize = ThemeData.CHUNK_SIZE;
            int totalChunks = (int) Math.ceil((double) base64.length() / chunkSize);
            for (int i = 0; i < totalChunks; i++) {
                int start = i * chunkSize;
                int end = Math.min(start + chunkSize, base64.length());
                DataChunkEntity chunk = new DataChunkEntity(base64.substring(start, end));
                serviceProvider.getThemeService().saveDataChunk(chunk);
                themeData.addDataChunkId(StoredIds.objectId(chunk.getId()));
            }
        } else {
            themeData.setLargeFile(false);
            themeData.setContentRaw(base64);
        }

        if (existing != null) {
            serviceProvider.getThemeService().deleteChunks(existing);
            existing.setThemeData(themeData);
            HistoryData historyData = existing.getHistoryData();
            if (historyData == null) {
                historyData = new HistoryData();
            }
            Event[] oldEvents = historyData.getEvents() == null ? new Event[0] : historyData.getEvents();
            Event[] newEvents = new Event[oldEvents.length + 1];
            System.arraycopy(oldEvents, 0, newEvents, 0, oldEvents.length);
            newEvents[oldEvents.length] = new Event(EventType.ENTITY_EDITED, LocalDateTime.now(), accessor == null ? null : accessor.getId());
            historyData.setEvents(newEvents);
            existing.setHistoryData(historyData);
            serviceProvider.getThemeService().replace(existing);
        } else {
            serviceProvider.getThemeService().insert(new ThemeEntity(themeData, accessor == null ? null : accessor.getId()));
        }

        try {
            themePackDeployer.deploy(normalizedId, zipBytes);
        } catch (Exception ex) {
            LOGGER.error("Failed to deploy theme pack '{}'", normalizedId, ex);
            resourceGroupIndex.contentChanged();
            return listWithError(request, response, "Saved in Mongo but disk deploy failed: " + ex.getMessage());
        }
        resourceGroupIndex.contentChanged();
        response.sendRedirect(request.getContextPath()
                + EditorTree.catalogReturnHref(returnGroup, EditorTree.THEMES_ID));
        return null;
    }

    @PostMapping("/activate")
    public String activate(@RequestParam("themeId") String themeId,
            @RequestParam(value = "returnGroup", required = false) String returnGroup,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        String normalizedId = themeId == null ? "" : themeId.trim().toLowerCase(Locale.ROOT);
        ThemeInfo info = themeCatalog.find(normalizedId);
        if (info == null) {
            return listWithError(request, response, "Unknown theme");
        }
        settings.putValue(SettingKeys.CONTEXT_DATASOURCE_VIEW, SettingKeys.KEY_STYLES_THEME, info.getId());
        String schema = info.getDefaultSchema();
        if (schema != null && !schema.isBlank()) {
            settings.putValue(SettingKeys.CONTEXT_DATASOURCE_VIEW, SettingKeys.KEY_STYLES_SCHEMA, schema);
        }
        settings.persistContext(SettingKeys.CONTEXT_DATASOURCE_VIEW);
        resourceGroupIndex.contentChanged();
        response.sendRedirect(request.getContextPath()
                + EditorTree.catalogReturnHref(returnGroup, EditorTree.THEMES_ID));
        return null;
    }

    @PostMapping("/reorder")
    public String reorder(@RequestParam("themeIds") List<String> themeIds,
            @RequestParam(value = "returnGroup", required = false) String returnGroup,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        int order = 100;
        for (String themeId : themeIds) {
            if (themeId == null || themeId.isBlank()) {
                continue;
            }
            ThemeEntity entity = serviceProvider.getThemeService().getByThemeId(themeId.trim().toLowerCase(Locale.ROOT));
            if (entity == null || entity.getThemeData() == null) {
                continue;
            }
            entity.getThemeData().setSortOrder(order);
            serviceProvider.getThemeService().replace(entity);
            order += 10;
        }
        resourceGroupIndex.contentChanged();
        response.sendRedirect(request.getContextPath()
                + EditorTree.catalogReturnHref(returnGroup, EditorTree.THEMES_ID));
        return null;
    }

    @PostMapping("/delete")
    public String delete(@RequestParam("themeId") String themeId,
            @RequestParam(value = "returnGroup", required = false) String returnGroup,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        String normalizedId = themeId == null ? "" : themeId.trim().toLowerCase(Locale.ROOT);
        if (ThemePackDeployer.isReservedId(normalizedId) || ShippedPackCatalog.isShippedTheme(normalizedId)) {
            return listWithError(request, response, "Cannot delete a bundled theme");
        }
        try {
            themePackDeployer.validateId(normalizedId);
        } catch (IllegalArgumentException ex) {
            return listWithError(request, response, "Invalid theme id");
        }
        ThemeSelection active = themeCatalog.resolve();
        if (normalizedId.equalsIgnoreCase(active.getCssId())) {
            return listWithError(request, response, "Cannot delete the active theme. Switch styles-theme first.");
        }
        ThemeEntity existing = serviceProvider.getThemeService().getByThemeId(normalizedId);
        if (existing != null) {
            serviceProvider.getThemeService().delete(existing);
            resourceGroupIndex.contentChanged();
        }
        try {
            themePackDeployer.undeploy(normalizedId);
        } catch (Exception ex) {
            LOGGER.warn("Failed to remove disk tree for theme '{}'", normalizedId, ex);
        }
        response.sendRedirect(request.getContextPath()
                + EditorTree.catalogReturnHref(returnGroup, EditorTree.THEMES_ID));
        return null;
    }

    private String listWithError(HttpServletRequest request, HttpServletResponse response, String error)
            throws IOException {
        response.sendRedirect(request.getContextPath()
                + EditorTree.withNotice(EditorTree.THEMES_HREF, error));
        return null;
    }

    private int nextSortOrder() {
        int max = 90;
        for (ThemeEntity theme : serviceProvider.getThemeService().getAllThemes()) {
            if (theme.getThemeData() != null) {
                max = Math.max(max, theme.getThemeData().getSortOrder());
            }
        }
        return max + 10;
    }
}

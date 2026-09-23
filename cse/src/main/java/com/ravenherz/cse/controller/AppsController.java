package com.ravenherz.cse.controller;

import com.ravenherz.cse.dal.StoredIds;
import com.ravenherz.cse.dal.dao.AppStoreService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.AppEntity;
import com.ravenherz.cse.dal.dto.DataChunkEntity;
import com.ravenherz.cse.dal.dto.basic.AppData;
import com.ravenherz.cse.dal.dto.basic.AppStoreSettings;
import com.ravenherz.cse.dal.dto.basic.Event;
import com.ravenherz.cse.dal.dto.basic.HistoryData;
import com.ravenherz.cse.dal.dto.basic.enums.EventType;
import com.ravenherz.cse.present.EditorTree;
import com.ravenherz.cse.present.ResourceGroupIndex;
import com.ravenherz.cse.store.AppStoreException;
import com.ravenherz.cse.util.frontend.ShippedPackCatalog;
import com.ravenherz.cse.util.staticapps.AppInstallSlug;
import com.ravenherz.cse.util.staticapps.AppManifest;
import com.ravenherz.cse.util.staticapps.StaticAppDeployer;
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
import java.util.Locale;

@Controller
@RequestMapping("/editor/apps")
public class AppsController extends AbstractController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppsController.class);

    @Autowired
    private StaticAppDeployer staticAppDeployer;

    @Autowired
    private ResourceGroupIndex resourceGroupIndex;

    @Autowired
    private AppStoreService appStoreService;

    @GetMapping
    public String list(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        response.sendRedirect(request.getContextPath() + EditorTree.APPS_HREF);
        return null;
    }

    @PostMapping("/upload")
    public String upload(@RequestParam(value = "slug", required = false) String slug,
            @RequestParam(value = "returnGroup", required = false) String returnGroup,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }

        if (file == null || file.isEmpty()) {
            return listWithError(request, response, ".cseapp file is required");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null
                || !originalFilename.toLowerCase(Locale.ROOT).endsWith(StaticAppDeployer.APP_PACKAGE_EXT)) {
            return listWithError(request, response, "Only .cseapp files are accepted");
        }

        byte[] zipBytes = file.getBytes();
        try {
            staticAppDeployer.validateZip(zipBytes);
        } catch (IOException ex) {
            return listWithError(request, response, ex.getMessage());
        }

        AppManifest manifest = staticAppDeployer.readManifest(zipBytes);
        String normalizedSlug = AppInstallSlug.resolve(slug, manifest);
        if (normalizedSlug.isEmpty()) {
            return listWithError(request, response,
                    "Pack needs a slug in version.manifest. The zip filename is not used.");
        }
        try {
            staticAppDeployer.validateSlug(normalizedSlug);
        } catch (IllegalArgumentException ex) {
            return listWithError(request, response, "Slug must be lowercase letters, digits, and hyphens");
        }

        String base64 = Base64.getEncoder().encodeToString(zipBytes);
        AppData appData = new AppData();
        appData.setSlug(normalizedSlug);
        appData.setOriginalFilename(originalFilename);
        appData.setSizeInBytes(zipBytes.length);
        if (manifest != null) {
            appData.setAppName(manifest.getName());
            appData.setAppVersion(manifest.getVersion());
            appData.setAuthor(manifest.getAuthor());
            appData.setCompany(manifest.getCompany());
            appData.setDescription(manifest.getDescription());
        }

        AppEntity existing = serviceProvider.getAppService().getBySlug(normalizedSlug);
        if (existing != null && existing.getAppData() != null) {
            appData.applyStoreSettings(existing.getAppData().storeSettings());
            appData.setStoreTables(existing.getAppData().getStoreTables());
        }
        if (manifest != null) {
            appData.applyManifestTables(manifest.getStoreTables());
        }

        if (base64.length() > AppData.CHUNK_SIZE) {
            appData.setLargeFile(true);
            int chunkSize = AppData.CHUNK_SIZE;
            int totalChunks = (int) Math.ceil((double) base64.length() / chunkSize);
            for (int i = 0; i < totalChunks; i++) {
                int start = i * chunkSize;
                int end = Math.min(start + chunkSize, base64.length());
                DataChunkEntity chunk = new DataChunkEntity(base64.substring(start, end));
                serviceProvider.getAppService().saveDataChunk(chunk);
                appData.addDataChunkId(StoredIds.objectId(chunk.getId()));
            }
        } else {
            appData.setLargeFile(false);
            appData.setContentRaw(base64);
        }

        if (existing != null) {
            serviceProvider.getAppService().deleteChunks(existing);
            existing.setAppData(appData);
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
            serviceProvider.getAppService().replace(existing);
        } else {
            serviceProvider.getAppService().insert(new AppEntity(appData, accessor == null ? null : accessor.getId()));
        }

        try {
            staticAppDeployer.deploy(normalizedSlug, zipBytes);
        } catch (Exception ex) {
            LOGGER.error("Failed to deploy static app '{}'", normalizedSlug, ex);
            resourceGroupIndex.contentChanged();
            return listWithError(request, response, "Saved in Mongo but disk deploy failed: " + ex.getMessage());
        }
        resourceGroupIndex.contentChanged();

        response.sendRedirect(request.getContextPath()
                + EditorTree.catalogReturnHref(returnGroup, EditorTree.APPS_ID));
        return null;
    }

    @PostMapping("/delete")
    public String delete(@RequestParam("slug") String slug,
            @RequestParam(value = "returnGroup", required = false) String returnGroup,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        String normalizedSlug = slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT);
        if (ShippedPackCatalog.isShippedApp(normalizedSlug) || StaticAppDeployer.isReservedSlug(normalizedSlug)) {
            return listWithError(request, response, "Cannot delete a bundled app");
        }
        try {
            staticAppDeployer.validateSlug(normalizedSlug);
        } catch (IllegalArgumentException ex) {
            return listWithError(request, response, "Invalid slug");
        }
        AppEntity existing = serviceProvider.getAppService().getBySlug(normalizedSlug);
        if (existing != null) {
            serviceProvider.getAppService().delete(existing);
            resourceGroupIndex.contentChanged();
        }
        try {
            staticAppDeployer.undeploy(normalizedSlug);
        } catch (Exception ex) {
            LOGGER.warn("Failed to remove disk tree for '{}'", normalizedSlug, ex);
        }
        response.sendRedirect(request.getContextPath()
                + EditorTree.catalogReturnHref(returnGroup, EditorTree.APPS_ID));
        return null;
    }

    @PostMapping("/store")
    public String store(@RequestParam("slug") String slug,
            @RequestParam(value = "storeEnabled", defaultValue = "false") boolean storeEnabled,
            @RequestParam(value = "storeOpen", defaultValue = "false") boolean storeOpen,
            @RequestParam(value = "maxDataKb", required = false) Integer maxDataKb,
            @RequestParam(value = "maxDocs", required = false) Integer maxDocs,
            @RequestParam(value = "maxBytesMb", required = false) Integer maxBytesMb,
            @RequestParam(value = "returnGroup", required = false) String returnGroup,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        String normalizedSlug = slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT);
        if (ShippedPackCatalog.isShippedApp(normalizedSlug) || StaticAppDeployer.isReservedSlug(normalizedSlug)) {
            return listWithError(request, response, "Bundled apps do not have a data store");
        }
        try {
            staticAppDeployer.validateSlug(normalizedSlug);
            AppEntity app = serviceProvider.getAppService().getBySlug(normalizedSlug);
            if (app == null || app.getAppData() == null || !app.getAppData().requestsStore()) {
                return listWithError(request, response, "This pack does not declare a data store");
            }
            AppStoreSettings settings = AppStoreSettings.copyOf(app.getAppData().storeSettings());
            settings.setEnabled(storeEnabled);
            settings.setSchemaOpen(storeOpen);
            if (maxDataKb != null) {
                settings.setMaxDataBytes(maxDataKb * 1024);
            }
            if (maxDocs != null) {
                settings.setMaxDocs(maxDocs);
            }
            if (maxBytesMb != null) {
                settings.setMaxBytes(maxBytesMb * 1024L * 1024L);
            }
            appStoreService.updateStoreSettings(normalizedSlug, settings);
        } catch (IllegalArgumentException ex) {
            return listWithError(request, response, "Invalid slug");
        } catch (AppStoreException ex) {
            return listWithError(request, response, ex.getMessage());
        }
        resourceGroupIndex.contentChanged();
        response.sendRedirect(request.getContextPath()
                + EditorTree.catalogReturnHref(returnGroup, EditorTree.APPS_ID));
        return null;
    }

    private String listWithError(HttpServletRequest request, HttpServletResponse response, String error)
            throws IOException {
        response.sendRedirect(request.getContextPath()
                + EditorTree.withNotice(EditorTree.APPS_HREF, error));
        return null;
    }
}

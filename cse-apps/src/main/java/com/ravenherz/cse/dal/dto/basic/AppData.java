package com.ravenherz.cse.dal.dto.basic;

import org.springframework.data.mongodb.core.index.Indexed;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

import com.ravenherz.cse.store.AppStoreTableSpec;

public final class AppData {

    public static final int CHUNK_SIZE = 7000000;

    @Indexed
    private String slug;
    private String originalFilename;
    private long sizeInBytes;
    private String contentRaw;
    private boolean largeFile;
    private List<ObjectId> dataChunkIds;
    private String appName;
    private String appVersion;
    private String author;
    private String company;
    private String description;
    private boolean storeEnabled;
    private boolean storeOpen;
    private AppStoreSettings storeSettings;
    private List<AppStoreTableSpec> storeTables;

    public AppData() {
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    public void setSizeInBytes(long sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }

    public String getContentRaw() {
        return contentRaw;
    }

    public void setContentRaw(String contentRaw) {
        this.contentRaw = contentRaw;
    }

    public boolean isLargeFile() {
        return largeFile;
    }

    public void setLargeFile(boolean largeFile) {
        this.largeFile = largeFile;
    }

    public List<ObjectId> getDataChunkIds() {
        return dataChunkIds;
    }

    public void setDataChunkIds(List<ObjectId> dataChunkIds) {
        this.dataChunkIds = dataChunkIds;
    }

    public void addDataChunkId(ObjectId chunkId) {
        if (this.dataChunkIds == null) {
            this.dataChunkIds = new ArrayList<>();
        }
        this.dataChunkIds.add(chunkId);
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isStoreEnabled() {
        return storeSettings().isEnabled();
    }

    public void setStoreEnabled(boolean storeEnabled) {
        this.storeEnabled = storeEnabled;
        storeSettings().setEnabled(storeEnabled);
    }

    public boolean isStoreOpen() {
        return storeSettings().isSchemaOpen();
    }

    public void setStoreOpen(boolean storeOpen) {
        this.storeOpen = storeOpen;
        storeSettings().setSchemaOpen(storeOpen);
    }

    public AppStoreSettings getStoreSettings() {
        return storeSettings;
    }

    public void setStoreSettings(AppStoreSettings storeSettings) {
        applyStoreSettings(storeSettings);
    }

    public AppStoreSettings storeSettings() {
        if (storeSettings == null) {
            storeSettings = AppStoreSettings.fromLegacy(storeEnabled, storeOpen);
        }
        return storeSettings;
    }

    public void applyStoreSettings(AppStoreSettings incoming) {
        if (incoming == null) {
            if (storeSettings == null) {
                storeSettings = AppStoreSettings.fromLegacy(storeEnabled, storeOpen);
            }
            return;
        }
        AppStoreSettings copy = AppStoreSettings.copyOf(incoming);
        this.storeSettings = copy;
        this.storeEnabled = copy.isEnabled();
        this.storeOpen = copy.isSchemaOpen();
    }

    public boolean requestsStore() {
        return !getStoreTables().isEmpty();
    }

    public List<AppStoreTableSpec> getStoreTables() {
        return storeTables == null ? List.of() : storeTables;
    }

    public void setStoreTables(List<AppStoreTableSpec> storeTables) {
        this.storeTables = storeTables == null ? new ArrayList<>() : new ArrayList<>(storeTables);
    }

    public void applyManifestTables(List<AppStoreTableSpec> fromManifest) {
        this.storeTables = AppStoreTableSpec.merge(getStoreTables(), fromManifest);
    }
}

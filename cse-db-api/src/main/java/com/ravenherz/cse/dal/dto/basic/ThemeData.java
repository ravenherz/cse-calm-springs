package com.ravenherz.cse.dal.dto.basic;

import org.springframework.data.mongodb.core.index.Indexed;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public final class ThemeData {

    public static final int CHUNK_SIZE = 7000000;

    @Indexed
    private String themeId;
    private String originalFilename;
    private long sizeInBytes;
    private String contentRaw;
    private boolean largeFile;
    private List<ObjectId> dataChunkIds;
    private String title;
    private String author;
    private String shell;
    private String defaultSchema;
    private List<String> schemas;
    private int sortOrder;

    public ThemeData() {
    }

    public String getThemeId() {
        return themeId;
    }

    public void setThemeId(String themeId) {
        this.themeId = themeId;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getShell() {
        return shell;
    }

    public void setShell(String shell) {
        this.shell = shell;
    }

    public String getDefaultSchema() {
        return defaultSchema;
    }

    public void setDefaultSchema(String defaultSchema) {
        this.defaultSchema = defaultSchema;
    }

    public List<String> getSchemas() {
        return schemas;
    }

    public void setSchemas(List<String> schemas) {
        this.schemas = schemas;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}

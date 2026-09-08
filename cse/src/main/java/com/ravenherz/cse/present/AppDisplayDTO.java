package com.ravenherz.cse.present;

import com.ravenherz.cse.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class AppDisplayDTO {
    private String id;
    private String slug;
    private String originalFilename;
    private long sizeInBytes;
    private boolean largeFile;
    private List<String> chunkIds = new ArrayList<>();
    private String publicUrl;
    private String name;
    private String version;
    private String author;
    private String company;
    private String description;
    private boolean productLogo;
    private boolean bundled;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getSizeLabel() {
        return StringUtils.formatByteSize(sizeInBytes);
    }

    public boolean isLargeFile() {
        return largeFile;
    }

    public void setLargeFile(boolean largeFile) {
        this.largeFile = largeFile;
    }

    public List<String> getChunkIds() {
        return chunkIds;
    }

    public void setChunkIds(List<String> chunkIds) {
        this.chunkIds = chunkIds == null ? new ArrayList<>() : chunkIds;
    }

    public int getChunkCount() {
        return chunkIds == null ? 0 : chunkIds.size();
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public void setPublicUrl(String publicUrl) {
        this.publicUrl = publicUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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

    public boolean isProductLogo() {
        return productLogo;
    }

    public void setProductLogo(boolean productLogo) {
        this.productLogo = productLogo;
    }

    public boolean isBundled() {
        return bundled;
    }

    public void setBundled(boolean bundled) {
        this.bundled = bundled;
    }

    public String getHint() {
        if (name == null || name.isBlank()) {
            return version;
        }
        if (version == null || version.isBlank()) {
            return name;
        }
        return name + " " + version;
    }
}

package com.ravenherz.cse.content;

public class AlbumImageDTO {

    private String src;
    private String full;
    private String fileName;
    private String description;

    public AlbumImageDTO() {
    }

    public AlbumImageDTO(String src, String full, String fileName) {
        this(src, full, fileName, null);
    }

    public AlbumImageDTO(String src, String full, String fileName, String description) {
        this.src = src;
        this.full = full;
        this.fileName = fileName;
        this.description = description;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getFull() {
        return full;
    }

    public void setFull(String full) {
        this.full = full;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAlt() {
        if (description != null && !description.isBlank()) {
            return description;
        }
        return fileName == null ? "" : fileName;
    }
}

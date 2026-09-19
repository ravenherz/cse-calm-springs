package com.ravenherz.cse.present;

import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.basic.ResourceSizeHint;
import com.ravenherz.cse.dal.dto.basic.enums.ResourceType;

public record ResourceTreeFile(String id, String name, String href, Mark mark, boolean preview,
        String key, boolean canDelete, boolean canActivate, String embedKey) {

    public enum Mark {
        NONE, PAGE, ALBUM, PLAYLIST, APP, THEME, URL_TEMPLATE
    }

    public ResourceTreeFile(String id, String name, String href, Mark mark, boolean preview) {
        this(id, name, href, mark, preview, null, defaultCanDelete(mark, href), false, null);
    }

    public ResourceTreeFile(String id, String name, String href, Mark mark) {
        this(id, name, href, mark, false);
    }

    public ResourceTreeFile(String id, String name, String href) {
        this(id, name, href, Mark.NONE, false);
    }

    public ResourceTreeFile(String id, String name) {
        this(id, name, null, Mark.NONE, false);
    }

    public ResourceTreeFile withPreview(boolean preview) {
        return this.preview == preview ? this
                : new ResourceTreeFile(id, name, href, mark, preview, key, canDelete, canActivate, embedKey);
    }

    public ResourceTreeFile withKey(String key) {
        return new ResourceTreeFile(id, name, href, mark, preview, key, canDelete, canActivate, embedKey);
    }

    public ResourceTreeFile withDelete(boolean canDelete) {
        return this.canDelete == canDelete ? this
                : new ResourceTreeFile(id, name, href, mark, preview, key, canDelete, canActivate, embedKey);
    }

    public ResourceTreeFile withActivate(boolean canActivate) {
        return this.canActivate == canActivate ? this
                : new ResourceTreeFile(id, name, href, mark, preview, key, canDelete, canActivate, embedKey);
    }

    public ResourceTreeFile withEmbedId(String embedKey) {
        return new ResourceTreeFile(id, name, href, mark, preview, key, canDelete, canActivate, embedKey);
    }

    public boolean isPage() {
        return href != null && !href.isBlank();
    }

    public boolean isAlbum() {
        return mark == Mark.ALBUM;
    }

    public boolean isContentPage() {
        return mark == Mark.PAGE;
    }

    public boolean isPlaylist() {
        return mark == Mark.PLAYLIST;
    }

    public boolean isApp() {
        return mark == Mark.APP;
    }

    public boolean isTheme() {
        return mark == Mark.THEME;
    }

    public boolean isUrlTemplate() {
        return mark == Mark.URL_TEMPLATE;
    }

    public boolean isResourceFile() {
        return mark == Mark.NONE && (href == null || href.isBlank());
    }

    public boolean isImageFile() {
        return isResourceFile() && name != null && ResourceType.getByFileName(name) == ResourceType.IMAGE;
    }

    public boolean isVideoFile() {
        return isResourceFile() && name != null && ResourceType.getByFileName(name) == ResourceType.VIDEO;
    }

    public boolean isBinaryFile() {
        return isResourceFile() && name != null && ResourceType.getByFileName(name) == ResourceType.BINARY;
    }

    public String kind() {
        if (isAlbum()) {
            return "album";
        }
        if (isContentPage()) {
            return "page";
        }
        if (isPlaylist()) {
            return "playlist";
        }
        if (isApp()) {
            return "app";
        }
        if (isTheme()) {
            return "theme";
        }
        if (isUrlTemplate()) {
            return "url-template";
        }
        return "resource";
    }

    public String embedTag() {
        if (isAlbum() || isContentPage()) {
            return "cse-page";
        }
        if (isPlaylist()) {
            return "cse-playlist";
        }
        if (isUrlTemplate()) {
            return "cse-url";
        }
        if (isApp()) {
            return "cse-app";
        }
        if (isImageFile()) {
            return "cse-image";
        }
        if (isVideoFile()) {
            return "cse-video";
        }
        if (isBinaryFile()) {
            return "cse-binary";
        }
        return "";
    }

    public String embedId() {
        if (embedKey != null && !embedKey.isBlank()) {
            return embedKey.trim();
        }
        if (isContentPage() || isAlbum() || isApp()) {
            return key == null ? "" : key.trim();
        }
        if (isImageFile() || isVideoFile() || isBinaryFile()) {
            return id == null ? "" : id.trim();
        }
        return "";
    }

    public boolean isEmbeddable() {
        String tag = embedTag();
        String embedId = embedId();
        return tag != null && !tag.isBlank() && embedId != null && !embedId.isBlank();
    }

    public boolean canEdit() {
        return mark == Mark.PAGE || mark == Mark.ALBUM || mark == Mark.PLAYLIST || mark == Mark.URL_TEMPLATE;
    }

    public boolean canRename() {
        return isResourceFile() || isContentPage() || isAlbum() || isPlaylist() || isUrlTemplate();
    }

    public boolean canDownload() {
        return isResourceFile() && key != null && !key.isBlank();
    }

    public boolean canOpen() {
        return isApp() && href != null && !href.isBlank();
    }

    private static boolean defaultCanDelete(Mark mark, String href) {
        if (mark == Mark.APP || mark == Mark.THEME) {
            return false;
        }
        if (mark == Mark.PAGE || mark == Mark.ALBUM || mark == Mark.PLAYLIST || mark == Mark.URL_TEMPLATE) {
            return true;
        }
        return href == null || href.isBlank();
    }

    public static ResourceTreeFile from(ResourceEntity resource) {
        if (resource == null || resource.getId() == null) {
            return null;
        }
        String name = "";
        String path = null;
        if (resource.getResourceData() != null) {
            name = resource.getResourceData().getFileName();
            path = resource.getResourceData().getPathPublic();
        }
        if (name == null || name.isBlank()) {
            name = resource.getId().toString();
        }
        return new ResourceTreeFile(resource.getId().toString(), name, null, Mark.NONE, false, path, true, false, null);
    }

    public static ResourceTreeFile from(ResourceSizeHint hint) {
        if (hint == null || hint.id() == null) {
            return null;
        }
        String name = hint.fileName();
        if (name == null || name.isBlank()) {
            name = hint.id().toString();
        }
        return new ResourceTreeFile(hint.id().toString(), name, null, Mark.NONE, false, hint.pathPublic(), true, false, null);
    }
}

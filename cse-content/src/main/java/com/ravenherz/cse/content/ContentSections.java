package com.ravenherz.cse.content;

import com.ravenherz.cse.core.admin.AdminField;
import com.ravenherz.cse.core.admin.AdminFieldError;
import com.ravenherz.cse.core.admin.AdminPresentation;
import com.ravenherz.cse.core.admin.AdminSection;
import com.ravenherz.cse.core.admin.AdminSectionRecords;
import com.ravenherz.cse.core.admin.AdminSectionSource;
import com.ravenherz.cse.core.admin.CardPlace;
import com.ravenherz.cse.core.admin.FieldType;
import com.ravenherz.cse.core.admin.TreeGlyph;
import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.dao.PlaylistService;
import com.ravenherz.cse.dal.dao.UrlTemplateService;
import com.ravenherz.cse.dal.dto.PlaylistEntity;
import com.ravenherz.cse.dal.dto.UrlTemplateEntity;
import com.ravenherz.cse.dal.dto.basic.PlaylistData;
import com.ravenherz.cse.dal.dto.basic.UrlTemplateData;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
final class UrlTemplateAdmin implements AdminSectionSource {

    public static final String SECTION_ID = "url-templates";

    @Override
    public AdminSection section() {
        return new AdminSection(SECTION_ID, "URL Templates", List.of(
                new AdminField("urlTemplateId", "Id", FieldType.TEXT, true, List.of(), CardPlace.SOURCE),
                new AdminField("urlPattern", "Pattern", FieldType.TEXT, true, List.of(), CardPlace.TARGET),
                new AdminField("urlDefaultText", "Default text", FieldType.TEXT, false, List.of()),
                new AdminField("urlImage", "Image", FieldType.TEXT, false, List.of())),
                new AdminPresentation("{urlTemplateId}", TreeGlyph.FILE));
    }
}

@Component
final class PlaylistAdmin implements AdminSectionSource {

    public static final String SECTION_ID = "playlists";

    @Override
    public AdminSection section() {
        return new AdminSection(SECTION_ID, "Playlists", List.of(
                new AdminField("playlistId", "Id", FieldType.TEXT, true, List.of(), CardPlace.CODE),
                new AdminField("title", "Title", FieldType.TEXT, true, List.of(), CardPlace.SOURCE),
                new AdminField("description", "Description", FieldType.TEXT, false, List.of()),
                new AdminField("refImageId", "Cover", FieldType.ENTITY_ID, false, List.of())),
                new AdminPresentation("{title}", TreeGlyph.FILE));
    }
}

@Component
final class UrlTemplateRecords implements AdminSectionRecords {

    private final UrlTemplateService store;

    UrlTemplateRecords(UrlTemplateService store) {
        this.store = store;
    }

    @Override
    public String sectionId() {
        return UrlTemplateAdmin.SECTION_ID;
    }

    @Override
    public List<Map<String, String>> list() {
        List<Map<String, String>> rows = new ArrayList<>();
        for (UrlTemplateEntity row : store.getAllUrlTemplates()) {
            rows.add(fields(row));
        }
        return rows;
    }

    @Override
    public Map<String, String> find(String id) {
        UrlTemplateEntity row = load(id);
        return row == null ? null : fields(row);
    }

    @Override
    public List<AdminFieldError> create(Map<String, String> posted) {
        UrlTemplateEntity row = new UrlTemplateEntity();
        row.setUrlTemplateData(new UrlTemplateData());
        List<AdminFieldError> errors = apply(row, posted, null);
        if (!errors.isEmpty()) {
            return errors;
        }
        store.insert(row);
        return List.of();
    }

    @Override
    public List<AdminFieldError> update(String id, Map<String, String> posted) {
        UrlTemplateEntity row = load(id);
        if (row == null) {
            return List.of(new AdminFieldError("", "That URL template was not found"));
        }
        List<AdminFieldError> errors = apply(row, posted, row.getId());
        if (!errors.isEmpty()) {
            return errors;
        }
        store.replace(row);
        return List.of();
    }

    @Override
    public void delete(String id) {
        UrlTemplateEntity row = load(id);
        if (row != null) {
            store.delete(row);
        }
    }

    private List<AdminFieldError> apply(UrlTemplateEntity row, Map<String, String> posted, EntityId self) {
        Map<String, String> values = posted == null ? Map.of() : posted;
        String slug = text(values.get("urlTemplateId"));
        String pattern = text(values.get("urlPattern"));
        List<AdminFieldError> errors = new ArrayList<>();
        if (slug.isEmpty()) {
            errors.add(new AdminFieldError("urlTemplateId", "Required"));
        } else if (taken(slug, self)) {
            errors.add(new AdminFieldError("urlTemplateId", "A template with this id already exists"));
        }
        if (pattern.isEmpty()) {
            errors.add(new AdminFieldError("urlPattern", "Required"));
        }
        if (!errors.isEmpty()) {
            return errors;
        }
        row.setUrlTemplateId(slug);
        if (row.getUrlTemplateData() == null) {
            row.setUrlTemplateData(new UrlTemplateData());
        }
        row.getUrlTemplateData().setUrlPattern(pattern);
        row.getUrlTemplateData().setUrlDefaultText(text(values.get("urlDefaultText")));
        row.getUrlTemplateData().setUrlImage(text(values.get("urlImage")));
        return List.of();
    }

    private boolean taken(String slug, EntityId self) {
        UrlTemplateEntity other = store.getByUrlTemplateId(slug);
        return other != null && (self == null || !self.equals(other.getId()));
    }

    private UrlTemplateEntity load(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        try {
            Object entity = store.getById(UrlTemplateEntity.class, EntityId.of(id.trim()));
            return entity instanceof UrlTemplateEntity row ? row : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Map<String, String> fields(UrlTemplateEntity row) {
        UrlTemplateData data = row.getUrlTemplateData();
        Map<String, String> values = new LinkedHashMap<>();
        values.put("id", row.getId() == null ? "" : row.getId().toString());
        values.put("urlTemplateId", text(row.getUrlTemplateId()));
        values.put("urlPattern", data == null ? "" : text(data.getUrlPattern()));
        values.put("urlDefaultText", data == null ? "" : text(data.getUrlDefaultText()));
        values.put("urlImage", data == null ? "" : text(data.getUrlImage()));
        return values;
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }
}

@Component
final class PlaylistRecords implements AdminSectionRecords {

    private final PlaylistService store;

    PlaylistRecords(PlaylistService store) {
        this.store = store;
    }

    @Override
    public String sectionId() {
        return PlaylistAdmin.SECTION_ID;
    }

    @Override
    public List<Map<String, String>> list() {
        List<Map<String, String>> rows = new ArrayList<>();
        for (PlaylistEntity row : store.getAllPlaylists()) {
            rows.add(fields(row));
        }
        return rows;
    }

    @Override
    public Map<String, String> find(String id) {
        PlaylistEntity row = load(id);
        return row == null ? null : fields(row);
    }

    @Override
    public List<AdminFieldError> create(Map<String, String> posted) {
        PlaylistEntity row = new PlaylistEntity();
        row.setPlaylistData(new PlaylistData());
        List<AdminFieldError> errors = apply(row, posted, null);
        if (!errors.isEmpty()) {
            return errors;
        }
        store.insert(row);
        return List.of();
    }

    @Override
    public List<AdminFieldError> update(String id, Map<String, String> posted) {
        PlaylistEntity row = load(id);
        if (row == null) {
            return List.of(new AdminFieldError("", "That playlist was not found"));
        }
        List<AdminFieldError> errors = apply(row, posted, row.getId());
        if (!errors.isEmpty()) {
            return errors;
        }
        store.replace(row);
        return List.of();
    }

    @Override
    public void delete(String id) {
        PlaylistEntity row = load(id);
        if (row != null) {
            store.delete(row);
        }
    }

    private List<AdminFieldError> apply(PlaylistEntity row, Map<String, String> posted, EntityId self) {
        Map<String, String> values = posted == null ? Map.of() : posted;
        String slug = text(values.get("playlistId"));
        String title = text(values.get("title"));
        List<AdminFieldError> errors = new ArrayList<>();
        if (slug.isEmpty()) {
            errors.add(new AdminFieldError("playlistId", "Required"));
        } else if (taken(slug, self)) {
            errors.add(new AdminFieldError("playlistId", "A playlist with this id already exists"));
        }
        if (title.isEmpty()) {
            errors.add(new AdminFieldError("title", "Required"));
        }
        String cover = text(values.get("refImageId"));
        EntityId coverId = null;
        if (!cover.isEmpty()) {
            try {
                coverId = EntityId.of(cover);
            } catch (IllegalArgumentException ex) {
                errors.add(new AdminFieldError("refImageId", "Use a 24 character id"));
            }
        }
        if (!errors.isEmpty()) {
            return errors;
        }
        row.setPlaylistId(slug);
        if (row.getPlaylistData() == null) {
            row.setPlaylistData(new PlaylistData());
        }
        row.getPlaylistData().setTitle(title);
        row.getPlaylistData().setDescription(text(values.get("description")));
        row.getPlaylistData().setRefImageId(coverId);
        return List.of();
    }

    private boolean taken(String slug, EntityId self) {
        PlaylistEntity other = store.getByPlaylistId(slug);
        return other != null && (self == null || !self.equals(other.getId()));
    }

    private PlaylistEntity load(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        try {
            Object entity = store.getById(PlaylistEntity.class, EntityId.of(id.trim()));
            return entity instanceof PlaylistEntity row ? row : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Map<String, String> fields(PlaylistEntity row) {
        PlaylistData data = row.getPlaylistData();
        Map<String, String> values = new LinkedHashMap<>();
        values.put("id", row.getId() == null ? "" : row.getId().toString());
        values.put("playlistId", text(row.getPlaylistId()));
        values.put("title", data == null ? "" : text(data.getTitle()));
        values.put("description", data == null ? "" : text(data.getDescription()));
        values.put("refImageId", data == null || data.getRefImageId() == null ? "" : data.getRefImageId().toString());
        return values;
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }
}

package com.ravenherz.cse.dal.dto.events;

import com.ravenherz.cse.util.CseEmbedProcessor;
import com.ravenherz.cse.util.MarkdownRenderer;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.basic.AlbumData;
import com.ravenherz.cse.dal.dto.basic.Event;
import com.ravenherz.cse.dal.dto.basic.PageData;
import com.ravenherz.cse.dal.dto.basic.enums.EventType;
import com.ravenherz.cse.content.AlbumImageDTO;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

public class PageEvent extends PageData {

    public static class PageEventConverter {

        private static Logger LOGGER = LoggerFactory.getLogger(PageEventConverter.class);

        private static DateTimeFormatter fmt = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("h:mm a")
                .toFormatter(Locale.US);

        public static PageEvent toEvent(ItemEntity itemEntity) {
            if (itemEntity != null && itemEntity.isAlbum()) {
                return toEvent(itemEntity, java.util.Collections.emptyList());
            }
            return toEvent(itemEntity, (ResourceEntity) null);
        }

        public static PageEvent toEvent(ItemEntity itemEntity, List<ResourceEntity> albumImages) {
            Event created = createdEvent(itemEntity);
            if (itemEntity != null && itemEntity.isAlbum()) {
                return new PageEvent(itemEntity, albumImages, created);
            }
            return toEvent(itemEntity, (ResourceEntity) null);
        }

        public static PageEvent toEvent(ItemEntity itemEntity, ResourceEntity featuredImage) {
            Event created = createdEvent(itemEntity);
            PageData pageData = itemEntity.getPageData() == null ? new PageData() : itemEntity.getPageData();
            return new PageEvent(itemEntity.getId().toString(),
                    categoryItemName(itemEntity),
                    itemEntity.getUniqueUriName(),
                    pageData, created, featuredImage);
        }

        private static Event createdEvent(ItemEntity itemEntity) {
            if (itemEntity.getHistoryData() == null || itemEntity.getHistoryData().getEvents() == null) {
                return new Event();
            }
            return Arrays.stream(itemEntity.getHistoryData().getEvents())
                    .filter(event -> event.getEventType().equals(EventType.ENTITY_CREATED))
                    .findFirst().orElse(new Event());
        }

        private static String categoryItemName(ItemEntity itemEntity) {
            if (itemEntity.getRefCategory() == null
                    || itemEntity.getRefCategory().getCategoryData() == null
                    || itemEntity.getRefCategory().getCategoryData().getItemName() == null) {
                return "";
            }
            return itemEntity.getRefCategory().getCategoryData().getItemName();
        }

        public static PageEventComment toPageEventComment(Comment comment) {
            LocalDateTime published = comment == null ? null : comment.getPublicationTime();
            String date = "";
            String time = "";
            if (published != null) {
                date = String.format("%s %s, %s",
                        published.toLocalDate().getMonth()
                                .getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                        getDayOfMonth(published.toLocalDate().getDayOfMonth()),
                        published.getYear());
                time = published.format(fmt).replace("AM", "am").replace("PM", "pm");
            }
            return new PageEventComment(authorLogin(comment), date, time,
                    comment == null ? null : comment.getMessage());
        }

        private static String authorLogin(Comment comment) {
            if (comment == null || comment.getAuthor() == null
                    || comment.getAuthor().getAccountData() == null) {
                return "Unknown";
            }
            String login = comment.getAuthor().getAccountData().getLogin();
            return login == null || login.isBlank() ? "Unknown" : login;
        }

        private static String getDayOfMonth(int dayOfMonth) {
            switch (dayOfMonth % 10) {
                case 1:
                    return String.valueOf(dayOfMonth) + "st";
                case 2:
                    return String.valueOf(dayOfMonth) + "nd";
                case 3:
                    return String.valueOf(dayOfMonth) + "rd";
                default:
                    return String.valueOf(dayOfMonth) + "th";
            }
        }
    }

    private LocalDateTime created;
    private String id;
    private String categoryItemName;
    private String uniqueUriName;
    private String imageLinkFull;
    private String pageLink;
    private List<String> tags;
    private List<PageEventComment> pageComments;
    private boolean album;
    private String title;
    private List<AlbumImageDTO> albumImages = new ArrayList<>();

    private PageEvent(String id, String categoryItemName, String uniqueUriName, PageData pageData,
            Event event, ResourceEntity featured) {
        super(pageData.getHeader(),
                pageData.getSubHeader(),
                CseEmbedProcessor.expand(MarkdownRenderer.render(pageData.getDescription())),
                pageData.getTags());
        this.title = shown(pageData.getHeader(), uniqueUriName);
        setNoTopDisplayImage(pageData.isNoTopDisplayImage());
        setExportPdf(pageData.isExportPdf());
        this.uniqueUriName = uniqueUriName;
        this.pageLink = "./?page=" + uniqueUriName;
        if (featured != null && featured.getResourceData() != null
                && featured.getResourceData().getPathPublic() != null) {
            this.imageLinkFull = "./content-protected" + featured.getResourceData().getPathPublic();
        } else {
            this.imageLinkFull = "./content-public/cse-core/images/no-image.jpg";
        }
        this.id = id;
        this.categoryItemName = categoryItemName;
        this.tags = pageData.getTags() == null ? new ArrayList<>() : pageData.getTags();
        if (event != null) {
            this.created = event.getLocalDateTime();
        }
        this.pageComments = convertComments(pageData);
    }

    private PageEvent(ItemEntity item, List<ResourceEntity> images, Event event) {
        super(albumHeader(item),
                albumSubHeader(item),
                CseEmbedProcessor.expand(MarkdownRenderer.render(albumDescription(item))),
                albumTags(item));
        this.title = albumTitle(item);
        this.uniqueUriName = item.getUniqueUriName();
        this.pageLink = "./?album=" + uniqueUriName;
        this.album = true;
        this.albumImages = toAlbumImages(images);
        if (!this.albumImages.isEmpty()) {
            this.imageLinkFull = this.albumImages.get(0).getSrc();
        } else {
            this.imageLinkFull = "./content-public/cse-core/images/no-image.jpg";
        }
        this.id = item.getId().toString();
        this.categoryItemName = PageEventConverter.categoryItemName(item);
        this.tags = albumTags(item);
        if (event != null) {
            this.created = event.getLocalDateTime();
        }
        this.pageComments = new ArrayList<>();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    private static String shown(String header, String fallback) {
        if (header != null && !header.isBlank()) {
            return header;
        }
        return fallback == null ? "" : fallback;
    }

    private static AlbumData albumData(ItemEntity item) {
        return item.getAlbumData() == null ? new AlbumData() : item.getAlbumData();
    }

    private static String albumTitle(ItemEntity item) {
        return shown(albumData(item).getHeader(), item.getUniqueUriName());
    }

    private static String albumHeader(ItemEntity item) {
        AlbumData data = albumData(item);
        if (data.getHeader() != null && !data.getHeader().isBlank()) {
            return data.getHeader();
        }
        return albumTitle(item);
    }

    private static String albumSubHeader(ItemEntity item) {
        return albumData(item).getSubHeader();
    }

    private static String albumDescription(ItemEntity item) {
        return albumData(item).getDescription();
    }

    private static List<String> albumTags(ItemEntity item) {
        List<String> tags = albumData(item).getTags();
        return tags == null ? new ArrayList<>() : tags;
    }

    private static List<AlbumImageDTO> toAlbumImages(List<ResourceEntity> images) {
        List<AlbumImageDTO> out = new ArrayList<>();
        if (images == null) {
            return out;
        }
        for (ResourceEntity image : images) {
            if (image.getResourceData() == null || image.getResourceData().getPathPublic() == null) {
                continue;
            }
            String full = "./content-protected" + image.getResourceData().getPathPublic();
            String src = "./content-protected" + image.displayPublicPath();
            out.add(new AlbumImageDTO(src, full, image.getResourceData().getFileName(),
                    image.getResourceData().getImageDescription()));
        }
        return out;
    }

    private List<PageEventComment> convertComments(PageData pageData) {
        if (pageData == null || pageData.getComments() == null) {
            return new ArrayList<>();
        }

        return pageData.getComments().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Comment::getPublicationTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(PageEventConverter::toPageEventComment)
                .collect(Collectors.toList());
    }

    public String getCategoryItemName() {
        return categoryItemName;
    }

    public String getId() {
        return id;
    }

    public String getUniqueUriName() {
        return uniqueUriName;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public String getImageLinkFull() {
        return imageLinkFull;
    }

    public String getPageLink() {
        return pageLink;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCategoryItemName(String categoryItemName) {
        this.categoryItemName = categoryItemName;
    }

    public void setImageLinkFull(String imageLinkFull) {
        this.imageLinkFull = imageLinkFull;
    }

    public void setPageLink(String pageLink) {
        this.pageLink = pageLink;
    }

    @Override public List<String> getTags() {
        return tags;
    }

    @Override public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<PageEventComment> getPageComments() {
        return pageComments;
    }

    public void setPageComments(
            List<PageEventComment> pageComments) {
        this.pageComments = pageComments;
    }

    public boolean isAlbum() {
        return album;
    }

    public void setAlbum(boolean album) {
        this.album = album;
    }

    public List<AlbumImageDTO> getAlbumImages() {
        return albumImages;
    }

    public void setAlbumImages(List<AlbumImageDTO> albumImages) {
        this.albumImages = albumImages == null ? new ArrayList<>() : albumImages;
    }
}

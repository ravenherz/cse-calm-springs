package com.ravenherz.rhzwe.dal.dto.events;

import com.ravenherz.rhzwe.dal.dto.ItemEntity;
import com.ravenherz.rhzwe.dal.dto.basic.Event;
import com.ravenherz.rhzwe.dal.dto.basic.PageData;
import com.ravenherz.rhzwe.dal.dto.basic.enums.EventType;
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

            return new PageEvent(itemEntity.getId().toString(),
                    itemEntity.getRefCategory().getCategoryData().getItemName(),
                    itemEntity.getUniqueUriName(),
                    itemEntity.getPageData(), Arrays.stream(itemEntity.getHistoryData().getEvents())
                    .filter(event -> event.getEventType().equals(EventType.ENTITY_CREATED))
                    .findFirst().orElse(new Event()));
            // this code sux
        }

        public static PageEventComment toPageEventComment(Comment comment) {

            return new PageEventComment(comment.getAuthor().getAccountData().getLogin(),
                    String.format("%s %s, %s",
                            comment.getPublicationTime().toLocalDate().getMonth()
                                    .getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                            getDayOfMonth(
                                    comment.getPublicationTime().toLocalDate().getDayOfMonth()),
                            comment.getPublicationTime().getYear()
                    ),
                    comment.getPublicationTime().format(fmt).replace("AM", "am")
                            .replace("PM", "pm"),
                    comment.getMessage());
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
    private String imageLinkFull;
    private String pageLink;
    private List<String> tags;
    private List<PageEventComment> pageComments;

    private PageEvent(String id, String categoryItemName, String uniqueUriName, PageData pageData,
            Event event) {
        super(pageData.getTitle(),
                pageData.getHeader(),
                pageData.getSubHeader(),
                pageData.getDescription(),
                pageData.getTags());
        this.pageLink = "./?page=" + uniqueUriName;
        if (pageData.getRefImage() != null) {
            this.imageLinkFull = "./content-protected" + pageData.getRefImage().getResourceData()
                    .getPathPublic();
        } else {
            this.imageLinkFull = "static/content-public/rhz-we-core/images/no-image.jpg";
        }
        this.id = id;
        this.categoryItemName = categoryItemName;
        this.tags = pageData.getTags() == null ? new ArrayList<>() : pageData.getTags();
        if (event != null) {
            this.created = event.getLocalDateTime();
        }
        this.pageComments = convertComments(pageData);
    }

    private List<PageEventComment> convertComments(PageData pageData) {
        if (pageData == null || pageData.getComments() == null) {
            return new ArrayList<>();
        }

        return pageData.getComments().stream()
                .sorted(Comparator.comparing(Comment::getPublicationTime).reversed())
                .map(PageEventConverter::toPageEventComment)
                .collect(Collectors.toList());
    }

    public String getCategoryItemName() {
        return categoryItemName;
    }

    public String getId() {
        return id;
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
}

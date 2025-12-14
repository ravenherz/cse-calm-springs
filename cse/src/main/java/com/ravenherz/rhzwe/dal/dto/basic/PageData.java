package com.ravenherz.rhzwe.dal.dto.basic;

import com.ravenherz.rhzwe.dal.dto.AccountEntity;
import com.ravenherz.rhzwe.dal.dto.ResourceEntity;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Reference;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Entity
public class PageData extends ItemData implements Serializable {

    @Entity
    public static class Comment {
        @Reference
        private AccountEntity author;
        private LocalDateTime publicationTime;
        private String message;

        public Comment() {
        }

        public Comment(AccountEntity author, LocalDateTime publicationTime, String message) {
            this.author = author;
            this.publicationTime = publicationTime;
            this.message = message;
        }

        public AccountEntity getAuthor() {
            return author;
        }

        public void setAuthor(AccountEntity author) {
            this.author = author;
        }

        public LocalDateTime getPublicationTime() {
            return publicationTime;
        }

        public void setPublicationTime(LocalDateTime publicationTime) {
            this.publicationTime = publicationTime;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    @Reference
    private ResourceEntity refImage;



    private String header;


    private String description;
    private List<String> tags;
    private List<Comment> comments;

    public PageData() {
        super();
    }

    /**
     * Public constructor
     *
     * @param title title of the page
     * @param header the header of the item
     * @param subHeader the subHeader of the item
     * @param description the description of the item
     * @param tags the array of html
     */
    public PageData(String title, String header, String subHeader,
            String description, List<String> tags) {
        super(title, subHeader);
        this.header = header;
        this.description = description;
        this.tags = tags;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getSubHeader() {
        return subHeader;
    }

    public void setSubHeader(String subHeader) {
        this.subHeader = subHeader;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public ResourceEntity getRefImage() {
        return refImage;
    }

    public void setRefImage(ResourceEntity refImage) {
        this.refImage = refImage;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PageData pageData = (PageData) o;
        return Objects.equals(refImage, pageData.refImage) &&
                Objects.equals(title, pageData.title) &&
                Objects.equals(header, pageData.header) &&
                Objects.equals(subHeader, pageData.subHeader) &&
                Objects.equals(description, pageData.description) &&
                Objects.equals(tags, pageData.tags);
    }

    @Override public int hashCode() {

        return Objects.hash(refImage, title, header, subHeader, description, tags);
    }

    @Override public String toString() {
        return "PageData{" +
                "refImage=" + refImage +
                ", title='" + title + '\'' +
                ", header='" + header + '\'' +
                ", subHeader='" + subHeader + '\'' +
                ", description='" + description + '\'' +
                ", tags=" + tags +
                '}';
    }
}

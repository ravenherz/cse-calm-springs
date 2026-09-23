package com.ravenherz.cse.dal.dto.basic;

import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.dto.AccountEntity;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class PageData extends ItemData implements Serializable {

    public static class Comment {
        @Field("author")
        private ObjectId authorId;
        @Transient
        private AccountEntity author;
        private LocalDateTime publicationTime;
        private String message;

        public Comment() {
        }

        public Comment(AccountEntity author, LocalDateTime publicationTime, String message) {
            setAuthor(author);
            this.publicationTime = publicationTime;
            this.message = message;
        }

        public ObjectId getAuthorId() {
            return authorId != null ? authorId : (author == null || author.getId() == null
                    ? null : new ObjectId(author.getId().toHexString()));
        }

        public AccountEntity getAuthor() {
            return author;
        }

        public void setAuthor(AccountEntity author) {
            this.author = author;
            this.authorId = author == null || author.getId() == null
                    ? null : new ObjectId(author.getId().toHexString());
        }

        public void attachAuthor(AccountEntity author) {
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

    @Field("refImage")
    private EntityId refImageId;
    private String header;
    private String description;
    private List<String> tags;
    private List<Comment> comments;
    private boolean noTopDisplayImage;
    private boolean exportPdf;

    public PageData() {
        super();
    }

    /**
     * @param header the header of the item
     * @param subHeader the subHeader of the item
     * @param description the description of the item
     * @param tags the array of html
     */
    public PageData(String header, String subHeader,
            String description, List<String> tags) {
        super(subHeader);
        this.header = header;
        this.description = description;
        this.tags = tags;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

//    public String getSubHeader() {
//        return subHeader;
//    }
//
//    public void setSubHeader(String subHeader) {
//        this.subHeader = subHeader;
//    }

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

    public EntityId getRefImageId() {
        return refImageId;
    }

    public void setRefImageId(EntityId refImageId) {
        this.refImageId = refImageId;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public boolean isNoTopDisplayImage() {
        return noTopDisplayImage;
    }

    public void setNoTopDisplayImage(boolean noTopDisplayImage) {
        this.noTopDisplayImage = noTopDisplayImage;
    }

    public boolean isExportPdf() {
        return exportPdf;
    }

    public void setExportPdf(boolean exportPdf) {
        this.exportPdf = exportPdf;
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PageData pageData = (PageData) o;
        return noTopDisplayImage == pageData.noTopDisplayImage &&
                exportPdf == pageData.exportPdf &&
                Objects.equals(refImageId, pageData.refImageId) &&
                Objects.equals(header, pageData.header) &&
                Objects.equals(subHeader, pageData.subHeader) &&
                Objects.equals(description, pageData.description) &&
                Objects.equals(tags, pageData.tags);
    }

    @Override public int hashCode() {

        return Objects.hash(refImageId, header, subHeader, description, tags, noTopDisplayImage,
                exportPdf);
    }

    @Override public String toString() {
        return "PageData{" +
                "refImage=" + refImageId +
                ", header='" + header + '\'' +
                ", subHeader='" + subHeader + '\'' +
                ", description='" + description + '\'' +
                ", tags=" + tags +
                ", noTopDisplayImage=" + noTopDisplayImage +
                ", exportPdf=" + exportPdf +
                '}';
    }
}

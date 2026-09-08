package com.ravenherz.cse.dal.dto;

import com.ravenherz.cse.constants.MongoCollections;
import com.ravenherz.cse.dal.dto.basic.AlbumData;
import com.ravenherz.cse.dal.dto.basic.PageData;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.List;

@Document(collection = MongoCollections.DATABASE_ITEMS)
public final class ItemEntity extends BasicEntity implements Serializable {

    public enum ItemType {
        PAGE,
        ALBUM
    }

    private ItemType itemType;
    @Indexed(unique = true)
    protected String uniqueUriName;
    @Field("refCategory")
    private ObjectId refCategoryId;
    @Transient
    private CategoryEntity refCategory;
    private PageData pageData;
    private AlbumData albumData;

    public ItemEntity() {

    }

    public ItemEntity(String uniqueUriName, PageData pageData,
            AccountEntity creator) {
        super("0.1.0", null, creator);
        this.uniqueUriName = uniqueUriName;
        this.pageData = pageData;
        this.itemType = ItemType.PAGE;
    }

    public ItemEntity(String uniqueUriName, AlbumData albumData,
            AccountEntity creator) {
        super("0.1.0", null, creator);
        this.uniqueUriName = uniqueUriName;
        this.albumData = albumData;
        this.itemType = ItemType.ALBUM;
    }

    public ObjectId getRefCategoryId() {
        return refCategoryId != null ? refCategoryId : (refCategory == null ? null : refCategory.getId());
    }

    public CategoryEntity getRefCategory() {
        return refCategory;
    }

    public void setRefCategory(CategoryEntity refCategory) {
        this.refCategory = refCategory;
        this.refCategoryId = refCategory == null ? null : refCategory.getId();
    }

    public void attachRefCategory(CategoryEntity refCategory) {
        this.refCategory = refCategory;
    }

    public PageData getPageData() {
        return pageData;
    }

    public void setPageData(PageData pageData) {
        this.pageData = pageData;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
    }

    public AlbumData getAlbumData() {
        return albumData;
    }

    public void setAlbumData(AlbumData albumData) {
        this.albumData = albumData;
    }

    public String getUniqueUriName() {
        return uniqueUriName;
    }

    public void setUniqueUriName(String uniqueUriName) {
        this.uniqueUriName = uniqueUriName;
    }

    public boolean isAlbum() {
        return ItemType.ALBUM.equals(itemType);
    }

    public boolean hasTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return false;
        }
        String needle = tag.trim();
        List<String> tags;
        if (isAlbum()) {
            tags = albumData == null ? null : albumData.getTags();
        } else {
            tags = pageData == null ? null : pageData.getTags();
        }
        if (tags == null) {
            return false;
        }
        for (String candidate : tags) {
            if (candidate != null && needle.equals(candidate.trim())) {
                return true;
            }
        }
        return false;
    }
}



package com.ravenherz.rhzwe.dal.dto;

import com.ravenherz.rhzwe.constants.Strings;
import com.ravenherz.rhzwe.dal.dto.basic.AlbumData;
import com.ravenherz.rhzwe.dal.dto.basic.PageData;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Indexed;
import dev.morphia.annotations.Reference;

import java.io.Serializable;

@Entity(Strings.DATABASE_ITEMS)
public final class ItemEntity extends BasicEntity implements Serializable {

    public enum ItemType {
        PAGE,
        ALBUM
    }

    private ItemType itemType;
    @Indexed
    protected String uniqueUriName;
    @Reference
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
    }

    public CategoryEntity getRefCategory() {
        return refCategory;
    }

    public void setRefCategory(CategoryEntity refCategory) {
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
}



package com.ravenherz.cse.dal.dto;

import com.ravenherz.cse.constants.MongoCollections;
import com.ravenherz.cse.dal.dto.basic.CategoryData;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Objects;

@Document(collection = MongoCollections.DATABASE_CATEGORIES)
public final class CategoryEntity extends BasicEntity implements Serializable {

    private CategoryData categoryData;

    public CategoryEntity() {

    }

    public CategoryEntity(CategoryData categoryData, AccountEntity creator) {
        super(null, creator);
        this.categoryData = categoryData;
    }

    public CategoryData getCategoryData() {
        return categoryData;
    }

    public void setCategoryData(CategoryData categoryData) {
        this.categoryData = categoryData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        CategoryEntity that = (CategoryEntity) o;
        return Objects.equals(categoryData, that.categoryData);
    }

    @Override
    public int hashCode() {

        return Objects.hash(super.hashCode(), categoryData);
    }

    @Override
    public String toString() {
        return "CategoryEntity{" +
                "categoryData=" + categoryData +
                ", id='" + id + '\'' +
                '}';
    }
}

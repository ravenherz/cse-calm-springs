package com.ravenherz.rhzwe.dal.dao;

import com.ravenherz.rhzwe.dal.dto.CategoryEntity;

import java.util.List;

public interface CategoryService extends Service {
    void delete(CategoryEntity category);

    List<CategoryEntity> getAllByVisibility(boolean isVisible);

    List<CategoryEntity> getAllActive();

    default List<CategoryEntity> getAllCategories() {
        return getAll().stream()
                .map(e -> (CategoryEntity) e)
                .collect(java.util.stream.Collectors.toList());
    }
}

package com.ravenherz.rhzwe.dal.dao.impl;

import com.ravenherz.rhzwe.dal.dao.BasicService;
import com.ravenherz.rhzwe.dal.dao.CategoryService;
import com.ravenherz.rhzwe.dal.dto.CategoryEntity;
import dev.morphia.query.filters.Filters;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Lazy
@Repository(value = "categoryService")
@Scope(value = "singleton")
public class CategoryServiceImpl extends BasicService implements CategoryService {

    @Override
    public List<CategoryEntity> getAllByVisibility(boolean isVisible) {
        return dataProvider.getDatastore().find(CategoryEntity.class)
                .filter(Filters.eq("categoryData.isVisible", isVisible))
                .stream()
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryEntity> getAllActive() {
        return dataProvider.getDatastore().find(CategoryEntity.class)
                .filter(Filters.eq("categoryData.isActive", true))
                .stream()
                .collect(Collectors.toList());
    }
}

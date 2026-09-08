package com.ravenherz.cse.dal.dao.impl;

import com.ravenherz.cse.dal.DataProvider;
import com.ravenherz.cse.dal.dao.BasicService;
import com.ravenherz.cse.dal.dao.CategoryService;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.CategoryEntity;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository(value = "categoryService")
public class CategoryServiceImpl extends BasicService implements CategoryService {

    public CategoryServiceImpl(DataProvider dataProvider) {
        super(dataProvider);
    }

    @Override
    public void delete(CategoryEntity category) {
        if (category != null) {
            mongo().remove(category);
        }
    }

    @Override
    public List<BasicEntity> getAll() {
        return new java.util.ArrayList<>(mongo().findAll(CategoryEntity.class));
    }

    @Override
    public List<CategoryEntity> getAllByVisibility(boolean isVisible) {
        return mongo().find(Query.query(Criteria.where("categoryData.isVisible").is(isVisible)),
                CategoryEntity.class);
    }

    @Override
    public List<CategoryEntity> getAllActive() {
        return mongo().find(Query.query(Criteria.where("categoryData.isActive").is(true)),
                CategoryEntity.class);
    }
}

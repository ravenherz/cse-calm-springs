package com.ravenherz.rhzwe.dal.dao;

import com.ravenherz.rhzwe.dal.dto.CategoryEntity;

import java.util.List;

public interface CategoryService extends Service {
    List<CategoryEntity> getAllByVisibility(boolean isVisible);

    List<CategoryEntity> getAllActive();
}

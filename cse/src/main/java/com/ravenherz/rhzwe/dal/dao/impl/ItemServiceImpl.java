package com.ravenherz.rhzwe.dal.dao.impl;

import com.ravenherz.rhzwe.dal.dao.BasicService;
import com.ravenherz.rhzwe.dal.dao.ItemService;
import com.ravenherz.rhzwe.dal.dto.CategoryEntity;
import com.ravenherz.rhzwe.dal.dto.ItemEntity;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.query.Query;
import dev.morphia.query.filters.Filters;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Lazy
@Repository(value = "itemService")
@Scope(value = "singleton")
public class ItemServiceImpl extends BasicService implements ItemService {

    @Override
    public List<ItemEntity> getAllByCategory(CategoryEntity categoryEntity) {
        return getPageEntities()
                .filter(Filters.eq("refCategory", categoryEntity))
                .stream()
                .collect(Collectors.toList());
    }

    @Override
    public ItemEntity getByName(String name) {
        return getPageEntities()
                .filter(Filters.eq("uniqueUriName", name))
                .first();
    }

    @Override public List<ItemEntity> getAllByTag(String tag) {
        return getPageEntities()
                .filter(Filters.expr(new Expression(String.format("{'pageData.tags': {$elemMatch: {$eq: '%s'}}}", tag))))
                .stream()
                .collect(Collectors.toList());
    }

    private Query<ItemEntity> getPageEntities() {
        return dataProvider.getDatastore().find(ItemEntity.class);
    }
}

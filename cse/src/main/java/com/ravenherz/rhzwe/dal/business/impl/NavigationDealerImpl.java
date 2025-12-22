package com.ravenherz.rhzwe.dal.business.impl;

import com.ravenherz.rhzwe.dal.EntityUtils;
import com.ravenherz.rhzwe.dal.business.Dealer;
import com.ravenherz.rhzwe.dal.business.NavigationDealer;
import com.ravenherz.rhzwe.dal.business.basic.Navigation;
import com.ravenherz.rhzwe.dal.business.basic.NavigationUnit;
import com.ravenherz.rhzwe.dal.business.basic.NavigationUnit.NavigationUnitType;
import com.ravenherz.rhzwe.dal.dto.AccountEntity;
import com.ravenherz.rhzwe.dal.dto.basic.enums.*;

import org.springframework.context.annotation.*;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.stream.Collectors;

@Lazy
@Repository(value = "navigationDealer")
@Scope(value = "singleton")
@DependsOn("serviceProvider")
public class NavigationDealerImpl extends Dealer implements NavigationDealer {

    @Override
    public Navigation getNavigation(AccountEntity accessor) {
        Navigation nav = new Navigation();
        nav.addItems(
            serviceProvider.getCategoryService()
                .getAllByVisibility(true).stream()
                .filter(entity -> EntityUtils.isAccessible(entity, AccessType.ACCESS_READ, accessor))
                .map(categoryEntity -> {
                    return new NavigationUnit(
                        NavigationUnitType.CATEGORY, null,
                        categoryEntity.getCategoryData().getNavigationTitle(),
                        categoryEntity.getCategoryData().getNavigationDescription(),
                        serviceProvider.getItemService()
                            .getAllByCategory(categoryEntity).stream()
                            .filter(item -> EntityUtils.isAccessible(item, AccessType.ACCESS_READ, accessor))
                            .map(item -> {
                                return new NavigationUnit(
                                    switch (item.getItemType()) {
                                        case PAGE -> NavigationUnitType.PAGE;
                                        case ALBUM -> NavigationUnitType.ALBUM;
                                    },
                                    item.getUniqueUriName(),
                                    item.getPageData() != null ? item.getPageData().getTitle(): "<null>",
                                    item.getPageData() != null ? item.getPageData().getSubHeader(): "<null>",
                                    null,
                                    item.selectCreationLocalDateTime()
                                );
                            })
                            .sorted(Comparator.comparing(NavigationUnit::getDateTimeCreated).reversed())
                            .collect(Collectors.toList()),
                        categoryEntity.selectCreationLocalDateTime()
                    );
                })
                .filter(cat -> !cat.getChildren().isEmpty())
                .collect(Collectors.toList())
        );

        return nav;
    }
}

package com.ravenherz.cse.dal.business.impl;

import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.business.Dealer;
import com.ravenherz.cse.dal.business.NavigationDealer;
import com.ravenherz.cse.dal.business.basic.Navigation;
import com.ravenherz.cse.dal.business.basic.NavigationUnit;
import com.ravenherz.cse.dal.EntityAccess;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.ItemEntity;
import com.ravenherz.cse.dal.dto.basic.enums.AccessType;
import com.ravenherz.cse.present.CategorySectionDTO;

import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.stream.Collectors;

@Repository(value = "navigationDealer")
public class NavigationDealerImpl extends Dealer implements NavigationDealer {

    public NavigationDealerImpl(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    public Navigation getNavigation(AccountEntity accessor) {
        Navigation nav = new Navigation();
        nav.addItems(
            serviceProvider.getCategoryService()
                .getAllByVisibility(true).stream()
                .filter(entity -> EntityAccess.isAccessible(entity, AccessType.ACCESS_READ, accessor))
                .sorted(CategorySectionDTO.displayOrder())
                .map(categoryEntity -> {
                    return new NavigationUnit(
                        NavigationUnit.NavigationUnitType.CATEGORY, null,
                        categoryEntity.getCategoryData().getNavigationTitle(),
                        categoryEntity.getCategoryData().getNavigationDescription(),
                        serviceProvider.getItemService()
                            .getAllByCategory(categoryEntity).stream()
                            .filter(item -> EntityAccess.isAccessible(item, AccessType.ACCESS_READ, accessor))
                            .map(item -> {
                                ItemEntity.ItemType type = item.getItemType() == null
                                        ? ItemEntity.ItemType.PAGE : item.getItemType();
                                String title;
                                String description;
                                if (type == ItemEntity.ItemType.ALBUM) {
                                    title = item.getAlbumData() != null ? item.getAlbumData().getTitle() : "<null>";
                                    description = item.getAlbumData() != null ? item.getAlbumData().getSubHeader() : "<null>";
                                } else {
                                    title = item.getPageData() != null ? item.getPageData().getTitle() : "<null>";
                                    description = item.getPageData() != null ? item.getPageData().getSubHeader() : "<null>";
                                }
                                return new NavigationUnit(
                                    type == ItemEntity.ItemType.ALBUM
                                            ? NavigationUnit.NavigationUnitType.ALBUM
                                            : NavigationUnit.NavigationUnitType.PAGE,
                                    item.getUniqueUriName(),
                                    title,
                                    description,
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

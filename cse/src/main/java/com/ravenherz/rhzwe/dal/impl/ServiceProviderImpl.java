package com.ravenherz.rhzwe.dal.impl;

import com.ravenherz.rhzwe.dal.ServiceProvider;
import com.ravenherz.rhzwe.dal.dao.AccountService;
import com.ravenherz.rhzwe.dal.dao.CategoryService;
import com.ravenherz.rhzwe.dal.dao.ItemService;
import com.ravenherz.rhzwe.dal.dao.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Lazy
@Service(value = "serviceProvider")
@Scope(value = "singleton")
@DependsOn({"dataProvider", "categoryService", "itemService", "accountService", "resourceService"})
public class ServiceProviderImpl implements ServiceProvider {

    private static AccountService accountService;
    private static CategoryService categoryService;
    private static ItemService itemService;
    private static ResourceService resourceService;

    @Override
    public AccountService getAccountService() {
        return accountService;
    }

    @Override
    public ResourceService getResourceService() {
        return resourceService;
    }

    @Override
    public ItemService getItemService() {
        return itemService;
    }

    @Override
    public CategoryService getCategoryService() {
        return categoryService;
    }

    @Autowired @Lazy
    public void setAccountService(AccountService accountServiceImpl) {
        accountService = accountServiceImpl;
    }

    @Autowired @Lazy
    public void setCategoryService(CategoryService categoryServiceImpl) {
        categoryService = categoryServiceImpl;
    }

    @Autowired @Lazy
    public void setItemService(ItemService itemServiceImpl) {
        itemService = itemServiceImpl;
    }

    @Autowired @Lazy
    public void setResourceService(ResourceService resourceServiceImpl) {
        resourceService = resourceServiceImpl;
    }
}

package com.ravenherz.cse.dal.impl;

import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.dao.AccountService;
import com.ravenherz.cse.dal.dao.AppService;
import com.ravenherz.cse.dal.dao.CategoryService;
import com.ravenherz.cse.dal.dao.ItemService;
import com.ravenherz.cse.dal.dao.PlaylistService;
import com.ravenherz.cse.dal.dao.ResourceGroupService;
import com.ravenherz.cse.dal.dao.ResourceService;
import com.ravenherz.cse.dal.dao.RoleMatrixService;
import com.ravenherz.cse.dal.dao.RoleService;
import com.ravenherz.cse.dal.dao.ThemeService;
import org.springframework.stereotype.Service;

@Service(value = "serviceProvider")
public class ServiceProviderImpl implements ServiceProvider {

    private final AccountService accountService;
    private final CategoryService categoryService;
    private final ItemService itemService;
    private final ResourceService resourceService;
    private final ResourceGroupService resourceGroupService;
    private final AppService appService;
    private final ThemeService themeService;
    private final PlaylistService playlistService;
    private final RoleService roleService;
    private final RoleMatrixService roleMatrixService;

    public ServiceProviderImpl(AccountService accountService, CategoryService categoryService,
            ItemService itemService, ResourceService resourceService,
            ResourceGroupService resourceGroupService, AppService appService,
            ThemeService themeService, PlaylistService playlistService,
            RoleService roleService, RoleMatrixService roleMatrixService) {
        this.accountService = accountService;
        this.categoryService = categoryService;
        this.itemService = itemService;
        this.resourceService = resourceService;
        this.resourceGroupService = resourceGroupService;
        this.appService = appService;
        this.themeService = themeService;
        this.playlistService = playlistService;
        this.roleService = roleService;
        this.roleMatrixService = roleMatrixService;
    }

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

    @Override
    public ResourceGroupService getResourceGroupService() {
        return resourceGroupService;
    }

    @Override
    public AppService getAppService() {
        return appService;
    }

    @Override
    public ThemeService getThemeService() {
        return themeService;
    }

    @Override
    public PlaylistService getPlaylistService() {
        return playlistService;
    }

    @Override
    public RoleService getRoleService() {
        return roleService;
    }

    @Override
    public RoleMatrixService getRoleMatrixService() {
        return roleMatrixService;
    }
}

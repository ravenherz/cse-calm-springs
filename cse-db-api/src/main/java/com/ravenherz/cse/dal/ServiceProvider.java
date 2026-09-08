package com.ravenherz.cse.dal;

import com.ravenherz.cse.dal.dao.AccountService;
import com.ravenherz.cse.dal.dao.AppService;
import com.ravenherz.cse.dal.dao.ThemeService;
import com.ravenherz.cse.dal.dao.CategoryService;
import com.ravenherz.cse.dal.dao.ItemService;
import com.ravenherz.cse.dal.dao.PlaylistService;
import com.ravenherz.cse.dal.dao.ResourceService;
import com.ravenherz.cse.dal.dao.ResourceGroupService;

public interface ServiceProvider {

    ItemService getItemService();

    CategoryService getCategoryService();

    AccountService getAccountService();

    ResourceService getResourceService();

    ResourceGroupService getResourceGroupService();

    AppService getAppService();

    ThemeService getThemeService();

    PlaylistService getPlaylistService();
}

package com.ravenherz.cse.transfer;

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
import com.ravenherz.cse.dal.dao.UrlTemplateService;

/**
 * Stores a site export reads. The WAR's {@code ServiceProvider} implements this.
 */
public interface SiteServices {

    ItemService getItemService();

    CategoryService getCategoryService();

    AccountService getAccountService();

    ResourceService getResourceService();

    ResourceGroupService getResourceGroupService();

    AppService getAppService();

    ThemeService getThemeService();

    PlaylistService getPlaylistService();

    UrlTemplateService getUrlTemplateService();

    RoleService getRoleService();

    RoleMatrixService getRoleMatrixService();
}

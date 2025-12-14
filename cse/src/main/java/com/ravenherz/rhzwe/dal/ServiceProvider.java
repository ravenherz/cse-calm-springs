package com.ravenherz.rhzwe.dal;

import com.ravenherz.rhzwe.dal.dao.AccountService;
import com.ravenherz.rhzwe.dal.dao.CategoryService;
import com.ravenherz.rhzwe.dal.dao.ItemService;
import com.ravenherz.rhzwe.dal.dao.ResourceService;

public interface ServiceProvider {

    ItemService getItemService();

    CategoryService getCategoryService();

    AccountService getAccountService();

    ResourceService getResourceService();
}

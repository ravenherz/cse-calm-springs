package com.ravenherz.cse.admin;

import com.ravenherz.cse.dal.dto.AccountEntity;
import org.springframework.ui.Model;

/**
 * Sidebar flags and theme ids for an editor page. The WAR implements this.
 */
public interface EditorChrome {

    void apply(Model model, AccountEntity accessor);
}

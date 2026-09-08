package com.ravenherz.cse.controller;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import com.ravenherz.cse.present.AccountDisplayDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/editor")
public class EditorAccountsController extends AbstractController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditorAccountsController.class);

    @GetMapping("/accounts")
    public String listAccounts(Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }

        List<AccountEntity> accounts = serviceProvider.getAccountService().getAllAccounts();
        List<AccountDisplayDTO> accountDTOs = new ArrayList<>();
        
        for (AccountEntity account : accounts) {
            AccountDisplayDTO dto = new AccountDisplayDTO();
            dto.setId(account.getId().toString());
            
            if (account.getAccountData() != null) {
                dto.setLogin(account.getAccountData().getLogin());
                dto.setEmailAddress(account.getAccountData().getEmailAddress());
                dto.setLevel(account.getAccountData().getLevel() != null ? account.getAccountData().getLevel().name() : null);
                dto.setLoginable(account.getAccountData().isLoginable());
                
                if (account.getAccountData().getSessions() != null) {
                    var sessions = account.getAccountData().getSessions();
                    long activeCount = sessions.stream().filter(s -> !Boolean.TRUE.equals(s.getFinished())).count();
                    dto.setSessionTotalCount(sessions.size());
                    dto.setSessionActiveCount((int) activeCount);
                    
                    sessions.stream()
                        .filter(s -> s.getStartedServerDateTime() != null)
                        .min(java.util.Comparator.comparing(s -> s.getStartedServerDateTime()))
                        .ifPresent(s -> {
                            dto.setSessionFirstDate(s.getStartedServerDateTime());
                            dto.setSessionFirstAddress(s.getRemoteAddress());
                            dto.setSessionFirstUserAgent(s.getUserAgent());
                        });
                    
                    sessions.stream()
                        .filter(s -> s.getStartedServerDateTime() != null)
                        .max(java.util.Comparator.comparing(s -> s.getStartedServerDateTime()))
                        .ifPresent(s -> {
                            dto.setSessionLastDate(s.getStartedServerDateTime());
                            dto.setSessionLastAddress(s.getRemoteAddress());
                            dto.setSessionLastUserAgent(s.getUserAgent());
                        });
                }
            }
            
            accountDTOs.add(dto);
        }
        
        model.addAttribute("accounts", accountDTOs);
        addEditorChrome(model, accessor);

        return "/admin/editor-accounts-list";
    }

    @PostMapping("/account/delete")
    public String deleteAccount(@RequestParam(value = "id", required = false) String id,
                                 Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }

        if (id == null || id.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/editor/accounts");
            return null;
        }

        AccountEntity account;
        try {
            org.bson.types.ObjectId objId = new org.bson.types.ObjectId(id.trim());
            account = (AccountEntity) serviceProvider.getAccountService().getById(AccountEntity.class, objId);
        } catch (Exception e) {
            error(404, request, response);
            return null;
        }

        if (account != null && account.getAccountData() != null
                && SecurityLevel.OWNER.equals(account.getAccountData().getLevel())) {
            response.sendRedirect(request.getContextPath() + "/editor/roles?error=sole-owner");
            return null;
        }

        try {
            serviceProvider.getAccountService().delete(account);
        } catch (Exception e) {
            LOGGER.error("Failed to delete account: " + e.getMessage(), e);
        }

        response.sendRedirect(request.getContextPath() + "/editor/accounts");
        return null;
    }
}

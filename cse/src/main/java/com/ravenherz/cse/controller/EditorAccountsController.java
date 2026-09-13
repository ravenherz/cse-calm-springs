package com.ravenherz.cse.controller;

import com.ravenherz.cse.dal.dao.RoleMatrixService;
import com.ravenherz.cse.dal.dao.RoleService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.RoleEntity;
import com.ravenherz.cse.dal.role.CapabilityIds;
import com.ravenherz.cse.present.AccountDirectory;
import com.ravenherz.cse.present.AccountDisplayDTO;
import com.ravenherz.cse.present.RoleOptionDTO;
import com.ravenherz.cse.security.AccountRolePolicy;
import com.ravenherz.cse.security.AccountRoles;
import com.ravenherz.cse.security.CapabilityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/editor")
public class EditorAccountsController extends AbstractController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditorAccountsController.class);

    @Autowired
    private CapabilityService capabilityService;

    private RoleService roles() {
        return serviceProvider.getRoleService();
    }

    private RoleMatrixService matrix() {
        return serviceProvider.getRoleMatrixService();
    }

    @GetMapping("/accounts")
    public String listAccounts(@RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "saved", required = false) String saved,
            @RequestParam(value = "error", required = false) String error,
            Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }

        roles().ensureSeeded();
        matrix().ensureSeeded(roles());

        String query = AccountDirectory.normalizeQuery(q);
        List<AccountEntity> accounts = serviceProvider.getAccountService().getAllAccounts();
        List<RoleEntity> allRoles = roles().getAll();
        List<RoleEntity> customRoles = allRoles.stream()
                .filter(role -> role != null && !role.isSystem() && !role.isArchived())
                .toList();
        boolean actorHasEditor = capabilityService.canOpenEditor(accessor);
        boolean actorIsOwner = AccountRoles.isOwner(accessor, roles());

        List<AccountDisplayDTO> accountDTOs = new ArrayList<>();
        List<AccountDisplayDTO> transferTargets = new ArrayList<>();
        for (AccountEntity account : accounts) {
            AccountDisplayDTO dto = toDisplay(account, accessor, actorHasEditor, customRoles);
            if (!dto.isOwner() && dto.getId() != null && dto.getLogin() != null) {
                transferTargets.add(dto);
            }
            if (AccountDirectory.loginMatches(dto.getLogin(), query)) {
                accountDTOs.add(dto);
            }
        }
        accountDTOs.sort(Comparator.comparing(
                dto -> dto.getLogin() == null ? "" : dto.getLogin().toLowerCase(Locale.ROOT)));

        model.addAttribute("accounts", accountDTOs);
        model.addAttribute("q", query);
        model.addAttribute("canTransfer", AccountRolePolicy.canTransfer(actorIsOwner));
        model.addAttribute("transferTargets", transferTargets);
        model.addAttribute("saved", saved != null);
        model.addAttribute("transferred", "transfer".equals(saved));
        model.addAttribute("error", AccountRolePolicy.messageFor(error));
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

        String accountsUrl = request.getContextPath() + "/editor/accounts";
        if (id == null || id.trim().isEmpty()) {
            response.sendRedirect(accountsUrl);
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

        if (account != null && AccountRoles.isOwner(account, serviceProvider.getRoleService())) {
            response.sendRedirect(accountsUrl + "?error=sole-owner");
            return null;
        }

        try {
            serviceProvider.getAccountService().delete(account);
        } catch (Exception e) {
            LOGGER.error("Failed to delete account: " + e.getMessage(), e);
        }

        response.sendRedirect(accountsUrl);
        return null;
    }

    private AccountDisplayDTO toDisplay(AccountEntity account, AccountEntity accessor,
            boolean actorHasEditor, List<RoleEntity> customRoles) {
        AccountDisplayDTO dto = new AccountDisplayDTO();
        if (account.getId() != null) {
            dto.setId(account.getId().toString());
        }
        boolean self = accessor.getId() != null && accessor.getId().equals(account.getId());
        dto.setSelf(self);
        if (account.getAccountData() == null) {
            dto.setInitial(AccountDirectory.initial(null));
            dto.setAssignableRoles(List.of());
            return dto;
        }
        var data = account.getAccountData();
        dto.setLogin(data.getLogin());
        dto.setShownName(data.getShownName());
        dto.setAvatar(AccountDirectory.safeAvatar(data.getAvatar()));
        dto.setInitial(AccountDirectory.initial(data.getLogin()));
        dto.setEmailAddress(data.getEmailAddress());
        dto.setLevel(data.getLevel() != null ? data.getLevel().name() : null);
        dto.setLoginable(data.isLoginable());

        RoleEntity current = roleOf(account);
        dto.setRoleId(current == null ? data.getRoleId() : current.idHex());
        dto.setRoleName(current == null ? "—" : current.getName());
        dto.setOwner(AccountRoles.isOwner(account, roles()));
        boolean canEdit = AccountRolePolicy.canEdit(actorHasEditor, current);
        dto.setCanEdit(canEdit);
        if (!canEdit) {
            dto.setLockHint(dto.isOwner() ? "Owner" : "Locked");
        }
        List<RoleEntity> assignable = AccountRolePolicy.assignableRoles(customRoles, actorHasEditor, current);
        if (self && actorHasEditor) {
            assignable = assignable.stream()
                    .filter(role -> role.isOwner() || matrix().allows(role.idHex(), CapabilityIds.EDITOR_ACCESS))
                    .toList();
        }
        dto.setAssignableRoles(assignable.stream()
                .map(role -> new RoleOptionDTO(role.idHex(), role.getName(), role.isLoginable()))
                .toList());

        if (data.getSessions() != null) {
            var sessions = data.getSessions();
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
        return dto;
    }

    private RoleEntity roleOf(AccountEntity account) {
        String roleId = AccountRoles.roleId(account, roles());
        RoleEntity role = roles().getById(roleId);
        return role != null ? role : roles().getBySlug(roleId);
    }
}

package com.ravenherz.cse.controller;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import com.ravenherz.cse.present.RoleAccountDTO;
import com.ravenherz.cse.security.AccountRolePolicy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.types.ObjectId;
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
public class EditorRolesController extends AbstractController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditorRolesController.class);

    @GetMapping("/roles")
    public String listRoles(@RequestParam(value = "saved", required = false) String saved,
            @RequestParam(value = "error", required = false) String error,
            Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }

        List<AccountEntity> accounts = serviceProvider.getAccountService().getAllAccounts();
        long owners = AccountRolePolicy.ownerCount(accounts);
        SecurityLevel actorLevel = accessor.getAccountData() != null
                ? accessor.getAccountData().getLevel() : null;

        List<RoleAccountDTO> rows = new ArrayList<>();
        List<RoleAccountDTO> transferTargets = new ArrayList<>();
        for (AccountEntity account : accounts) {
            RoleAccountDTO row = toRow(account, accessor, actorLevel, owners);
            rows.add(row);
            if (!SecurityLevel.OWNER.equals(account.getAccountData() == null
                    ? null : account.getAccountData().getLevel())
                    && row.getId() != null && row.getLogin() != null) {
                transferTargets.add(row);
            }
        }

        model.addAttribute("accounts", rows);
        model.addAttribute("canTransfer", AccountRolePolicy.canTransfer(actorLevel));
        model.addAttribute("transferTargets", transferTargets);
        model.addAttribute("saved", saved != null);
        model.addAttribute("transferred", "transfer".equals(saved));
        model.addAttribute("error", AccountRolePolicy.messageFor(error));
        addEditorChrome(model, accessor);
        return "/admin/editor-roles";
    }

    @PostMapping("/roles/transfer")
    public String transferOwnership(@RequestParam(value = "id", required = false) String id,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        return saveRole(id, SecurityLevel.OWNER.name(), request, response);
    }

    @PostMapping("/roles/save")
    public String saveRole(@RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "level", required = false) String levelName,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }

        String rolesUrl = request.getContextPath() + "/editor/roles";
        if (id == null || id.isBlank() || levelName == null || levelName.isBlank()) {
            response.sendRedirect(rolesUrl + "?error=unknown-level");
            return null;
        }

        SecurityLevel requested;
        try {
            requested = SecurityLevel.valueOf(levelName.trim());
        } catch (IllegalArgumentException ex) {
            response.sendRedirect(rolesUrl + "?error=unknown-level");
            return null;
        }

        AccountEntity target;
        try {
            target = (AccountEntity) serviceProvider.getAccountService()
                    .getById(AccountEntity.class, new ObjectId(id.trim()));
        } catch (Exception ex) {
            error(404, request, response);
            return null;
        }
        if (target == null || target.getAccountData() == null) {
            error(404, request, response);
            return null;
        }

        List<AccountEntity> accounts = serviceProvider.getAccountService().getAllAccounts();
        long owners = AccountRolePolicy.ownerCount(accounts);
        SecurityLevel actorLevel = accessor.getAccountData().getLevel();
        SecurityLevel current = target.getAccountData().getLevel();
        boolean self = accessor.getId() != null && accessor.getId().equals(target.getId());

        AccountRolePolicy.Decision decision = AccountRolePolicy.evaluate(
                actorLevel, self, current, requested, owners);
        if (!decision.allowed()) {
            response.sendRedirect(rolesUrl + "?error=" + decision.error());
            return null;
        }

        target.getAccountData().setLevel(requested);
        target.getAccountData().setLoginable(decision.loginable());
        if (!serviceProvider.getAccountService().replace(target)) {
            LOGGER.warn("Failed to save role for account {}", id);
            response.sendRedirect(rolesUrl + "?error=save-failed");
            return null;
        }
        if (decision.transferOwnership()) {
            for (AccountEntity previous : AccountRolePolicy.ownersExcept(accounts, target.getId())) {
                if (previous.getAccountData() == null) {
                    continue;
                }
                previous.getAccountData().setLevel(SecurityLevel.ADMIN);
                previous.getAccountData().setLoginable(true);
                if (!serviceProvider.getAccountService().replace(previous)) {
                    LOGGER.warn("Failed to demote previous owner {}", previous.getId());
                    response.sendRedirect(rolesUrl + "?error=save-failed");
                    return null;
                }
            }
        }

        response.sendRedirect(rolesUrl + (decision.transferOwnership() ? "?saved=transfer" : "?saved=1"));
        return null;
    }

    private static RoleAccountDTO toRow(AccountEntity account, AccountEntity accessor,
            SecurityLevel actorLevel, long owners) {
        RoleAccountDTO dto = new RoleAccountDTO();
        if (account.getId() != null) {
            dto.setId(account.getId().toString());
        }
        boolean self = accessor.getId() != null && accessor.getId().equals(account.getId());
        dto.setSelf(self);
        if (account.getAccountData() == null) {
            dto.setAssignableLevels(List.of());
            return dto;
        }
        SecurityLevel current = account.getAccountData().getLevel();
        dto.setLogin(account.getAccountData().getLogin());
        dto.setEmailAddress(account.getAccountData().getEmailAddress());
        dto.setLevel(current != null ? current.name() : null);
        dto.setLoginable(account.getAccountData().isLoginable());
        boolean canEdit = AccountRolePolicy.canEdit(actorLevel, current);
        dto.setCanEdit(canEdit);
        if (!canEdit) {
            dto.setLockHint(SecurityLevel.OWNER.equals(current) ? "Owner" : "Locked");
        }
        dto.setAssignableLevels(AccountRolePolicy.assignableLevels(actorLevel, current, self, owners)
                .stream()
                .map(Enum::name)
                .toList());
        return dto;
    }
}

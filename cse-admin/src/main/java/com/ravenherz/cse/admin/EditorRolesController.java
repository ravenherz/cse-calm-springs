package com.ravenherz.cse.admin;

import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.dao.AccountService;
import com.ravenherz.cse.dal.dao.RoleMatrixService;
import com.ravenherz.cse.dal.dao.RoleService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.RoleEntity;
import com.ravenherz.cse.dal.dto.basic.RoleGrant;
import com.ravenherz.cse.dal.role.CapabilityIds;
import com.ravenherz.cse.dal.role.RoleSeeds;
import com.ravenherz.cse.security.AccountAccessor;
import com.ravenherz.cse.security.AccountRolePolicy;
import com.ravenherz.cse.security.AccountRoles;
import com.ravenherz.cse.security.CapabilityRecord;
import com.ravenherz.cse.security.CapabilityService;
import com.ravenherz.cse.security.RoleMatrixView;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/editor")
public class EditorRolesController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditorRolesController.class);
    private static final Pattern SLUG = Pattern.compile("^[a-z0-9-]{1,32}$");

    private final AccountAccessor authSupport;
    private final EditorChrome editorChrome;
    private final RoleService roles;
    private final RoleMatrixService matrix;
    private final AccountService accountService;
    private final CapabilityService capabilityService;

    public EditorRolesController(AccountAccessor authSupport, EditorChrome editorChrome, RoleService roles,
            RoleMatrixService matrix,             AccountService accountService, CapabilityService capabilityService) {
        this.authSupport = authSupport;
        this.editorChrome = editorChrome;
        this.roles = roles;
        this.matrix = matrix;
        this.accountService = accountService;
        this.capabilityService = capabilityService;
    }

    @GetMapping("/roles")
    public String listRoles(@RequestParam(value = "saved", required = false) String saved,
            @RequestParam(value = "error", required = false) String error,
            Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
            return null;
        }

        roles.ensureSeeded();
        matrix.ensureSeeded(roles);
        capabilityService.catalog().refresh();

        boolean actorIsOwner = AccountRoles.isOwner(accessor, roles);

        List<RoleEntity> allRoles = roles.getAll();
        List<RoleEntity> customRoles = allRoles.stream()
                .filter(role -> role != null && !role.isSystem() && !role.isArchived())
                .toList();

        model.addAttribute("canEditMatrix", actorIsOwner);
        model.addAttribute("canCrudRoles", actorIsOwner);
        model.addAttribute("saved", saved != null);
        model.addAttribute("matrixSaved", "matrix".equals(saved));
        model.addAttribute("error", AccountRolePolicy.messageFor(error));
        model.addAttribute("matrixRoles", allRoles.stream().filter(role -> !role.isArchived()).toList());
        model.addAttribute("matrixGroups", matrixGroups(allRoles));
        model.addAttribute("customRoles", customRoles);
        editorChrome.apply(model, accessor);
        return "/admin/editor-roles";
    }

    @PostMapping("/roles/matrix")
    public String saveMatrix(@RequestParam(value = "grant", required = false) List<String> grants,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        String rolesUrl = request.getContextPath() + "/editor/roles";
        if (!AccountRoles.isOwner(accessor, roles)) {
            response.sendRedirect(rolesUrl + "?error=forbidden");
            return null;
        }
        capabilityService.catalog().refresh();
        Set<String> knownCaps = new HashSet<>();
        for (CapabilityRecord record : capabilityService.catalog().all()) {
            knownCaps.add(record.id());
        }
        RoleEntity guest = roles.guest();
        RoleEntity owner = roles.owner();
        String guestId = guest == null ? null : guest.idHex();
        String ownerId = owner == null ? null : owner.idHex();
        List<RoleGrant> next = new ArrayList<>();
        if (grants != null) {
            for (String raw : grants) {
                if (raw == null || !raw.contains("|")) {
                    continue;
                }
                int split = raw.lastIndexOf('|');
                String capabilityId = raw.substring(0, split);
                String roleId = raw.substring(split + 1);
                if (!knownCaps.contains(capabilityId) || roleId.isBlank()) {
                    continue;
                }
                if (roleId.equals(ownerId)) {
                    continue;
                }
                CapabilityRecord record = capabilityService.catalog().find(capabilityId);
                if (roleId.equals(guestId) && (record == null || !record.guestSafe())) {
                    continue;
                }
                next.add(new RoleGrant(capabilityId, roleId));
            }
        }
        matrix.replaceGrants(next);
        response.sendRedirect(rolesUrl + "?saved=matrix");
        return null;
    }

    @PostMapping("/roles/create")
    public String createRole(@RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "slug", required = false) String slug,
            @RequestParam(value = "loginable", required = false) String loginable,
            @RequestParam(value = "copyFrom", required = false) String copyFrom,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        String rolesUrl = request.getContextPath() + "/editor/roles";
        if (!AccountRoles.isOwner(accessor, roles)) {
            response.sendRedirect(rolesUrl + "?error=forbidden");
            return null;
        }
        String normalizedName = name == null ? "" : name.trim();
        String normalizedSlug = slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT);
        if (normalizedName.isEmpty() || normalizedName.length() > 40 || !SLUG.matcher(normalizedSlug).matches()
                || RoleSeeds.SYSTEM_SLUGS.contains(normalizedSlug) || RoleSeeds.isRetiredSlug(normalizedSlug)
                || roles.getBySlug(normalizedSlug) != null) {
            response.sendRedirect(rolesUrl + "?error=unknown-level");
            return null;
        }
        RoleEntity role = new RoleEntity();
        role.setName(normalizedName);
        role.setSlug(normalizedSlug);
        role.setLoginable("on".equals(loginable) || "true".equals(loginable));
        role.setSortOrder(nextSortOrder());
        roles.insert(role);
        if (copyFrom != null && !copyFrom.isBlank() && role.getId() != null) {
            List<RoleGrant> grants = new ArrayList<>(matrix.get().getGrants());
            for (RoleGrant grant : List.copyOf(grants)) {
                if (grant != null && copyFrom.equals(grant.getRoleId())) {
                    grants.add(new RoleGrant(grant.getCapabilityId(), role.idHex()));
                }
            }
            matrix.replaceGrants(grants);
        }
        response.sendRedirect(rolesUrl + "?saved=1");
        return null;
    }

    @PostMapping("/roles/archive")
    public String archiveRole(@RequestParam("id") String id,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        return mutateCustomRole(id, true, false, request, response);
    }

    @PostMapping("/roles/delete")
    public String deleteRole(@RequestParam("id") String id,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        return mutateCustomRole(id, false, true, request, response);
    }

    @PostMapping("/roles/transfer")
    public String transferOwnership(@RequestParam(value = "id", required = false) String id,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        RoleEntity owner = roles.owner();
        return saveRole(id, owner == null ? null : owner.idHex(), request, response);
    }

    @PostMapping("/roles/save")
    public String saveRole(@RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "roleId", required = false) String roleId,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
            return null;
        }

        String accountsUrl = request.getContextPath() + "/editor/accounts";
        if (id == null || id.isBlank() || roleId == null || roleId.isBlank()) {
            response.sendRedirect(accountsUrl + "?error=unknown-level");
            return null;
        }

        RoleEntity requested = roles.getById(roleId.trim());
        if (requested == null) {
            requested = roles.getBySlug(roleId.trim());
        }
        if (requested == null) {
            response.sendRedirect(accountsUrl + "?error=unknown-level");
            return null;
        }

        AccountEntity target;
        try {
            target = (AccountEntity) accountService
                    .getById(AccountEntity.class, EntityId.of(id.trim()));
        } catch (Exception ex) {
            response.sendRedirect(request.getContextPath() + "/?error=404");
            return null;
        }
        if (target == null || target.getAccountData() == null) {
            response.sendRedirect(request.getContextPath() + "/?error=404");
            return null;
        }

        List<AccountEntity> accounts = accountService.getAllAccounts();
        long owners = AccountRolePolicy.ownerCount(accounts, roles);
        boolean actorIsOwner = AccountRoles.isOwner(accessor, roles);
        boolean actorHasEditor = capabilityService.canOpenEditor(accessor);
        RoleEntity current = roleOf(target);
        boolean self = accessor.getId() != null && accessor.getId().equals(target.getId());
        boolean requestedHasEditor = requested.isOwner()
                || (requested.getId() != null && matrix.allows(requested.idHex(), CapabilityIds.EDITOR_ACCESS));

        AccountRolePolicy.Decision decision = AccountRolePolicy.evaluate(
                actorIsOwner, actorHasEditor, self, current, requested, requestedHasEditor, owners);
        if (!decision.allowed()) {
            response.sendRedirect(accountsUrl + "?error=" + decision.error());
            return null;
        }

        AccountRoles.assign(target.getAccountData(), requested, requestedHasEditor);
        if (!accountService.replace(target)) {
            LOGGER.warn("Failed to save role for account {}", id);
            response.sendRedirect(accountsUrl + "?error=save-failed");
            return null;
        }
        if (decision.transferOwnership()) {
            RoleEntity fallback = ensureAdminFallback();
            for (AccountEntity previous : AccountRolePolicy.ownersExcept(accounts, target.getId(), roles)) {
                if (previous.getAccountData() == null) {
                    continue;
                }
                AccountRoles.assign(previous.getAccountData(), fallback, true);
                if (!accountService.replace(previous)) {
                    LOGGER.warn("Failed to demote previous owner {}", previous.getId());
                    response.sendRedirect(accountsUrl + "?error=save-failed");
                    return null;
                }
            }
        }

        response.sendRedirect(accountsUrl + (decision.transferOwnership() ? "?saved=transfer" : "?saved=1"));
        return null;
    }

    private String mutateCustomRole(String id, boolean archive, boolean delete,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        String rolesUrl = request.getContextPath() + "/editor/roles";
        if (!AccountRoles.isOwner(accessor, roles)) {
            response.sendRedirect(rolesUrl + "?error=forbidden");
            return null;
        }
        RoleEntity role = roles.getById(id);
        if (role == null || role.isSystem()) {
            response.sendRedirect(rolesUrl + "?error=system-role");
            return null;
        }
        if (delete) {
            for (AccountEntity account : accountService.getAllAccounts()) {
                if (account.getAccountData() != null && id.equals(account.getAccountData().getRoleId())) {
                    response.sendRedirect(rolesUrl + "?error=role-in-use");
                    return null;
                }
            }
        }
        if (wouldRemoveLastEditor(role)) {
            response.sendRedirect(rolesUrl + "?error=last-editor");
            return null;
        }
        if (delete) {
            roles.delete(role);
        } else if (archive) {
            role.setArchived(true);
            roles.replace(role);
        }
        response.sendRedirect(rolesUrl + "?saved=1");
        return null;
    }

    private boolean wouldRemoveLastEditor(RoleEntity removing) {
        if (removing.getId() == null || !matrix.allows(removing.idHex(), CapabilityIds.EDITOR_ACCESS)) {
            return false;
        }
        boolean otherEditorRoleHeld = false;
        for (RoleEntity role : roles.getAll()) {
            if (role == null || role.isOwner() || role.getId() == null
                    || role.getId().equals(removing.getId())) {
                continue;
            }
            if (!matrix.allows(role.idHex(), CapabilityIds.EDITOR_ACCESS)) {
                continue;
            }
            for (AccountEntity account : accountService.getAllAccounts()) {
                if (account.getAccountData() != null
                        && role.idHex().equals(account.getAccountData().getRoleId())) {
                    otherEditorRoleHeld = true;
                    break;
                }
            }
        }
        return !otherEditorRoleHeld;
    }

    private RoleEntity ensureAdminFallback() {
        RoleEntity admin = roles.getBySlug(RoleSeeds.ADMIN);
        if (admin != null) {
            if (!matrix.allows(admin.idHex(), CapabilityIds.EDITOR_ACCESS)) {
                List<RoleGrant> grants = new ArrayList<>(matrix.get().getGrants());
                grants.add(new RoleGrant(CapabilityIds.EDITOR_ACCESS, admin.idHex()));
                matrix.replaceGrants(grants);
            }
            return admin;
        }
        admin = new RoleEntity();
        admin.setSlug(RoleSeeds.ADMIN);
        admin.setName("Admin");
        admin.setLoginable(true);
        admin.setSortOrder(70);
        roles.insert(admin);
        List<RoleGrant> grants = new ArrayList<>(matrix.get().getGrants());
        for (String cap : CapabilityIds.EDITOR_ALL) {
            grants.add(new RoleGrant(cap, admin.idHex()));
        }
        matrix.replaceGrants(grants);
        return admin;
    }

    private int nextSortOrder() {
        int max = 70;
        for (RoleEntity role : roles.getAll()) {
            if (role != null && !role.isOwner() && role.getSortOrder() > max && role.getSortOrder() < 1000) {
                max = role.getSortOrder();
            }
        }
        return max + 10;
    }

    private RoleEntity roleOf(AccountEntity account) {
        String roleId = AccountRoles.roleId(account, roles);
        RoleEntity role = roles.getById(roleId);
        return role != null ? role : roles.getBySlug(roleId);
    }

    private List<Map<String, Object>> matrixGroups(List<RoleEntity> allRoles) {
        return RoleMatrixView.groups(
                capabilityService.catalog().all(),
                allRoles,
                (roleId, capId) -> matrix.allows(roleId, capId));
    }
}

package com.ravenherz.cse.security;

import com.ravenherz.cse.dal.dao.RoleService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.RoleEntity;
import com.ravenherz.cse.dal.EntityId;

import java.util.ArrayList;
import java.util.List;

/**
 * Rules for changing an account’s role from the Accounts directory.
 */
public final class AccountRolePolicy {

    private AccountRolePolicy() {
    }

    public record Decision(boolean allowed, String error, Boolean loginable, boolean transferOwnership) {
        static Decision deny(String error) {
            return new Decision(false, error, null, false);
        }

        static Decision allow(boolean loginable) {
            return new Decision(true, null, loginable, false);
        }

        static Decision forTransfer() {
            return new Decision(true, null, true, true);
        }
    }

    public static boolean isOwnerRole(RoleEntity role) {
        return role != null && role.isOwner();
    }

    public static boolean isGuestRole(RoleEntity role) {
        return role != null && role.isGuest();
    }

    public static boolean canTransfer(boolean actorIsOwner) {
        return actorIsOwner;
    }

    public static boolean canEdit(boolean actorHasEditor, RoleEntity current) {
        if (isOwnerRole(current)) {
            return false;
        }
        return actorHasEditor;
    }

    public static long ownerCount(List<AccountEntity> accounts, RoleService roles) {
        return ownersExcept(accounts, null, roles).size();
    }

    public static List<AccountEntity> ownersExcept(List<AccountEntity> accounts, EntityId keepId,
            RoleService roles) {
        List<AccountEntity> owners = new ArrayList<>();
        if (accounts == null) {
            return owners;
        }
        for (AccountEntity account : accounts) {
            if (!AccountRoles.isOwner(account, roles)) {
                continue;
            }
            if (keepId != null && keepId.equals(account.getId())) {
                continue;
            }
            owners.add(account);
        }
        return owners;
    }

    public static List<RoleEntity> assignableRoles(List<RoleEntity> customRoles, boolean actorHasEditor,
            RoleEntity current) {
        List<RoleEntity> out = new ArrayList<>();
        if (!canEdit(actorHasEditor, current) || customRoles == null) {
            return out;
        }
        for (RoleEntity role : customRoles) {
            if (role == null || role.isSystem() || role.isArchived()) {
                continue;
            }
            out.add(role);
        }
        return out;
    }

    public static Decision evaluate(boolean actorIsOwner, boolean actorHasEditor, boolean actorIsTarget,
            RoleEntity current, RoleEntity requested, boolean requestedHasEditor, long owners) {
        if (requested == null) {
            return Decision.deny("unknown-level");
        }
        if (isGuestRole(requested)) {
            return Decision.deny("guest-not-assignable");
        }
        if (!actorHasEditor) {
            return Decision.deny("forbidden");
        }
        if (isOwnerRole(current) && !isOwnerRole(requested)) {
            return Decision.deny("sole-owner");
        }
        if (isOwnerRole(requested)) {
            if (isOwnerRole(current)) {
                return Decision.allow(true);
            }
            if (owners == 0) {
                return Decision.allow(true);
            }
            if (actorIsOwner && !actorIsTarget) {
                return Decision.forTransfer();
            }
            return Decision.deny("owner-required");
        }
        if (actorIsTarget && actorHasEditor && !requestedHasEditor) {
            return Decision.deny("self-lockout");
        }
        return Decision.allow(requested.isLoginable());
    }

    public static String messageFor(String error) {
        if (error == null || error.isBlank()) {
            return null;
        }
        return switch (error) {
            case "unknown-level" -> "Unknown role.";
            case "guest-not-assignable" -> "Guest is not an account role.";
            case "owner-required" -> "Only the owner can transfer ownership.";
            case "sole-owner", "last-owner" ->
                    "There must be exactly one owner. Transfer ownership from another account.";
            case "self-lockout" -> "You cannot remove your own editor access.";
            case "forbidden" -> "Not allowed.";
            case "save-failed" -> "Could not save the role.";
            case "role-in-use" -> "Reassign accounts before deleting this role.";
            case "system-role" -> "System roles cannot be changed.";
            case "last-editor" -> "Keep at least one non-owner role that can open Catalog.";
            default -> "Could not save the role.";
        };
    }

    public static boolean sameId(RoleEntity role, String idHex) {
        if (role == null || role.getId() == null || idHex == null) {
            return false;
        }
        return role.getId().toHexString().equals(idHex);
    }
}

package com.ravenherz.cse.security;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

/**
 * Rules for changing {@link SecurityLevel} in the editor Roles tab.
 * HTTP already requires ADMIN or OWNER to open {@code /editor/**}.
 * The site always has exactly one OWNER: granting OWNER is a transfer.
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

    public static long ownerCount(List<AccountEntity> accounts) {
        return ownersExcept(accounts, null).size();
    }

    public static List<AccountEntity> ownersExcept(List<AccountEntity> accounts, ObjectId keepId) {
        List<AccountEntity> owners = new ArrayList<>();
        if (accounts == null) {
            return owners;
        }
        for (AccountEntity account : accounts) {
            if (account == null || account.getAccountData() == null
                    || !SecurityLevel.OWNER.equals(account.getAccountData().getLevel())) {
                continue;
            }
            if (keepId != null && keepId.equals(account.getId())) {
                continue;
            }
            owners.add(account);
        }
        return owners;
    }

    public static boolean actorIsOwner(SecurityLevel actorLevel) {
        return SecurityLevel.OWNER.equals(actorLevel);
    }

    public static boolean canTransfer(SecurityLevel actorLevel) {
        return actorIsOwner(actorLevel);
    }

    public static boolean canEdit(SecurityLevel actorLevel, SecurityLevel current) {
        if (current == SecurityLevel.OWNER) {
            return false;
        }
        if (actorLevel == null) {
            return false;
        }
        return actorLevel.getIntLevel() >= SecurityLevel.ADMIN.getIntLevel();
    }

    public static List<SecurityLevel> assignableLevels(SecurityLevel actorLevel,
            SecurityLevel current, boolean actorIsTarget, long owners) {
        List<SecurityLevel> levels = new ArrayList<>();
        for (SecurityLevel level : SecurityLevel.values()) {
            if (level == SecurityLevel.GUEST) {
                continue;
            }
            if (!canOffer(actorLevel, current, level, actorIsTarget, owners)) {
                continue;
            }
            levels.add(level);
        }
        return levels;
    }

    public static Decision evaluate(SecurityLevel actorLevel, boolean actorIsTarget,
            SecurityLevel current, SecurityLevel requested, long owners) {
        if (requested == null) {
            return Decision.deny("unknown-level");
        }
        if (requested == SecurityLevel.GUEST) {
            return Decision.deny("guest-not-assignable");
        }
        if (actorLevel == null
                || actorLevel.getIntLevel() < SecurityLevel.ADMIN.getIntLevel()) {
            return Decision.deny("forbidden");
        }
        if (current == SecurityLevel.OWNER && requested != SecurityLevel.OWNER) {
            return Decision.deny("sole-owner");
        }
        if (requested == SecurityLevel.OWNER) {
            if (current == SecurityLevel.OWNER) {
                return Decision.allow(true);
            }
            if (owners == 0) {
                return Decision.allow(true);
            }
            if (actorIsOwner(actorLevel) && !actorIsTarget) {
                return Decision.forTransfer();
            }
            return Decision.deny("owner-required");
        }
        if (actorIsTarget && requested.getIntLevel() < SecurityLevel.ADMIN.getIntLevel()) {
            return Decision.deny("self-lockout");
        }
        return Decision.allow(loginableFor(requested));
    }

    public static boolean loginableFor(SecurityLevel level) {
        return level != null
                && level != SecurityLevel.GUEST
                && level != SecurityLevel.INACTIVE_USER
                && level != SecurityLevel.GUIDE;
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
            default -> "Could not save the role.";
        };
    }

    private static boolean canOffer(SecurityLevel actorLevel, SecurityLevel current,
            SecurityLevel candidate, boolean actorIsTarget, long owners) {
        if (current == SecurityLevel.OWNER) {
            return false;
        }
        if (candidate == SecurityLevel.OWNER) {
            return false;
        }
        if (actorIsTarget && candidate.getIntLevel() < SecurityLevel.ADMIN.getIntLevel()) {
            return false;
        }
        return true;
    }
}

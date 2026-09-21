package com.ravenherz.cse.engine.util;

import com.ravenherz.cse.util.AccountAvatars;
import com.ravenherz.cse.util.PasswordHashes;

import com.ravenherz.cse.dal.dao.AccountService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import com.ravenherz.cse.security.AccessRuntime;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public view and self-serve edits for {@link AccountData}. Does not expose hash, sessions,
 * or activation tokens.
 */
public final class AccountProfile {

    public static final int SHOWN_NAME_MAX = 25;
    public static final int BIO_MAX = 2000;
    public static final int EMAIL_MAX = 200;

    private AccountProfile() {
    }

    public static Map<String, Object> view(AccountData data) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (data == null) {
            return out;
        }
        out.put("login", nullToEmpty(data.getLogin()));
        out.put("emailAddress", nullToEmpty(data.getEmailAddress()));
        out.put("shownName", nullToEmpty(data.getShownName()));
        out.put("bio", nullToEmpty(data.getBio()));
        out.put("avatar", data.getAvatar());
        out.put("level", data.getLevel() == null ? null : data.getLevel().name());
        out.put("roleId", data.getRoleId());
        out.put("admin", isAdmin(data, null));
        return out;
    }

    public static String apply(AccountData data, Map<String, String> json, PasswordHashes hashes,
            AccountService accounts) {
        if (data == null) {
            return "Sign in required";
        }
        String email = trim(json.get("ACCOUNT_EMAIL"));
        if (email.isEmpty()) {
            return "Email is required";
        }
        if (email.length() > EMAIL_MAX || email.indexOf('@') < 1 || email.indexOf('.') < 0) {
            return "Email is not valid";
        }
        String shownName = trim(json.get("ACCOUNT_SHOWN_NAME"));
        if (shownName.length() > SHOWN_NAME_MAX) {
            return "Shown name is too long";
        }
        String bio = trim(json.get("ACCOUNT_BIO"));
        if (bio.length() > BIO_MAX) {
            return "Bio is too long";
        }
        if (!email.equalsIgnoreCase(nullToEmpty(data.getEmailAddress())) && accounts != null) {
            AccountEntity other = accounts.getByEmail(email);
            if (other != null && other.getAccountData() != null
                    && !nullToEmpty(data.getLogin()).equals(other.getAccountData().getLogin())) {
                return "Email is already taken";
            }
        }
        String passwordError = applyPassword(data, json, hashes);
        if (passwordError != null) {
            return passwordError;
        }
        if (json != null && json.containsKey("ACCOUNT_AVATAR")) {
            String avatar = json.get("ACCOUNT_AVATAR");
            if (avatar == null || avatar.isBlank()) {
                data.setAvatar(null);
            } else {
                try {
                    data.setAvatar(AccountAvatars.store(avatar));
                } catch (IOException ex) {
                    return ex.getMessage() == null ? "Could not read that image" : ex.getMessage();
                }
            }
        }
        data.setEmailAddress(email);
        data.setShownName(shownName.isEmpty() ? null : shownName);
        data.setBio(bio.isEmpty() ? null : bio);
        return null;
    }

    private static String applyPassword(AccountData data, Map<String, String> json,
            PasswordHashes hashes) {
        if (json == null) {
            return null;
        }
        String password = nullToEmpty(json.get("ACCOUNT_PASSWORD"));
        String retype = nullToEmpty(json.get("ACCOUNT_PASSWORD_RETYPE"));
        if (password.isEmpty() && retype.isEmpty()) {
            return null;
        }
        if (!password.equals(retype)) {
            return "Passwords don't match";
        }
        if (hashes != null && hashes.isTooLong(password)) {
            return "Password is too long";
        }
        String current = nullToEmpty(json.get("ACCOUNT_PASSWORD_CURRENT"));
        if (current.isEmpty()) {
            return "Current password is required";
        }
        if (hashes == null || !hashes.verify(current, data.getHash())) {
            return "Current password is wrong";
        }
        data.setHash(hashes.hash(password));
        return null;
    }

    static boolean isAdmin(AccountData data) {
        return isAdmin(data, null);
    }

    static boolean isAdmin(AccountData data, AccountEntity account) {
        if (account != null) {
            return AccessRuntime.editorAccess(account);
        }
        if (data == null || data.getLevel() == null) {
            return false;
        }
        return data.getLevel().getIntLevel() >= SecurityLevel.ADMIN.getIntLevel();
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
